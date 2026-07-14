package com.example.mediabrowser.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

// Tolerant number serializers — older Gelbooru forks (Realbooru, TBIB,
// HypnoHub, etc.) return numeric fields as strings ("id":"12345") instead of
// numbers. kotlinx.serialization's isLenient flag doesn't convert those, so
// we accept either shape at the field level. This is what was making non-
// Rule34/Xbooru sites appear to "return nothing" — every post parse threw.

private object TolerantLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TolerantLong", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): Long {
        val jd = decoder as? JsonDecoder ?: return decoder.decodeLong()
        val el = jd.decodeJsonElement()
        val p = el as? JsonPrimitive ?: return 0L
        return p.content.toLongOrNull() ?: 0L
    }
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

private object TolerantIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TolerantInt", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Int {
        val jd = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val el = jd.decodeJsonElement()
        val p = el as? JsonPrimitive ?: return 0
        return p.content.toIntOrNull() ?: 0
    }
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

private object TolerantNullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TolerantNullableInt", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Int? {
        val jd = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val el = jd.decodeJsonElement()
        if (el is JsonNull) return null
        val p = el as? JsonPrimitive ?: return null
        return p.content.toIntOrNull()
    }
    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

@Serializable
data class ArchivePostDto(
    @SerialName("id") @Serializable(with = TolerantLongSerializer::class) val id: Long,
    @SerialName("preview_url") val previewUrl: String = "",
    @SerialName("file_url") val fileUrl: String = "",
    @SerialName("sample_url") val sampleUrl: String = "",
    @SerialName("width") @Serializable(with = TolerantIntSerializer::class) val width: Int = 0,
    @SerialName("height") @Serializable(with = TolerantIntSerializer::class) val height: Int = 0,
    @SerialName("score") @Serializable(with = TolerantIntSerializer::class) val score: Int = 0,
    @SerialName("tags") val tags: String = "",
    @SerialName("directory") @Serializable(with = TolerantNullableIntSerializer::class) val directory: Int? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("hash") val hash: String? = null,
    @SerialName("md5") val md5: String? = null
)
