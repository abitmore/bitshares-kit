package graphene.protocol

import graphene.serializers.StaticVarSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = HtlcHashSerializer::class)
sealed class HtlcHash

@Serializable(with = HtlcAlgoRipemd160Serializer::class)
data class HtlcAlgoRipemd160(
    val hash: Ripemd160
): HtlcHash() {
    override fun toString(): String = hash.toString()
}

@Serializable(with = HtlcAlgoSha1Serializer::class)
data class HtlcAlgoSha1(
    val hash: Sha1
): HtlcHash() {
    override fun toString(): String = hash.toString()
}

@Serializable(with = HtlcAlgoSha256Serializer::class)
data class HtlcAlgoSha256(
    val hash: Sha256
): HtlcHash() {
    override fun toString(): String = hash.toString()
}

@Serializable(with = HtlcAlgoHash160Serializer::class)
data class HtlcAlgoHash160(
    val hash: Hash160
): HtlcHash() {
    override fun toString(): String = hash.toString()
}

object HtlcHashSerializer : StaticVarSerializer<HtlcHash>(
    listOf(
        HtlcAlgoRipemd160::class,
        HtlcAlgoSha1::class,
        HtlcAlgoSha256::class,
        HtlcAlgoHash160::class,
    )
)

object HtlcAlgoRipemd160Serializer: KSerializer<HtlcAlgoRipemd160> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HtlcAlgoRipemd160", PrimitiveKind.STRING)
    private val elementSerializer = Ripemd160.serializer()
    override fun serialize(encoder: Encoder, value: HtlcAlgoRipemd160) = elementSerializer.serialize(encoder, value.hash)
    override fun deserialize(decoder: Decoder): HtlcAlgoRipemd160 = HtlcAlgoRipemd160(elementSerializer.deserialize(decoder))
}

object HtlcAlgoSha1Serializer: KSerializer<HtlcAlgoSha1> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HtlcAlgoSha1", PrimitiveKind.STRING)
    private val elementSerializer = Sha1.serializer()
    override fun serialize(encoder: Encoder, value: HtlcAlgoSha1) = elementSerializer.serialize(encoder, value.hash)
    override fun deserialize(decoder: Decoder): HtlcAlgoSha1 = HtlcAlgoSha1(elementSerializer.deserialize(decoder))
}

object HtlcAlgoSha256Serializer: KSerializer<HtlcAlgoSha256> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HtlcAlgoSha256", PrimitiveKind.STRING)
    private val elementSerializer = Sha256.serializer()
    override fun serialize(encoder: Encoder, value: HtlcAlgoSha256) = elementSerializer.serialize(encoder, value.hash)
    override fun deserialize(decoder: Decoder): HtlcAlgoSha256 = HtlcAlgoSha256(elementSerializer.deserialize(decoder))
}

object HtlcAlgoHash160Serializer: KSerializer<HtlcAlgoHash160> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HtlcAlgoHash160", PrimitiveKind.STRING)
    private val elementSerializer = Hash160.serializer()
    override fun serialize(encoder: Encoder, value: HtlcAlgoHash160) = elementSerializer.serialize(encoder, value.hash)
    override fun deserialize(decoder: Decoder): HtlcAlgoHash160 = HtlcAlgoHash160(elementSerializer.deserialize(decoder))
}