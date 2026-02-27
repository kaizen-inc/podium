# Podium

A base Kotlin infrastructure library for building Retrofit2-based API clients with built-in Gson serialization and Java 8+ date/time support.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.8.10-blue.svg)](https://kotlinlang.org)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.9.0-green.svg)](https://square.github.io/retrofit/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Overview

Podium provides a reusable foundation for creating type-safe HTTP API clients in Kotlin. It wraps [Retrofit2](https://square.github.io/retrofit/) with sensible defaults, including:

- **Configurable `ApiClient`** – a fluent builder for Retrofit services with support for custom `OkHttpClient`, `GsonBuilder`, `Call.Factory`, and `Converter.Factory`.
- **Authorization interceptors** – easily attach named OkHttp interceptors for authentication.
- **HTTP logging** – body-level logging via OkHttp's `HttpLoggingInterceptor` with a pluggable logger callback.
- **Gson date/time adapters** – out-of-the-box serialization for `OffsetDateTime`, `LocalDateTime`, `LocalDate`, `Date`, and `ByteArray`.
- **Response extensions** – a `Response<*>.getErrorResponse<T>()` extension for deserializing error bodies.
- **Collection format helpers** – `CSVParams`, `SSVParams`, `TSVParams`, `PIPESParams`, and `SPACEParams` for formatting query parameter collections.

## Installation

Podium is published to **GitHub Packages**. Add the repository and dependency to your project:

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/kaizen-inc/Podium")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("inc.kaizen.base:podium:0.1")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/kaizen-inc/Podium")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'inc.kaizen.base:podium:0.1'
}
```

> **Note:** You need a GitHub personal access token with the `read:packages` scope. Set `GITHUB_ACTOR` and `GITHUB_TOKEN` as environment variables or in your `~/.gradle/gradle.properties`.

## Usage

### Creating an API Client

```kotlin
import inc.kaizen.base.infrastructure.ApiClient

val client = ApiClient(baseUrl = "https://api.example.com")
```

### Creating a Retrofit Service

Define a Retrofit service interface as usual and then create it through the client:

```kotlin
import retrofit2.http.GET
import retrofit2.Response

interface UserService {
    @GET("users")
    suspend fun getUsers(): Response<List<User>>
}

val userService = client.createService(UserService::class.java)
```

### Adding Authorization

Attach named OkHttp interceptors for authentication (e.g., bearer tokens):

```kotlin
import okhttp3.Interceptor

val authInterceptor = Interceptor { chain ->
    val request = chain.request().newBuilder()
        .addHeader("Authorization", "Bearer <token>")
        .build()
    chain.proceed(request)
}

val client = ApiClient(baseUrl = "https://api.example.com")
    .addAuthorization("bearer", authInterceptor)
```

### Custom Logger

Plug in a custom logger to observe HTTP traffic:

```kotlin
val client = ApiClient(baseUrl = "https://api.example.com")
    .setLogger { message -> println(message) }
```

### Parsing Error Responses

Use the `getErrorResponse` extension to deserialize error bodies:

```kotlin
import inc.kaizen.base.infrastructure.getErrorResponse

data class ApiError(val code: Int, val message: String)

val response = userService.getUsers()
if (!response.isSuccessful) {
    val error = response.getErrorResponse<ApiError>()
    println(error?.message)
}
```

### Customizing Serialization

Provide your own `GsonBuilder` to customize serialization:

```kotlin
import com.google.gson.GsonBuilder

val customGsonBuilder = GsonBuilder()
    .setPrettyPrinting()
    .serializeNulls()

val client = ApiClient(
    baseUrl = "https://api.example.com",
    serializerBuilder = customGsonBuilder
)
```

The default `Serializer` already registers adapters for:

| Type | Adapter | Default Format |
|---|---|---|
| `OffsetDateTime` | `OffsetDateTimeAdapter` | `DateTimeFormatter.ISO_OFFSET_DATE_TIME` |
| `LocalDateTime` | `LocalDateTimeAdapter` | `DateTimeFormatter.ISO_LOCAL_DATE_TIME` |
| `LocalDate` | `LocalDateAdapter` | `DateTimeFormatter.ISO_LOCAL_DATE` |
| `ByteArray` | `ByteArrayAdapter` | UTF-8 string |

### Collection Formats

Format query parameter collections using the helpers in `CollectionFormats`:

```kotlin
import inc.kaizen.base.infrastructure.CollectionFormats.*

val csv = CSVParams("a", "b", "c")   // "a,b,c"
val ssv = SSVParams("a", "b", "c")   // "a b c"
val tsv = TSVParams("a", "b", "c")   // "a\tb\tc"
val pipes = PIPESParams("a", "b", "c") // "a|b|c"
```

## Project Structure

```
src/main/kotlin/inc/kaizen/base/infrastructure/
├── ApiClient.kt              # Core Retrofit client builder
├── ByteArrayAdapter.kt       # Gson adapter for ByteArray
├── CollectionFormats.kt      # Query parameter collection formatters
├── DateAdapter.kt            # Gson adapter for java.util.Date
├── LocalDateAdapter.kt       # Gson adapter for LocalDate
├── LocalDateTimeAdapter.kt   # Gson adapter for LocalDateTime
├── OffsetDateTimeAdapter.kt  # Gson adapter for OffsetDateTime
├── ResponseExt.kt            # Response extension for error deserialization
└── Serializer.kt             # Pre-configured GsonBuilder with all adapters
```

## Building

```bash
./gradlew build
```

## Testing

Tests use [Kotest](https://kotest.io/) and [MockK](https://mockk.io/):

```bash
./gradlew test
```

## Requirements

- **JDK** 8+
- **Kotlin** 1.8.10+

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

© 2023 Kaizen, Inc.
