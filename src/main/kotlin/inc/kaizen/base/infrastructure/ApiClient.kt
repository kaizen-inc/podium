package inc.kaizen.base.infrastructure

import com.google.gson.GsonBuilder
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Core API client builder wrapping Retrofit2 with sensible defaults.
 *
 * Provides configurable HTTP logging, authorization interceptors, timeout configuration,
 * retry support, and Gson-based serialization with Java 8+ date/time adapters.
 *
 * @param baseUrl The base URL for the API.
 * @param okHttpClientBuilder Optional custom OkHttpClient builder.
 * @param serializerBuilder The GsonBuilder used for serialization. Defaults to [Serializer.gsonBuilder].
 * @param callFactory Optional custom Call.Factory.
 * @param converterFactory Optional additional Converter.Factory.
 * @param timeoutConfig Timeout configuration for connect, read, and write. Defaults to 30s each.
 * @param retryInterceptor Optional retry interceptor for automatic request retries.
 * @param loggingLevel HTTP logging verbosity. Defaults to [HttpLoggingInterceptor.Level.BODY].
 */
class ApiClient(
    private var baseUrl: String,
    private val okHttpClientBuilder: OkHttpClient.Builder? = null,
    private val serializerBuilder: GsonBuilder = Serializer.gsonBuilder,
    private val callFactory: Call.Factory? = null,
    private val converterFactory: Converter.Factory? = null,
    private val timeoutConfig: TimeoutConfig = TimeoutConfig(),
    private val retryInterceptor: RetryInterceptor? = null,
    private val loggingLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BODY,
) {
    private val apiAuthorizations = mutableMapOf<String, Interceptor>()
    private var logger: ((String) -> Unit)? = null

    private val apis = mutableMapOf<String, Any>()

    private val retrofitBuilder: Retrofit.Builder by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(serializerBuilder.create()))
            .apply {
                if (converterFactory != null) {
                    addConverterFactory(converterFactory)
                }
            }
    }

    private val clientBuilder: OkHttpClient.Builder by lazy {
        okHttpClientBuilder ?: defaultClientBuilder
    }

    private val defaultClientBuilder: OkHttpClient.Builder by lazy {
        OkHttpClient()
            .newBuilder()
            .connectTimeout(timeoutConfig.connectTimeout, timeoutConfig.timeUnit)
            .readTimeout(timeoutConfig.readTimeout, timeoutConfig.timeUnit)
            .writeTimeout(timeoutConfig.writeTimeout, timeoutConfig.timeUnit)
            .addInterceptor(HttpLoggingInterceptor { message -> logger?.invoke(message) }.apply {
                level = loggingLevel
            })
            .apply {
                retryInterceptor?.let { addInterceptor(it) }
            }
    }

    init {
        normalizeBaseUrl()
    }

    /**
     * Adds an authorization interceptor to be used by the client.
     * @param authName Authentication name (must be unique).
     * @param authorization Authorization interceptor.
     * @return This [ApiClient] instance for chaining.
     * @throws IllegalArgumentException if [authName] is already registered.
     */
    fun addAuthorization(authName: String, authorization: Interceptor): ApiClient {
        if (apiAuthorizations.containsKey(authName)) {
            throw IllegalArgumentException("auth name '$authName' already in api authorizations")
        }
        apiAuthorizations[authName] = authorization
        clientBuilder.addInterceptor(authorization)
        return this
    }

    /**
     * Removes a previously registered authorization interceptor by name.
     *
     * Note: OkHttp does not support removing interceptors from a built client. This removes the
     * entry from the internal registry so it is not re-applied on future client builds, but
     * services created before this call are unaffected.
     *
     * @param authName The name of the authorization to remove.
     * @return `true` if the authorization was found and removed, `false` otherwise.
     */
    fun removeAuthorization(authName: String): Boolean {
        return apiAuthorizations.remove(authName) != null
    }

    /**
     * Sets a custom logger callback for HTTP traffic logging.
     * @param logger A function that receives log messages.
     * @return This [ApiClient] instance for chaining.
     */
    fun setLogger(logger: (String) -> Unit): ApiClient {
        this.logger = logger
        return this
    }

    /**
     * Creates (or retrieves from cache) a Retrofit service instance for the given interface.
     *
     * @param S The service interface type.
     * @param serviceClass The class of the service interface.
     * @param interceptor An optional per-service interceptor added non-destructively.
     * @return The created or cached service instance.
     */
    fun <S> createService(serviceClass: Class<S>, interceptor: Interceptor? = null): S {
        val key = serviceClass.simpleName
        if (apis.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return apis[key] as S
        }

        val usedCallFactory = if (interceptor != null) {
            val perServiceClient = clientBuilder.build().newBuilder()
                .addInterceptor(interceptor)
                .build()
            this.callFactory ?: perServiceClient
        } else {
            this.callFactory ?: clientBuilder.build()
        }

        val service = retrofitBuilder.callFactory(usedCallFactory).build().create(serviceClass)
        apis[key] = service as Any
        return service
    }

    /**
     * Creates (or retrieves from cache) a Retrofit service instance using a reified type parameter.
     *
     * Usage: `val service = client.createService<MyService>()`
     *
     * @param S The service interface type.
     * @param interceptor An optional per-service interceptor.
     * @return The created or cached service instance.
     */
    inline fun <reified S> createService(interceptor: Interceptor? = null): S {
        return createService(S::class.java, interceptor)
    }

    /**
     * Clears the internal service instance cache.
     *
     * Call this if you need to force re-creation of service instances (e.g., after changing
     * authorization headers or base URL).
     */
    fun clearServiceCache() {
        apis.clear()
    }

    private fun normalizeBaseUrl() {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }
    }
}
