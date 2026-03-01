package inc.kaizen.infra.podium

import retrofit2.HttpException
import retrofit2.Response

/**
 * A sealed class representing the result of an API call.
 *
 * @param T The type of the successful response body.
 */
sealed class ApiResult<out T> {

    /**
     * Represents a successful API response.
     *
     * @property data The deserialized response body.
     * @property code The HTTP status code.
     */
    data class Success<T>(val data: T, val code: Int) : ApiResult<T>()

    /**
     * Represents an API error response (HTTP 4xx/5xx).
     *
     * @property code The HTTP status code.
     * @property message The error message.
     * @property errorBody The raw error body string, if available.
     */
    data class Error(val code: Int, val message: String?, val errorBody: String? = null) : ApiResult<Nothing>()

    /**
     * Represents a network or unexpected exception.
     *
     * @property exception The thrown exception.
     */
    data class Exception(val exception: Throwable) : ApiResult<Nothing>()

    /**
     * Returns the data if this is a [Success], or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the data if this is a [Success], or the result of [defaultValue] otherwise.
     */
    fun getOrElse(defaultValue: () -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue()
    }

    /**
     * Transforms the [Success] data using [transform], leaving [Error] and [Exception] unchanged.
     */
    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data), code)
        is Error -> this
        is Exception -> this
    }

    /**
     * Chains another [ApiResult]-producing operation on [Success] data.
     */
    fun <R> flatMap(transform: (T) -> ApiResult<R>): ApiResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Exception -> this
    }

    /**
     * Executes [action] if this is a [Success], then returns this result unchanged.
     */
    fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes [action] if this is an [Error], then returns this result unchanged.
     */
    fun onError(action: (Error) -> Unit): ApiResult<T> {
        if (this is Error) action(this)
        return this
    }

    /**
     * Executes [action] if this is an [Exception], then returns this result unchanged.
     */
    fun onException(action: (Throwable) -> Unit): ApiResult<T> {
        if (this is Exception) action(exception)
        return this
    }

    /**
     * Returns true if this is a [Success].
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if this is an [Error].
     */
    val isError: Boolean get() = this is Error

    /**
     * Returns true if this is an [Exception].
     */
    val isException: Boolean get() = this is Exception
}

/**
 * Wraps a suspend Retrofit API call in an [ApiResult], catching common exceptions.
 *
 * Usage:
 * ```kotlin
 * val result = safeApiCall { userService.getUsers() }
 * when (result) {
 *     is ApiResult.Success -> println(result.data)
 *     is ApiResult.Error -> println("HTTP ${result.code}: ${result.message}")
 *     is ApiResult.Exception -> println("Network error: ${result.exception.message}")
 * }
 * ```
 *
 * @param T The type of the response body.
 * @param call The suspend function that performs the API call returning a [Response].
 * @return An [ApiResult] wrapping the result or error.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body, response.code())
            } else {
                ApiResult.Error(response.code(), "Response body is null")
            }
        } else {
            val errorBody = response.errorBody()?.use { it.string() }
            ApiResult.Error(response.code(), response.message(), errorBody)
        }
    } catch (e: HttpException) {
        ApiResult.Error(e.code(), e.message())
    } catch (e: Throwable) {
        ApiResult.Exception(e)
    }
}

/**
 * Wraps a suspend Retrofit API call whose body may legitimately be null (e.g. HTTP 204).
 *
 * A null body on a successful response is returned as [ApiResult.Success] with `null` data,
 * rather than being treated as an error.
 *
 * @param T The type of the response body.
 * @param call The suspend function performing the API call.
 * @return An [ApiResult] wrapping the result or error.
 */
suspend fun <T> safeApiCallNullable(call: suspend () -> Response<T>): ApiResult<T?> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            ApiResult.Success(response.body(), response.code())
        } else {
            val errorBody = response.errorBody()?.use { it.string() }
            ApiResult.Error(response.code(), response.message(), errorBody)
        }
    } catch (e: HttpException) {
        ApiResult.Error(e.code(), e.message())
    } catch (e: Throwable) {
        ApiResult.Exception(e)
    }
}

