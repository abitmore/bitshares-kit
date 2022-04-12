package graphene.chain

import graphene.protocol.GRAPHENE_DEFAULT_MIN_COMMITTEE_MEMBER_COUNT
import graphene.protocol.GRAPHENE_DEFAULT_MIN_WITNESS_COUNT
import graphene.protocol.UInt16
import graphene.protocol.UInt32
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImmutableChainParameters(
    @SerialName("min_committee_member_count")
    val minCommitteeMemberCount: UInt16 = GRAPHENE_DEFAULT_MIN_COMMITTEE_MEMBER_COUNT,
    @SerialName("min_witness_count")
    val minWitnessCount: UInt16 = GRAPHENE_DEFAULT_MIN_WITNESS_COUNT,
    @SerialName("num_special_accounts")
    val numSpecialAccounts: UInt32 = 0U,
    @SerialName("num_special_assets")
    val numSpecialAssets: UInt32 = 0U,
) {
}