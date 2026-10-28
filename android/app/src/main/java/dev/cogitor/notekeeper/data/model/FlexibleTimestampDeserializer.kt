package dev.cogitor.notekeeper.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/** Jackson may emit LocalDateTime as ISO string or as [y,m,d,h,min,s]. */
class FlexibleTimestampDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) return null
        if (json.isJsonPrimitive) return json.asString
        if (json.isJsonArray) {
            val a = json.asJsonArray
            if (a.size() >= 3) {
                fun n(i: Int) = if (i < a.size()) a[i].asInt else 0
                return "%04d-%02d-%02dT%02d:%02d:%02d".format(n(0), n(1), n(2), n(3), n(4), n(5))
            }
        }
        return json.toString()
    }
}
