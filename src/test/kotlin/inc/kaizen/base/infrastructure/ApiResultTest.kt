package inc.kaizen.base.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

class ApiResultTest : FunSpec({

    test("Success wraps data and code") {
        val result = ApiResult.Success("data", 200)
        result.isSuccess shouldBe true
        result.isError shouldBe false
        result.isException shouldBe false
        result.data shouldBe "data"
        result.code shouldBe 200
        result.getOrNull() shouldBe "data"
        result.getOrElse { "fallback" } shouldBe "data"
    }

    test("Error wraps code and message") {
        val result = ApiResult.Error(404, "Not Found", """{"error":"missing"}""")
        result.isSuccess shouldBe false
        result.isError shouldBe true
        result.isException shouldBe false
        result.code shouldBe 404
        result.message shouldBe "Not Found"
        result.errorBody shouldBe """{"error":"missing"}"""
        result.getOrNull() shouldBe null
    }

    test("Exception wraps throwable") {
        val ex = IOException("network down")
        val result = ApiResult.Exception(ex)
        result.isSuccess shouldBe false
        result.isError shouldBe false
        result.isException shouldBe true
        result.exception shouldBe ex
        result.getOrNull() shouldBe null
        (result as ApiResult<String>).getOrElse { "fallback" } shouldBe "fallback"
    }

    // --- map ---

    test("map transforms Success data") {
        val result = ApiResult.Success(42, 200).map { it * 2 }
        result shouldBe ApiResult.Success(84, 200)
    }

    test("map passes through Error unchanged") {
        val error = ApiResult.Error(500, "Server Error")
        val result: ApiResult<Int> = error.map { 1 }
        result shouldBe error
    }

    test("map passes through Exception unchanged") {
        val ex = RuntimeException("boom")
        val exception = ApiResult.Exception(ex)
        val result: ApiResult<Int> = exception.map { 1 }
        result shouldBe exception
    }

    // --- flatMap ---

    test("flatMap chains Success into another result") {
        val result = ApiResult.Success("hello", 200).flatMap { ApiResult.Success(it.length, 200) }
        result shouldBe ApiResult.Success(5, 200)
    }

    test("flatMap can return Error from transform") {
        val result: ApiResult<Int> = ApiResult.Success("hello", 200).flatMap { ApiResult.Error(422, "bad") }
        result shouldBe ApiResult.Error(422, "bad")
    }

    test("flatMap passes through Error without calling transform") {
        val error = ApiResult.Error(404, "Not Found")
        var called = false
        val result: ApiResult<Int> = error.flatMap { called = true; ApiResult.Success(1, 200) }
        result shouldBe error
        called shouldBe false
    }

    // --- onSuccess / onError / onException ---

    test("onSuccess executes action for Success and returns same result") {
        var captured: String? = null
        val result = ApiResult.Success("hi", 200).onSuccess { captured = it }
        captured shouldBe "hi"
        result shouldBe ApiResult.Success("hi", 200)
    }

    test("onSuccess does not execute action for Error") {
        var called = false
        ApiResult.Error(400, "Bad").onSuccess { called = true }
        called shouldBe false
    }

    test("onError executes action for Error and returns same result") {
        var capturedCode = 0
        val error = ApiResult.Error(503, "Unavailable")
        val result: ApiResult<String> = error.onError { capturedCode = it.code }
        capturedCode shouldBe 503
        result shouldBe error
    }

    test("onError does not execute action for Success") {
        var called = false
        ApiResult.Success("ok", 200).onError { called = true }
        called shouldBe false
    }

    test("onException executes action for Exception and returns same result") {
        val ex = RuntimeException("fail")
        var captured: Throwable? = null
        val result: ApiResult<String> = ApiResult.Exception(ex).onException { captured = it }
        captured shouldBe ex
        result shouldBe ApiResult.Exception(ex)
    }

    test("onException does not execute action for Success") {
        var called = false
        ApiResult.Success("ok", 200).onException { called = true }
        called shouldBe false
    }

    // --- safeApiCall ---

    test("safeApiCall returns Success for successful response") {
        runTest {
            val result = safeApiCall {
                Response.success("hello")
            }
            result.shouldBeInstanceOf<ApiResult.Success<String>>()
            result.data shouldBe "hello"
            result.code shouldBe 200
        }
    }

    test("safeApiCall returns Error for unsuccessful response") {
        runTest {
            val errorBody = """{"msg":"bad request"}""".toResponseBody("application/json".toMediaType())
            val result = safeApiCall<String> {
                Response.error(400, errorBody)
            }
            result.shouldBeInstanceOf<ApiResult.Error>()
            result.code shouldBe 400
        }
    }

    test("safeApiCall returns Exception for IOException") {
        runTest {
            val result = safeApiCall<String> {
                throw IOException("connection refused")
            }
            result.shouldBeInstanceOf<ApiResult.Exception>()
            result.exception.shouldBeInstanceOf<IOException>()
        }
    }

    test("safeApiCall returns Error for null body") {
        runTest {
            val result = safeApiCall<String> {
                Response.success(null)
            }
            result.shouldBeInstanceOf<ApiResult.Error>()
            result.message shouldBe "Response body is null"
        }
    }

    // --- safeApiCallNullable ---

    test("safeApiCallNullable returns Success with null body for 204-style response") {
        runTest {
            val result = safeApiCallNullable<String> {
                @Suppress("UNCHECKED_CAST")
                Response.success<String>(null)
            }
            result.shouldBeInstanceOf<ApiResult.Success<String?>>()
            result.data shouldBe null
        }
    }

    test("safeApiCallNullable returns Error for error response") {
        runTest {
            val errorBody = "not found".toResponseBody("text/plain".toMediaType())
            val result = safeApiCallNullable<String> {
                Response.error(404, errorBody)
            }
            result.shouldBeInstanceOf<ApiResult.Error>()
            result.code shouldBe 404
        }
    }
})

