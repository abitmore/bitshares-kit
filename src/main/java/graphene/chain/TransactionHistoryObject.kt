package graphene.chain

import graphene.protocol.SignedTransaction
import graphene.protocol.TransactionHistoryId
import graphene.protocol.TransactionHistoryIdType
import graphene.protocol.TransactionIdType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class K207_TransactionHistoryObject(
    @SerialName("id")
    override val id: TransactionHistoryId,
    @SerialName("trx")
    val transaction: SignedTransaction,
    @SerialName("trx_id")
    val transactionId: TransactionIdType,
) : AbstractObject(), TransactionHistoryIdType {
//    time_point_sec get_expiration()const { return trx.expiration; }
}
