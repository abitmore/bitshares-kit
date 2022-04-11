package graphene.protocol

import graphene.serializers.VoteIdTypeSerializer
import kotlinx.serialization.Serializable

@Serializable(with = VoteIdTypeSerializer::class)
@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
data class VoteIdType(
    val type: VoteType = VoteType.COMMITTEE,
    val instance: UInt32 = 0U,
) : GrapheneComponent, Comparable<VoteIdType> {

    enum class VoteType {
        COMMITTEE,
        WITNESS,
        WORKER,
    }

    val content: UInt32 = instance shl 8 or type.ordinal.toUInt()
    override fun toString(): String {
        return "${type.ordinal}:${instance}"
    }
    override fun compareTo(other: VoteIdType): Int {
        return content.compareTo(other.content)
    }
    override fun hashCode(): Int = content.hashCode()
    override fun equals(other: Any?): Boolean = other is VoteIdType && content == other.content

}