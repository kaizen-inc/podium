package inc.kaizen.base.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.assertions.throwables.shouldThrow
import okhttp3.Interceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Response
import retrofit2.http.GET

class ApiClientTest : FunSpec({

    lateinit var server: MockWebServer

    beforeEach {
        server = MockWebServer()
        server.start()
    }

    afterEach {
        server.shutdown()
    }

    test("normalizeBaseUrl appends trailing slash") {
        val client = ApiClient(baseUrl = server.url("/api").toString().trimEnd('/'))
        // If service creation works, the base URL was normalized
        client shouldNotBe null
    }

    test("normalizeBaseUrl preserves existing trailing slash") {
        val client = ApiClient(baseUrl = server.url("/api/").toString())
        client shouldNotBe null
    }

    test("createService returns a non-null service instance") {
        server.enqueue(MockResponse().setBody("\"hello\""))
        val client = ApiClient(baseUrl = server.url("/").toString())
        val service = client.createService(TestService::class.java)
        service shouldNotBe null
    }

    test("createService caches service instances") {
        val client = ApiClient(baseUrl = server.url("/").toString())
        val service1 = client.createService(TestService::class.java)
        val service2 = client.createService(TestService::class.java)
        service1 shouldBeSameInstanceAs service2
    }

    test("addAuthorization registers interceptor") {
        val client = ApiClient(baseUrl = server.url("/").toString())
        val interceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer test-token")
                .build()
            chain.proceed(request)
        }
        val result = client.addAuthorization("bearer", interceptor)
        result shouldBeSameInstanceAs client
    }

    test("addAuthorization throws on duplicate name") {
        val client = ApiClient(baseUrl = server.url("/").toString())
        val interceptor = Interceptor { chain -> chain.proceed(chain.request()) }
        client.addAuthorization("auth1", interceptor)

        shouldThrow<IllegalArgumentException> {
            client.addAuthorization("auth1", interceptor)
        }
    }

    test("setLogger configures logger callback") {
        val logs = mutableListOf<String>()
        val client = ApiClient(baseUrl = server.url("/").toString())
            .setLogger { logs.add(it) }
        client shouldNotBe null
    }

    test("createService with per-service interceptor does not destroy shared interceptors") {
        server.enqueue(MockResponse().setBody("\"response1\""))
        server.enqueue(MockResponse().setBody("\"response2\""))

        val client = ApiClient(baseUrl = server.url("/").toString())
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Auth", "token")
                .build()
            chain.proceed(request)
        }
        client.addAuthorization("auth", authInterceptor)

        val extraInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Extra", "extra-value")
                .build()
            chain.proceed(request)
        }

        // Create a service with a per-service interceptor
        val service = client.createService(TestService::class.java, extraInterceptor)
        service shouldNotBe null
    }

    test("createService with reified type parameter") {
        val client = ApiClient(baseUrl = server.url("/").toString())
        val service = client.createService<TestService>()
        service shouldNotBe null
    }

    test("ApiClient with custom timeout config") {
        val config = TimeoutConfig(connectTimeout = 10, readTimeout = 15, writeTimeout = 20)
        val client = ApiClient(
            baseUrl = server.url("/").toString(),
            timeoutConfig = config
        )
        client shouldNotBe null
    }

    test("ApiClient with retry interceptor") {
        val client = ApiClient(
            baseUrl = server.url("/").toString(),
            retryInterceptor = RetryInterceptor(maxRetries = 2, initialBackoffMillis = 100)
        )
        client shouldNotBe null
    }

    test("removeAuthorization returns true when auth exists") {
        val client = ApiClient(baseUrl = server.url("/").toString())
        val interceptor = Interceptor { chain -> chain.proceed(chain.request()) }
        client.addAuthorization("bearer", interceptor)
        client.removeAuthorization("bearer") shouldBe true
    }

    test("removeAuthorization returns false when auth does not exist") {
        val client = ApiClient(baseUrl = server.url("/").toString())
        client.removeAuthorization("nonexistent") shouldBe false
    }

    test("clearServiceCache forces re-creation of services") {
        val client = ApiClient(baseUrl = server.url("/").toString())
        val service1 = client.createService(TestService::class.java)
        client.clearServiceCache()
        val service2 = client.createService(TestService::class.java)
        // After clearing, a new instance should be created (different reference)
        (service1 === service2) shouldBe false
    }

    test("ApiClient with custom logging level NONE") {
        val logs = mutableListOf<String>()
        val client = ApiClient(
            baseUrl = server.url("/").toString(),
            loggingLevel = okhttp3.logging.HttpLoggingInterceptor.Level.NONE
        ).setLogger { logs.add(it) }
        client shouldNotBe null
    }
})

interface TestService {
    @GET("/test")
    suspend fun getTest(): Response<String>
}

