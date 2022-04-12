package graphene.protocol

import graphene.extension.toHexByteArray
import graphene.extension.toHexString
import graphene.serializers.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

// threshold weight

typealias FutureExtensions = StatSet<@Serializable(with = FutureExtensionSerializer::class) Unit>

fun emptyExtension() = sortedSetOf<Unit>()

object FutureExtensionSerializer : StaticVarSerializer<Unit>(
    listOf(
        Unit::class
    )
)

typealias Vector<T> = List<T>
typealias FlatSet<T> = @Serializable(with = SortedSetSerializer::class) SortedSet<T>
typealias StatSet<T> = @Serializable(with = SortedSetSerializer::class) SortedSet<T>

typealias FlatMap<K, V> = @Serializable(with = FlatMapSerializer::class) SortedMap<K, V>
typealias TypeSet<T> = @Serializable(with = SortedSetSerializer::class) SortedSet<T>
typealias FlatPair<A, B> = @Serializable(with = FlatPairSerializer::class) Pair<A, B>

typealias PriceFeeds = FlatMap<AccountId, FlatPair<@Serializable(TimePointSecSerializer::class) Instant, PriceFeedWithIcr>>

//typealias Ripemd160 = String
//typealias Sha256 = String

typealias BlockIdType = Ripemd160 //typealias block_id_type = fc::ripemd160; TODO
typealias ChecksumType = Ripemd160 //typealias checksum_type = fc::ripemd160; TODO
typealias TransactionIdType = Ripemd160 //typealias transaction_id_type = fc::ripemd160; TODO
typealias DigestType = Sha256 //typealias digest_type = fc::sha256; TODO
typealias SignatureType = CompactSignature // fc::ecc::compact_signature; TODO
typealias ShareType = Int64 //typealias share_type = safe<int64_t>; TODO
typealias WeightType = UInt16 //typealias weight_type = uint16_t; TODO

// crypto
typealias BlindFactorType = Sha256 //typedef fc::sha256 blind_factor_type; TODO


@Serializable(with = CommitmentSerializer::class)
class CommitmentType(data: ByteArray) : ZeroInitializedArray(data, 33) //typedef zero_initialized_array<unsigned char,33> commitment_type; TODO char[33]
object CommitmentSerializer : BinaryDataLikeSerializer<CommitmentType>(CommitmentType::class)

@Serializable(with = PublicKeyDataSerializer::class)
class PublicKeyData(data: ByteArray) : ZeroInitializedArray(data, 33) //typedef zero_initialized_array<unsigned char,33> public_key_data;
object PublicKeyDataSerializer : BinaryDataLikeSerializer<PublicKeyData>(PublicKeyData::class)

//typedef fc::sha256                               private_key_secret;
//typedef zero_initialized_array<unsigned char,65> public_key_point_data; ///< the full non-compressed version of the ECC point
//typedef zero_initialized_array<unsigned char,72> signature;

@Serializable(with = CompactSignatureSerializer::class)
class CompactSignature(data: ByteArray) : ZeroInitializedArray(data, 65) //typedef zero_initialized_array<unsigned char,65> compact_signature;
object CompactSignatureSerializer : BinaryDataLikeSerializer<CompactSignature>(CompactSignature::class)

typealias RangeProofType = BinaryData //typedef std::vector<char>                        range_proof_type;
//typedef zero_initialized_array<unsigned char,78> extended_key_data;

//using private_key_type = fc::ecc::private_key;
typealias ChainIdType = Sha256  //using chain_id_type = fc::sha256;
//using ratio_type = boost::rational<int32_t>;

//typealias time_point_sec = @Serializable(with = TimePointSecSerializer::class) Instant
typealias time_point_sec = Instant // TODO: 2022/4/6


object LocalDateTimeSecSerializer: KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return decoder.decodeString().toLocalDateTime()
        TODO()
    }
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val rounded = Instant.fromEpochSeconds(value.toInstant(TimeZone.UTC).epochSeconds)
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(rounded.toLocalDateTime(TimeZone.UTC).toString())
            is IOEncoder -> encoder.encodeInt(rounded.epochSeconds.toInt())
            else -> TODO()
        }
    }
}

@Serializable(with = Ripemd160Serializer::class)
class Ripemd160(data: ByteArray) : BinaryData(data) {

    companion object {
        val SIZE = 160
    }
    init {
        require(data.size == 20)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Ripemd160
        if (!data.contentEquals(other.data)) return false
        return true
    }
    override fun hashCode(): Int = data.contentHashCode()
    override fun toString(): String = data.toHexString()
}
object Ripemd160Serializer : KSerializer<Ripemd160> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Ripemd160", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Ripemd160 =
        when (decoder) {
            is JsonDecoder -> Ripemd160(decoder.decodeString().toHexByteArray())
            else -> TODO()
        }
    override fun serialize(encoder: Encoder, value: Ripemd160) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.data.toHexString())
            is IOEncoder -> encoder.encodeByteArray(value.data)
            else -> TODO()
        }
}


@Serializable(with = Sha1Serializer::class)
class Sha1(data: ByteArray) : BinaryData(data) {
    init {
        require(data.size == 20)
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Sha1
        if (!data.contentEquals(other.data)) return false
        return true
    }
    override fun hashCode(): Int = data.contentHashCode()
    override fun toString(): String = data.toHexString()
}
object Sha1Serializer : KSerializer<Sha1> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Sha1", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Sha1 =
        when (decoder) {
            is JsonDecoder -> Sha1(decoder.decodeString().toHexByteArray())
            else -> TODO()
        }
    override fun serialize(encoder: Encoder, value: Sha1) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.data.toHexString())
            is IOEncoder -> encoder.encodeByteArray(value.data)
            else -> TODO()
        }
}

@Serializable(with = Sha256Serializer::class)
class Sha256(data: ByteArray) : BinaryData(data) {
    init {
        require(data.size == 32)
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Sha256
        if (!data.contentEquals(other.data)) return false
        return true
    }
    override fun hashCode(): Int = data.contentHashCode()
    override fun toString(): String = data.toHexString()
}
object Sha256Serializer : KSerializer<Sha256> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Sha256", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Sha256 =
        when (decoder) {
            is JsonDecoder -> Sha256(decoder.decodeString().toHexByteArray())
            else -> TODO()
        }
    override fun serialize(encoder: Encoder, value: Sha256) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.data.toHexString())
            is IOEncoder -> encoder.encodeByteArray(value.data)
            else -> TODO()
        }
}

@Serializable(with = Hash160Serializer::class)
class Hash160(data: ByteArray) : BinaryData(data) {
    init {
        require(data.size == 20)
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Hash160
        if (!data.contentEquals(other.data)) return false
        return true
    }
    override fun hashCode(): Int = data.contentHashCode()
    override fun toString(): String = data.toHexString()
}
object Hash160Serializer : KSerializer<Hash160> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Hash160", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Hash160 =
        when (decoder) {
            is JsonDecoder -> Hash160(decoder.decodeString().toHexByteArray())
            else -> TODO()
        }
    override fun serialize(encoder: Encoder, value: Hash160) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.data.toHexString())
            is IOEncoder -> encoder.encodeByteArray(value.data)
            else -> TODO()
        }
}



open class ZeroInitializedArray(data: ByteArray, size: Int) : BinaryData(data) {
    init {
        require(data.size == size)
    }
}



@Serializable(with = BinaryDataSerializer::class)
open class BinaryData(val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BinaryData
        if (!data.contentEquals(other.data)) return false
        return true
    }
    override fun hashCode(): Int = data.contentHashCode()
    override fun toString(): String = data.toHexString()
}

object BinaryDataSerializer : KSerializer<BinaryData> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BinaryData", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): BinaryData =
        when (decoder) {
            is JsonDecoder -> BinaryData(decoder.decodeString().toHexByteArray())
            else -> TODO()
        }
    override fun serialize(encoder: Encoder, value: BinaryData) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.data.toHexString())
            is IOEncoder -> {
                encoder.encodeVarInt(value.data.size)
                encoder.encodeByteArray(value.data)
            }
            else -> TODO()
        }
}

open class BinaryDataLikeSerializer<T: BinaryData>(
    private val clazz: KClass<T>
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Commitment", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): T =
        when (decoder) {
            is JsonDecoder -> clazz.primaryConstructor!!.call(decoder.decodeString().toHexByteArray())
            else -> TODO()
        }
    override fun serialize(encoder: Encoder, value: T) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.data.toHexString())
            is IOEncoder -> encoder.encodeByteArray(value.data)
            else -> TODO()
        }
}