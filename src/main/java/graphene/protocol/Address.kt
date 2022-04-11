package graphene.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class AddressType(
    @Transient val reserved: Unit = Unit,
): GrapheneComponent, Comparable<AddressType> {

    override fun compareTo(other: AddressType): Int = 0
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = other is AddressType
    override fun toString(): String = TODO()
}


