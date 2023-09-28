package inc.kaizen.base.infrastructure

import com.google.gson.GsonBuilder
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type

class ApiClient(
    private var baseUrl: String,
    private val okHttpClientBuilder: OkHttpClient.Builder? = null,
    private val serializerBuilder: GsonBuilder = Serializer.gsonBuilder,
    private val callFactory: Call.Factory? = null,
    private val converterFactory: Converter.Factory? = null,
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
                addConverterFactory(object : Converter.Factory() {
                    override fun responseBodyConverter(
                        type: Type,
                        annotations: Array<out Annotation>,
                        retrofit: Retrofit
                    ): Converter<ResponseBody, *>? {
                        return super.responseBodyConverter(type, annotations, retrofit)
                    }
                })
            }
    }

    private val clientBuilder: OkHttpClient.Builder by lazy {
        okHttpClientBuilder ?: defaultClientBuilder
    }

    private val defaultClientBuilder: OkHttpClient.Builder by lazy {
        OkHttpClient()
            .newBuilder()
            .addInterceptor(HttpLoggingInterceptor { message -> logger?.invoke(message) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
    }

    init {
        normalizeBaseUrl()
    }

    /**
     * Adds an authorization to be used by the client
     * @param authName Authentication name
     * @param authorization Authorization interceptor
     * @return ApiClient
     */
    fun addAuthorization(authName: String, authorization: Interceptor): ApiClient {
        if (apiAuthorizations.containsKey(authName)) {
            throw RuntimeException("auth name $authName already in api authorizations")
        }
        apiAuthorizations[authName] = authorization
        clientBuilder.addInterceptor(authorization)
        return this
    }

    fun setLogger(logger: (String) -> Unit): ApiClient {
        this.logger = logger
        return this
    }

    fun <S> createService(serviceClass: Class<S>, interceptor: Interceptor? = null): S {
        return if (apis.containsKey(serviceClass.simpleName)) {
            apis[serviceClass.simpleName] as S
        } else {
            interceptor?.let {
                clientBuilder.interceptors().clear()
                clientBuilder.addInterceptor(interceptor)
            }
            val usedCallFactory = this.callFactory ?: clientBuilder.build()
             retrofitBuilder.callFactory(usedCallFactory).build().create(serviceClass)
        }
    }

    private fun normalizeBaseUrl() {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }
    }

    private inline fun <T, reified U> Iterable<T>.runOnFirst(callback: U.() -> Unit) {
        for (element in this) {
            if (element is U) {
                callback.invoke(element)
                break
            }
        }
    }
}
