package inc.kaizen.base.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class RetryInterceptorTest : FunSpec({

    lateinit var server: MockWebServer

    beforeEach {
        server = MockWebServer()
        server.start()
    }

    afterEach {
        server.shutdown()
    }

    test("no retry on successful response") {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 2, initialBackoffMillis = 50))
            .build()

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()
        response.code shouldBe 200
        server.requestCount shouldBe 1
    }

    test("retries on 500 for GET requests") {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 3, initialBackoffMillis = 50))
            .build()

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()
        response.code shouldBe 200
        server.requestCount shouldBe 3
    }

    test("does not retry on 400 client error") {
        server.enqueue(MockResponse().setResponseCode(400).setBody("bad request"))

        val client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 2, initialBackoffMillis = 50))
            .build()

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()
        response.code shouldBe 400
        server.requestCount shouldBe 1
    }

    test("returns last 500 response when retries exhausted") {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(502))

        val client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 1, initialBackoffMillis = 50))
            .build()

        val response = client.newCall(Request.Builder().url(server.url("/")).build()).execute()
        response.code shouldBe 502
        server.requestCount shouldBe 2
    }

    test("default configuration") {
        val interceptor = RetryInterceptor()
        // Just verify it can be created with defaults without exceptions
        interceptor shouldBe interceptor
    }
})

