import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

class Message(
    @JsonProperty("id") val id: Int,
    @JsonProperty("part") val part: Int,
    @JsonProperty("data") val data: String
) {
    override fun toString(): String {
        return "[ID: $id, Part: $part] $data"
    }

    fun json(): String {
        val mapper = ObjectMapper()
        return mapper.writeValueAsString(this)
    }

    companion object {
        fun fromJson(json: String): Message? {
            val mapper = ObjectMapper()
            return mapper.readValue(json, Message::class.java)
        }
    }
}
