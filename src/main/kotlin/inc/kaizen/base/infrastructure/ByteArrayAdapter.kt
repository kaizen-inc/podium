package inc.kaizen.base.infrastructure

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.Base64

/**
 * Gson [TypeAdapter] for [ByteArray] that encodes to/from Base64 strings.
 *
 * Base64 is the standard wire format for binary data in JSON APIs (RFC 7517 / OpenAPI).
 */
class ByteArrayAdapter : TypeAdapter<ByteArray>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter?, value: ByteArray?) {
        if (value == null) {
            out?.nullValue()
        } else {
            out?.value(Base64.getEncoder().encodeToString(value))
        }
    }

    @Throws(IOException::class)
    override fun read(out: JsonReader?): ByteArray? {
        out ?: return null
        return when (out.peek()) {
            NULL -> {
                out.nextNull()
                null
            }
            else -> Base64.getDecoder().decode(out.nextString())
        }
    }
}
