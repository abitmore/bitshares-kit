package graphene.chain

import graphene.protocol.*
import graphene.serializers.TimePointSecSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class K117_CustomAuthorityObject(
    @SerialName("id")
    override val id: CustomAuthorityId,
    @SerialName("account")
    val account: AccountIdType,
    @SerialName("enabled")
    val enabled: Boolean,
    @SerialName("idvalid_from") @Serializable(TimePointSecSerializer::class)
    val validFrom: Instant,
    @SerialName("valid_to") @Serializable(TimePointSecSerializer::class)
    val validTo: Instant,
    @SerialName("operation_type")
    val operationType: UnsignedInt,
    @SerialName("auth")
    val auth: Authority,
    @SerialName("restrictions")
    val restrictions: FlatMap<UInt16, Restriction>,
    @SerialName("restriction_counter")
    val restrictionCounter: UInt16 = 0U,
) : AbstractObject(), CustomAuthorityIdType
