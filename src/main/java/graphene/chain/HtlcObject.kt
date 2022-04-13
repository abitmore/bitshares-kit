package graphene.chain

import graphene.protocol.*
import graphene.serializers.TimePointSecSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class K116_HtlcObject(
    @SerialName("id")
    override val id: HtlcId,
    @SerialName("transfer")
    val transfer: TransferInfo,
    @SerialName("conditions")
    val conditions: ConditionInfo,
    @SerialName("memo")
    val memo: Optional<MemoData> = optional(),
) : AbstractObject(), HtlcIdType {

    @Serializable
    data class TransferInfo(
        @SerialName("from") val from: AccountIdType,
        @SerialName("to") val to: AccountIdType,
        @SerialName("amount") val amount: ShareType,
        @SerialName("asset_id") val asset: AssetIdType,
    )
    @Serializable
    data class ConditionInfo(
        @SerialName("hash_lock") val hashLock: HashLockInfo,
        @SerialName("time_lock") val timeLock: TimePointSec,
    ) {
        @Serializable
        data class HashLockInfo(
            @SerialName("preimage_hash") val preimageHash: HtlcHash,
            @SerialName("preimage_size") val preimageSize: UShort,
        )
        @Serializable
        data class TimePointSec(
            @SerialName("expiration") @Serializable(TimePointSecSerializer::class)
            val expiration: Instant,
        )
    }
}



