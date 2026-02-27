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
})

