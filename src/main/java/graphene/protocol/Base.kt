package graphene.protocol

import graphene.serializers.IOEncoder
import graphene.serializers.ObjectIdDefaultSerializer
import graphene.serializers.StaticVarSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder

internal val bytesComparator = Comparator<ByteArray> { o1, o2 ->
    var i = 0
    var j = 0
    while (i < o1.size && j < o2.size) {
        val a: Int = o1[i].toInt() and 0xff
        val b: Int = o2[j].toInt() and 0xff
        if (a != b) {
            return@Comparator a - b
        }
        i++
        j++
    }
    return@Comparator o1.size - o2.size
}

@InheritableSerialInfo
annotation class SerializeIndex

@Serializable(with = ExtensionSerializer::class) @SerializeIndex
interface Extension<T> : GrapheneComponent

class ExtensionSerializer<T: Extension<T>>(val elementSerializer: KSerializer<T>) : KSerializer<Extension<T>> {
    override val descriptor: SerialDescriptor = elementSerializer.descriptor
    override fun deserialize(decoder: Decoder): Extension<T> {
        return elementSerializer.deserialize(decoder)
    }
    override fun serialize(encoder: Encoder, value: Extension<T>) {
        when (encoder) {
            is JsonEncoder -> elementSerializer.serialize(encoder, value as T)
            is IOEncoder -> elementSerializer.serialize(encoder, value as T)
        }
    }
}

@Serializable(with = OperationResultSerializer::class)
sealed class OperationResult

@Serializable
data class VoidResult(
    @Transient val reserved: Unit = Unit,
) : OperationResult()

@Serializable(with = ObjectIdResultSerializer::class)
data class ObjectIdResult(
    @SerialName("id") val id: ObjectIdType,
) : OperationResult()

@Serializable(with = AssetResultSerializer::class)
data class AssetResult(
    @SerialName("asset") val asset: Asset,
) : OperationResult()

@Serializable
data class GenericOperationResult(
    @SerialName("new_objects") val newObjects: FlatSet<ObjectIdType>,
    @SerialName("updated_objects") val updatedObjects: FlatSet<ObjectIdType>,
    @SerialName("removed_objects") val removedObjects: FlatSet<ObjectIdType>,
) : OperationResult()

@Serializable
data class GenericExchangeOperationResult(
    @SerialName("paid") val paid: List<Asset>,
    @SerialName("received") val received: List<Asset>,
    @SerialName("fees") val fees: List<Asset>,
) : OperationResult()

@Serializable
data class ExtendableOperationResultDtl(
    @SerialName("impacted_accounts") val impactedAccounts: Optional<FlatSet<AccountIdType>> = optional(),
    @SerialName("new_objects") val newObjects: Optional<FlatSet<ObjectIdType>> = optional(),
    @SerialName("updated_objects") val updatedObjects: Optional<FlatSet<ObjectIdType>> = optional(),
    @SerialName("removed_objects") val removedObjects: Optional<FlatSet<ObjectIdType>> = optional(),
    @SerialName("paid") val paid: Optional<List<Asset>> = optional(),
    @SerialName("received") val received: Optional<List<Asset>> = optional(),
    @SerialName("fees") val fees: Optional<List<Asset>> = optional(),
) : OperationResult()

object OperationResultSerializer : StaticVarSerializer<OperationResult>(
    listOf(
        /* 0 */ VoidResult::class,
        /* 1 */ ObjectIdResult::class,
        /* 2 */ AssetResult::class,
        /* 3 */ GenericOperationResult::class,
        /* 4 */ GenericExchangeOperationResult::class,
        /* 5 */ ExtendableOperationResultDtl::class,
    )
)

internal object ObjectIdResultSerializer : KSerializer<ObjectIdResult> {
    val elementSerializer = ObjectIdDefaultSerializer
    override val descriptor: SerialDescriptor = elementSerializer.descriptor
    override fun serialize(encoder: Encoder, value: ObjectIdResult) =
        elementSerializer.serialize(encoder, value.id.id)
    override fun deserialize(decoder: Decoder): ObjectIdResult =
        ObjectIdResult(elementSerializer.deserialize(decoder))
}

internal object AssetResultSerializer : KSerializer<AssetResult> {
    val elementSerializer = Asset.serializer()
    override val descriptor: SerialDescriptor = elementSerializer.descriptor
    override fun serialize(encoder: Encoder, value: AssetResult) =
        elementSerializer.serialize(encoder, value.asset)
    override fun deserialize(decoder: Decoder): AssetResult =
        AssetResult(elementSerializer.deserialize(decoder))
}
