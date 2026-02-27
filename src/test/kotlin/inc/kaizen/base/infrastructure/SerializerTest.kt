package inc.kaizen.base.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date

class SerializerTest : FunSpec({

    test("gson should not be null") {
        Serializer.gson shouldNotBe null
    }

    test("gsonBuilder should not be null") {
        Serializer.gsonBuilder shouldNotBe null
    }

    test("round-trip OffsetDateTime serialization") {
        val original = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC)
        val json = Serializer.gson.toJson(original)
        val deserialized = Serializer.gson.fromJson(json, OffsetDateTime::class.java)
        deserialized shouldBe original
    }

    test("round-trip LocalDateTime serialization") {
        val original = LocalDateTime.of(2024, 6, 15, 10, 30, 0)
        val json = Serializer.gson.toJson(original)
        val deserialized = Serializer.gson.fromJson(json, LocalDateTime::class.java)
        deserialized shouldBe original
    }

    test("round-trip LocalDate serialization") {
        val original = LocalDate.of(2024, 6, 15)
        val json = Serializer.gson.toJson(original)
        val deserialized = Serializer.gson.fromJson(json, LocalDate::class.java)
        deserialized shouldBe original
    }

    test("round-trip ByteArray serialization") {
        val original = "hello world".toByteArray()
        val json = Serializer.gson.toJson(original)
        val deserialized = Serializer.gson.fromJson(json, ByteArray::class.java)
        String(deserialized) shouldBe String(original)
    }

    test("round-trip Date serialization") {
        val original = Date(1718451000000L) // A specific timestamp
        val json = Serializer.gson.toJson(original)
        val deserialized = Serializer.gson.fromJson(json, Date::class.java)
        deserialized shouldNotBe null
    }

    test("null OffsetDateTime serialization") {
        val json = Serializer.gson.toJson(null, OffsetDateTime::class.java)
        json shouldBe "null"
    }

    test("null LocalDateTime serialization") {
        val json = Serializer.gson.toJson(null, LocalDateTime::class.java)
        json shouldBe "null"
    }

    test("null LocalDate serialization") {
        val json = Serializer.gson.toJson(null, LocalDate::class.java)
        json shouldBe "null"
    }

    test("null ByteArray serialization") {
        val json = Serializer.gson.toJson(null, ByteArray::class.java)
        json shouldBe "null"
    }
})

