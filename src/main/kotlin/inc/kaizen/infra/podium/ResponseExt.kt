package inc.kaizen.infra.podium

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import retrofit2.Response

@Throws(JsonParseException::class)
inline fun <reified T> Response<*>.getErrorResponse(serializerBuilder: GsonBuilder = Serializer.gsonBuilder): T? {
    val serializer = serializerBuilder.create()
    return errorBody()?.use { body ->
        val reader = body.charStream()
        serializer.fromJson(reader, T::class.java)
    }
}
