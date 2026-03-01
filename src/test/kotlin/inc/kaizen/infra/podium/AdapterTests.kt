package inc.kaizen.infra.podium

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class OffsetDateTimeAdapterTest : FunSpec({
    val adapter = OffsetDateTimeAdapter()

    test("write non-null OffsetDateTime") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw), OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC))
        sw.toString() shouldBe "\"2024-01-15T10:30:00Z\""
    }

    test("write null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw).apply { setSerializeNulls(true) }, null)
        sw.toString() shouldBe "null"
    }

    test("read valid") {
        val result = adapter.read(JsonReader(StringReader("\"2024-01-15T10:30:00Z\"")))
        result shouldBe OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC)
    }

    test("read null token") {
        adapter.read(JsonReader(StringReader("null"))) shouldBe null
    }

    test("read null reader") {
        adapter.read(null) shouldBe null
    }
})

class LocalDateTimeAdapterTest : FunSpec({
    val adapter = LocalDateTimeAdapter()

    test("write non-null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw), LocalDateTime.of(2024, 1, 15, 10, 30))
        sw.toString() shouldBe "\"2024-01-15T10:30:00\""
    }

    test("write null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw).apply { setSerializeNulls(true) }, null)
        sw.toString() shouldBe "null"
    }

    test("read valid") {
        adapter.read(JsonReader(StringReader("\"2024-01-15T10:30:00\""))) shouldBe LocalDateTime.of(2024, 1, 15, 10, 30)
    }

    test("read null token") {
        adapter.read(JsonReader(StringReader("null"))) shouldBe null
    }

    test("read null reader") {
        adapter.read(null) shouldBe null
    }
})

class LocalDateAdapterTest : FunSpec({
    val adapter = LocalDateAdapter()

    test("write non-null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw), LocalDate.of(2024, 1, 15))
        sw.toString() shouldBe "\"2024-01-15\""
    }

    test("write null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw).apply { setSerializeNulls(true) }, null)
        sw.toString() shouldBe "null"
    }

    test("read valid") {
        adapter.read(JsonReader(StringReader("\"2024-01-15\""))) shouldBe LocalDate.of(2024, 1, 15)
    }

    test("read null token") {
        adapter.read(JsonReader(StringReader("null"))) shouldBe null
    }

    test("read null reader") {
        adapter.read(null) shouldBe null
    }
})

class ByteArrayAdapterTest : FunSpec({
    val adapter = ByteArrayAdapter()

    test("write non-null encodes as Base64") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw), "hello".toByteArray())
        sw.toString() shouldBe "\"aGVsbG8=\""
    }

    test("write null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw).apply { setSerializeNulls(true) }, null)
        sw.toString() shouldBe "null"
    }

    test("read valid Base64 decodes correctly") {
        val result = adapter.read(JsonReader(StringReader("\"aGVsbG8=\"")))
        String(result!!) shouldBe "hello"
    }

    test("round-trip encode and decode") {
        val original = byteArrayOf(0x00, 0x01, 0xFF.toByte(), 0xFE.toByte())
        val sw = StringWriter()
        adapter.write(JsonWriter(sw), original)
        val decoded = adapter.read(JsonReader(StringReader(sw.toString())))
        decoded shouldBe original
    }

    test("read null token") {
        adapter.read(JsonReader(StringReader("null"))) shouldBe null
    }

    test("read null reader") {
        adapter.read(null) shouldBe null
    }
})

class DateAdapterTest : FunSpec({
    val adapter = DateAdapter()

    test("write non-null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw), java.util.Date(1705312200000L))
        sw.toString().length shouldNotBe 0
    }

    test("write null") {
        val sw = StringWriter()
        adapter.write(JsonWriter(sw).apply { setSerializeNulls(true) }, null)
        sw.toString() shouldBe "null"
    }

    test("read null token") {
        adapter.read(JsonReader(StringReader("null"))) shouldBe null
    }

    test("read null reader") {
        adapter.read(null) shouldBe null
    }
})

