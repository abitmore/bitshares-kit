package graphene.serializers

import graphene.protocol.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder

object VoteIdTypeSerializer : KSerializer<VoteIdType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("VoteIdType", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): VoteIdType =
        decoder.decodeString().toVote()
    override fun serialize(encoder: Encoder, value: VoteIdType) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.toString())
            is IOEncoder -> UInt32.serializer().serialize(encoder, value.content)
            else -> TODO()
        }
}

object TimePointSecSerializer: KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Instant {
        return decoder.decodeString().toLocalDateTime().toInstant(TimeZone.UTC)
    }
    override fun serialize(encoder: Encoder, value: Instant) {
        val rounded = Instant.fromEpochSeconds(value.epochSeconds)
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(rounded.toLocalDateTime(TimeZone.UTC).toString())
            is IOEncoder -> encoder.encodeInt(rounded.epochSeconds.toInt())
            else -> throw Error("Operation for $encoder is not implemented.")
        }
    }
}

class OptionalSerializer<T>(
    private val elementSerializer: KSerializer<T>
) : KSerializer<Optional<T>> {

    override val descriptor: SerialDescriptor = elementSerializer.descriptor
    override fun deserialize(decoder: Decoder): Optional<T> {
        return try {
            elementSerializer.deserialize(decoder).toOptional()
        } catch (e: Throwable) {
            e.printStackTrace()
            optional(null)
        }
    }
    override fun serialize(encoder: Encoder, value: Optional<T>) {
        when (encoder) {
            is JsonEncoder -> if (value.isPresent) elementSerializer.serialize(encoder, value.value)
            is IOEncoder -> {
                encoder.encodePresence(value.isPresent)
                if (value.isPresent) elementSerializer.serialize(encoder, value.value)
            }
            else -> {
                TODO()
            }
        }
    }

}