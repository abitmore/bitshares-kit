package graphene.protocol

import graphene.serializers.TimePointSecSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/*  0 */ 
@Serializable
data class TransferOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("from") val from: AccountIdType, // Account to transfer asset from
    @SerialName("to") val to: AccountIdType, // Account to transfer asset to
    @SerialName("amount") val amount: Asset, // The amount of asset to transfer from @ref from to @ref to
    @SerialName("memo") val memo: Optional<MemoData> = optional(), // User provided data encrypted to the memo key of the "to" account
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/*  1 */ 
@Serializable
data class LimitOrderCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("seller") val account: AccountIdType,
    @SerialName("amount_to_sell") val amountToSell: Asset,
    @SerialName("min_to_receive") val minToReceive: Asset,
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("expiration") val expiration: Instant, // = MAXIMUM // The order will be removed from the books if not filled by expiration // Upon expiration, all unsold asset will be returned to seller
    @SerialName("fill_or_kill") val fillOrKill: Boolean = false,  // If this flag is set the entire order must be filled or the operation is rejected
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/*  2 */ 
@Serializable
data class LimitOrderCancelOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("fee_paying_account") val account: AccountIdType, // must be order->seller
    @SerialName("order") val order: LimitOrderIdType,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/*  3 */
@Serializable
data class CallOrderUpdateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("funding_account") val account: AccountIdType,// pays fee, collateral, and cover
    @SerialName("delta_collateral") val deltaCollateral: Asset,// the amount of collateral to add to the margin position
    @SerialName("delta_debt") val deltaDebt: Asset,// the amount of the debt to be paid off, may be negative to issue new debt
    @SerialName("extensions") val extensions: Extensions,
) : Operation() {
    //    typealias extensions_type = extension<options_type>, // note: this will be jsonified to {...} but no longer [...]
    @Serializable
    data class Extensions(
        @SerialName("target_collateral_ratio") val targetCollateralRatio: Optional<UInt16> = optional() // maximum CR to maintain when selling collateral on margin call
    ): Extension<Extensions>
}

/*  4 */
@Serializable
data class FillOrderOperation(
    @SerialName("fee") val fee: Asset, // paid by receiving account
    @SerialName("order_id") val order: ObjectIdType,
    @SerialName("account_id") val account: AccountIdType,
    @SerialName("pays") val pays: Asset,
    @SerialName("receives") val receives: Asset,
    @SerialName("fill_price") val fillPrice: PriceType,
    @SerialName("is_maker") val isMaker: Boolean,
) : Operation()           // Virtual

/*  5 */
@Serializable
data class AccountCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("registrar") val registrar: AccountIdType, // This account pays the fee. Must be a lifetime member.
    @SerialName("referrer") val referrer: AccountIdType, // This account receives a portion of the fee split between registrar and referrer. Must be a member.
    @SerialName("referrer_percent") val referrerPercent: UInt16, // = 0/ / Of the fee split between registrar and referrer, this percentage goes to the referrer. The rest goes to the registrar.
    @SerialName("name") val name: String,
    @SerialName("owner") val owner: Authority,
    @SerialName("active") val active: Authority,
    @SerialName("options") val options: AccountOptions,
    @SerialName("extensions") val extensions: Extensions,
) : Operation() {

    @Serializable
    data class Extensions(
        @SerialName("null_ext") val nullExt: Optional<Unit> = optional(),
        @SerialName("owner_special_authority") val ownerSpecialAuthority: Optional<SpecialAuthority> = optional(),
        @SerialName("active_special_authority") val activeSpecialAuthority: Optional<SpecialAuthority> = optional(),
        @SerialName("buyback_options") val buybackOptions: Optional<BuybackAccountOptions> = optional(),
    ): Extension<Extensions>
}

/*  6 */
@Serializable
data class AccountUpdateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("account") val account: AccountIdType, // The account to update
    @SerialName("owner") val owner: Optional<Authority> = optional(), // New owner authority. If set, this operation requires owner authority to execute.
    @SerialName("active") val active: Optional<Authority> = optional(), // New active authority. This can be updated by the current active authority.
    @SerialName("new_options") val newOptions: Optional<AccountOptions> = optional(), // New account options
    @SerialName("extensions") val extensions: Extensions,
) : Operation() {
    @Serializable
    data class Extensions(
        @SerialName("null_ext") val nullExt: Optional<Unit> = optional(),
        @SerialName("owner_special_authority") val ownerSpecialAuthority: Optional<SpecialAuthority> = optional(),
        @SerialName("active_special_authority") val activeSpecialAuthority: Optional<SpecialAuthority> = optional(),
    ) : Extension<Extensions>
}

/*  7 */ 
@Serializable
data class AccountWhitelistOperation(
    @SerialName("fee") val fee: Asset, // Paid by authorizing_account
    @SerialName("authorizing_account") val account: AccountIdType, // The account which is specifying an opinion of another account
    @SerialName("account_to_list") val target: AccountIdType, // The account being opined about
    @SerialName("new_listing") val type: UInt8, // = AccountListing.NO_LISTING // The new white and blacklist status of account_to_list, as determined by authorizing_account // This is a bitfield using values defined in the account_listing enum
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation() {
    @Serializable(with = AccountListingSerializer::class)
    enum class AccountListing(val value: UInt8) {
        NO_LISTING(0x00U), // No opinion is specified about this account
        WHITE_LISTED(0x01U), // This account is whitelisted, but not blacklisted
        BLACK_LISTED(0x02U), // This account is blacklisted, but not whitelisted
        WHITE_AND_BLACK_LISTED(0x03U) // This account is both whitelisted and blacklisted
    }
    object AccountListingSerializer : KSerializer<AccountListing> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AccountListing", PrimitiveKind.SHORT)
        override fun serialize(encoder: Encoder, value: AccountListing) =
            UInt8.serializer().serialize(encoder, value.value)
        override fun deserialize(decoder: Decoder): AccountListing =
            AccountListing.values()[UInt8.serializer().deserialize(decoder).toInt()]
    }
}

/*  8 */ 
@Serializable
data class AccountUpgradeOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("account_to_upgrade") val account: AccountIdType, // The account to upgrade; must not already be a lifetime member
    @SerialName("upgrade_to_lifetime_member") val isLifetime: Boolean, // If true, the account will be upgraded to a lifetime member; otherwise, it will add a year to the subscription
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // = false
) : Operation()

/*  9 */ 
@Serializable
data class AccountTransferOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("account_id") val account: AccountIdType,
    @SerialName("new_owner") val newOwner: AccountIdType,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 10 */ 
@Serializable
data class AssetCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType, // This account must sign and pay the fee for this operation. Later, this account may update the asset
    @SerialName("symbol") val symbol: String, // The ticker symbol of this asset
    @SerialName("precision") val precision: UInt8, // = 0 // Number of digits to the right of decimal point, must be less than or equal to 12
    // Options common to all assets.
    // @note common_options.core_exchange_rate technically needs to store the asset ID of this new asset. Since this
    // ID is not known at the time this operation is created, create this price as though the new asset has instance
    // ID 1, and the chain will overwrite it with the new asset's ID.
    @SerialName("common_options") val assetOptions: AssetOptions,
    @SerialName("bitasset_opts") val bitassetOptions: Optional<BitassetOptions> = optional(), // Options only available for BitAssets. MUST be non-null if and only if the asset is market-issued.
    @SerialName("is_prediction_market") val isPredictionMarket: Boolean, // = false, // For BitAssets, set this to true if the asset implements a prediction market; false otherwise
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 11 */
@Serializable
data class AssetUpdateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType,
    @SerialName("asset_to_update") val asset: AssetIdType,
    @SerialName("new_issuer") val newIssuer: Optional<AccountIdType> = optional(), // If the asset is to be given a new issuer, specify his ID here.
    @SerialName("new_options") val newOptions: AssetOptions,
    @SerialName("extensions") val extensions: Extensions,
) : Operation() {
    @Serializable
    data class Extensions(
        // After BSIP48, the precision of an asset can be updated if no supply is available
        // @note The parties involved still need to be careful
        @SerialName("new_precision") val newPrecision: Optional<UInt8> = optional(),
        // After BSIP48, if this option is set to true, the asset's core_exchange_rate won't be updated.
        // This is especially useful for committee-owned bitassets which can not be updated quickly.
        @SerialName("skip_core_exchange_rate") val skipCoreExchangeRate: Optional<Boolean> = optional(),
    ) : Extension<Extensions>
}

/* 12 */ 
@Serializable
data class AssetUpdateBitassetOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType,
    @SerialName("asset_to_update") val asset: AssetIdType,
    @SerialName("new_options") val newOptions: BitassetOptions,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 13 */ 
@Serializable
data class AssetUpdateFeedProducersOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType,
    @SerialName("asset_to_update") val asset: AssetIdType,
    @SerialName("new_feed_producers") val newFeedProducers: FlatSet<AccountIdType>,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 14 */ 
@Serializable
data class AssetIssueOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType, // Must be asset_to_issue->asset_id->issuer
    @SerialName("asset_to_issue") val asset: Asset,
    @SerialName("issue_to_account") val issueTo: AccountIdType,
    @SerialName("memo") val memo: Optional<MemoData> = optional(), // user provided data encrypted to the memo key of the "to" account
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 15 */ 
@Serializable
data class AssetReserveOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("payer") val account: AccountIdType,
    @SerialName("amount_to_reserve") val amount: Asset,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 16 */ 
@Serializable
data class AssetFundFeePoolOperation(
    @SerialName("fee") val fee: Asset, // core asset
    @SerialName("from_account") val account: AccountIdType,
    @SerialName("asset_id") val asset: AssetIdType,
    @SerialName("amount") val amount: ShareType, // core asset
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 17 */ 
@Serializable
data class AssetSettleOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("account") val account: AccountIdType, // Account requesting the force settlement. This account pays the fee
    @SerialName("amount") val amount: Asset, // Amount of asset to force settle. This must be a market-issued asset
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 18 */ 
@Serializable
data class AssetGlobalSettleOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType, // must equal issuer of @ref asset_to_settle
    @SerialName("asset_to_settle") val asset: AssetIdType,
    @SerialName("settle_price") val settlePrice: PriceType,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 19 */ 
@Serializable
data class AssetPublishFeedOperation(
    @SerialName("fee") val fee: Asset,// paid for by publisher
    @SerialName("publisher") val account: AccountIdType,
    @SerialName("asset_id") val asset: AssetIdType, // asset for which the feed is published
    @SerialName("feed") val feed: PriceFeed,
    @SerialName("extensions") val extensions: Extensions,
) : Operation() {
    @Serializable
    data class Extensions(
        // After BSIP77, price feed producers can feed ICR too
        @SerialName("initial_collateral_ratio") val initialCollateralRatio: Optional<UInt16> = optional()  // BSIP-77
    ): Extension<Extensions>
}

/* 20 */
@Serializable
data class WitnessCreateOperation(
    @SerialName("fee") val fee: Asset,
    // The account which owns the witness. This account pays the fee for this operation.
    @SerialName("witness_account") val account: AccountIdType,
    @SerialName("url") val url: String,
    @SerialName("block_signing_key") val blockSigningKey: PublicKeyType,
) : Operation()

/* 21 */ 
@Serializable
data class WitnessUpdateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("witness") val witness: WitnessIdType, // The witness object to update.
    @SerialName("witness_account") val account: AccountIdType, // The account which owns the witness. This account pays the fee for this operation.
    @SerialName("new_url") val newUrl: Optional<String> = optional(), // The new URL.
    @SerialName("new_signing_key") val newSigningKey: Optional<PublicKeyType> = optional(), // The new block signing key.
) : Operation()

/* 22 */ 
@Serializable
data class ProposalCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("fee_paying_account") val account: AccountIdType,
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("expiration_time") val expiration: Instant,
    @SerialName("proposed_ops") val proposedOperations: List<OperationWrapper>,
    @SerialName("review_period_seconds") val reviewPeriodSeconds: Optional<UInt32> = optional(),
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 23 */ 
@Serializable
data class ProposalUpdateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("fee_paying_account") val account: AccountIdType,
    @SerialName("proposal") val proposal: ProposalIdType,
    @SerialName("active_approvals_to_add") val activeApprovalsToAdd: FlatSet<AccountIdType>,
    @SerialName("active_approvals_to_remove") val activeApprovalsToRemove: FlatSet<AccountIdType>,
    @SerialName("owner_approvals_to_add") val ownerApprovalsToAdd: FlatSet<AccountIdType>,
    @SerialName("owner_approvals_to_remove") val ownerApprovalsToRemove: FlatSet<AccountIdType>,
    @SerialName("key_approvals_to_add") val keyApprovalsToAdd: FlatSet<PublicKeyType>,
    @SerialName("key_approvals_to_remove") val keyApprovalsToRemove: FlatSet<PublicKeyType>,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 24 */ 
@Serializable
data class ProposalDeleteOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("fee_paying_account") val account: AccountIdType,
    @SerialName("using_owner_authority") val usingOwnerAuthority: Boolean, // = false,
    @SerialName("proposal") val proposal: ProposalIdType,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 25 */ 
@Serializable
data class WithdrawPermissionCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("withdraw_from_account") val from: AccountIdType, // The account authorizing withdrawals from its balances
    @SerialName("authorized_account") val to: AccountIdType, // The account authorized to make withdrawals from withdraw_from_account
    @SerialName("withdrawal_limit") val limit: Asset, // The maximum amount authorized_account is allowed to withdraw in a given withdrawal period
    @SerialName("withdrawal_period_sec") val periodSeconds: UInt32, // = 0 // Length of the withdrawal period in seconds
    @SerialName("periods_until_expiration") val periodsUntilExpiration: UInt32, // = 0 // The number of withdrawal periods this permission is valid for
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("period_start_time") val periodStartTime: Instant, // Time at which the first withdrawal period begins; must be in the future
) : Operation()
/* 26 */ 
@Serializable
data class WithdrawPermissionUpdateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("withdraw_from_account") val from: AccountIdType, // This account pays the fee. Must match permission_to_update->withdraw_from_account
    @SerialName("authorized_account") val to: AccountIdType, // The account authorized to make withdrawals. Must match permission_to_update->authorized_account
    @SerialName("permission_to_update") val permission: WithdrawPermissionIdType, // ID of the permission which is being updated
    @SerialName("withdrawal_limit") val limit: Asset, // New maximum amount the withdrawer is allowed to charge per withdrawal period
    @SerialName("withdrawal_period_sec") val periodSeconds: UInt32, // = 0 // New length of the period between withdrawals
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("period_start_time") val periodStartTime: Instant, // New beginning of the next withdrawal period; must be in the future
    @SerialName("periods_until_expiration") val periodsUntilExpiration: UInt32, // = 0 // The new number of withdrawal periods for which this permission will be valid
) : Operation()

/* 27 */ 
@Serializable
data class WithdrawPermissionClaimOperation(
    @SerialName("fee") val fee: Asset, // Paid by withdraw_to_account
    @SerialName("withdraw_permission") val permission: WithdrawPermissionIdType, // ID of the permission authorizing this withdrawal
    @SerialName("withdraw_from_account") val from: AccountIdType, // Must match withdraw_permission->withdraw_from_account
    @SerialName("withdraw_to_account") val to: AccountIdType, // Must match withdraw_permision->authorized_account
    @SerialName("amount_to_withdraw") val amount: Asset, // Amount to withdraw. Must not exceed withdraw_permission->withdrawal_limit
    @SerialName("memo") val memo: Optional<MemoData> = optional(), // Memo for withdraw_from_account. Should generally be encrypted with withdraw_from_account->memo_key
) : Operation()

/* 28 */
@Serializable
data class WithdrawPermissionDeleteOperation(
    @SerialName("fee") val fee: Asset,
    // Must match withdrawal_permission->withdraw_from_account. This account pays the fee.
    @SerialName("withdraw_from_account") val from: AccountIdType,
    // The account previously authorized to make withdrawals. Must match withdrawal_permission->authorized_account
    @SerialName("authorized_account") val to: AccountIdType,
    // ID of the permission to be revoked.
    @SerialName("withdrawal_permission") val permission: WithdrawPermissionIdType,
) : Operation()

/* 29 */ 
@Serializable
data class CommitteeMemberCreateOperation(
    @SerialName("fee") val fee: Asset,
    // The account which owns the committee_member. This account pays the fee for this operation.
    @SerialName("committee_member_account") val account: AccountIdType,
    @SerialName("url") val url: String,
) : Operation()

/* 30 */ 
@Serializable
data class CommitteeMemberUpdateOperation(
    @SerialName("fee") val fee: Asset,
    // The committee member to update.
    @SerialName("committee_member") val committee: CommitteeMemberIdType,
    // The account which owns the committee_member. This account pays the fee for this operation.
    @SerialName("committee_member_account") val account: AccountIdType,
    @SerialName("new_url") val newUrl: Optional<String> = optional(),
) : Operation()
/* 31 */ 
@Serializable
data class CommitteeMemberUpdateGlobalParametersOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("new_parameters") val newParameters: ChainParameters,
) : Operation()

/* 32 */ 
@Serializable
data class VestingBalanceCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("creator") val creator: AccountIdType, // Who provides funds initially
    @SerialName("owner") val owner: AccountIdType, // Who is able to withdraw the balance
    @SerialName("amount") val amount: Asset,
    @SerialName("policy") val policy: VestingPolicyInitializer,
) : Operation()


/* 33 */ 
@Serializable
data class VestingBalanceWithdrawOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("vesting_balance") val vestingBalance: VestingBalanceIdType,
    val owner // Must be vesting_balance.owner
    : AccountIdType,
    @SerialName("amount") val amount: Asset,
) : Operation()
/* 34 */ 
@Serializable
data class WorkerCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("owner") val owner: AccountIdType,
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("work_begin_date") val begin: Instant,
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("work_end_date") val end: Instant,
    @SerialName("daily_pay") val dailyPay: ShareType,
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    // This should be set to the initializer appropriate for the type of worker to be created.
    @SerialName("initializer") val initializer: WorkerInitializer,
) : Operation()

/* 35 */ 
@Serializable
data class CustomOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("payer") val account: AccountIdType,
    @SerialName("required_auths") val requiredAuths: FlatSet<AccountIdType>,
    @SerialName("id") val id: UInt16, // = 0
    @SerialName("data") val data: BinaryData,
) : Operation()

/* 36 */ 
@Serializable
data class AssertOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("fee_paying_account") val account: AccountIdType,
    @SerialName("predicates") val predicates: List<Predicate>,
    @SerialName("required_auths") val requiredAuths: FlatSet<AccountIdType>,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 37 */ 
@Serializable
data class BalanceClaimOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("deposit_to_account") val account: AccountIdType,
    @SerialName("balance_to_claim") val balance: BalanceIdType,
    @SerialName("balance_owner_key") val balanceOwnerKey: PublicKeyType,
    @SerialName("total_claimed") val totalClaimed: Asset,
) : Operation()

/* 38 */ 
@Serializable
data class OverrideTransferOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType,
    @SerialName("from") val from: AccountIdType, // Account to transfer asset from
    @SerialName("to") val to: AccountIdType, // Account to transfer asset to
    @SerialName("amount") val amount: Asset, // The amount of asset to transfer from @ref from to @ref to
    @SerialName("memo") val memo: Optional<MemoData> = optional(), // User provided data encrypted to the memo key of the "to" account
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 39 */ 
@Serializable
data class TransferToBlindOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("amount") val amount: Asset,
    @SerialName("from") val from: AccountIdType,
    @SerialName("blinding_factor") val blindingFactor: BlindFactorType,
    @SerialName("outputs") val outputs: List<BlindOutput>,
) : Operation()
//354518f46abaaec4ef620127fc10637695e80000f29ad08e94f406221ed5d10f2a0000c5a6f9c9c38b0a9deaefe19afc293ade9213df03d7620b744b53227659829b35c57a0ded106ec8efc29b964cea4b01                                         0c363133383435393334306434 64dc90a248cd7380c956b5b02babaf4b0de3768bbded137c0e6307751705de30acc7cddf2c848456b1bf6e391d49893d2c8fec9791a515a4ff12eccfa18ce7a909313f25c9f4202f64e47d60414b1cba1b952c838c20bd6a8c2a374253e8117f90c1897003b07b9dd1018892878dfbec089f360103759519857ef67b31b0f08e76a9ab51c8849b3ab12ec747728316c52cb81500521cdd0001035386d212fdbf8f6e9c569fa7fac96d9301de285edc8cf8633dc14775593abd230103da20db3695d9e899560b6bf1e7b6dd9b61640fe0350a6513287b975c6c2f318364f71ac69730700589cb8e27e2b3cd4945372e7fdea4754c3aba46569523cd866e616c04bfdb32bcef9e859732675c5a6085c11f551082f28ed12ce414caad01afa0fa9de553f27b636791245212683b773add89df28d6a173eaa24a331b7196ef5a78f5ad0000
//354518f46abaaec4ef620127fc10637695e80000f29ad08e94f406221ed5d10f2a0000c5a6f9c9c38b0a9deaefe19afc293ade9213df03d7620b744b53227659829b35c57a0ded106ec8efc29b964cea4b01 6138459340d4000000000000000000000000000000000000000000000000000000 64dc90a248cd7380c956b5b02babaf4b0de3768bbded137c0e6307751705de30acc7cddf2c848456b1bf6e391d49893d2c8fec9791a515a4ff12eccfa18ce7a909313f25c9f4202f64e47d60414b1cba1b952c838c20bd6a8c2a374253e8117f90c1897003b07b9dd1018892878dfbec089f360103759519857ef67b31b0f08e76a9ab51c8849b3ab12ec747728316c52cb81500521cdd0001035386d212fdbf8f6e9c569fa7fac96d9301de285edc8cf8633dc14775593abd230103da20db3695d9e899560b6bf1e7b6dd9b61640fe0350a6513287b975c6c2f318364f71ac69730700589cb8e27e2b3cd4945372e7fdea4754c3aba46569523cd866e616c04bfdb32bcef9e859732675c5a6085c11f551082f28ed12ce414caad01afa0fa9de553f27b636791245212683b773add89df28d6a173eaa24a331b7196ef5a78f5ad0000
/* 40 */ 
@Serializable
data class BlindTransferOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("inputs") val inputs: List<BlindInput>,
    @SerialName("outputs") val outputs: List<BlindOutput>,
) : Operation()

/* 41 */ 
@Serializable
data class TransferFromBlindOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("amount") val amount: Asset,
    @SerialName("to") val to: AccountIdType,
    @SerialName("blinding_factor") val blindingFactor: BlindFactorType,
    @SerialName("inputs") val inputs: List<BlindInput>,
) : Operation()

/* 42 */ 
@Serializable
data class AssetSettleCancelOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("settlement") val settlement: ForceSettlementIdType,
    @SerialName("account") val account: AccountIdType, // Account requesting the force settlement. This account pays the fee
    @SerialName("amount") val amount: Asset, // Amount of asset to force settle. This must be a market-issued asset
) : Operation()  // Virtual

/* 43 */ 
@Serializable
data class AssetClaimFeesOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType, // must match issuer of asset from which we claim fees
    @SerialName("amount_to_claim") val amount: Asset,
    @SerialName("extensions") val extensions: Extensions,
) : Operation() {
    @Serializable
    data class Extensions(
        // Which asset to claim fees from. This is needed, e.g., to claim collateral-
        // denominated fees from a collateral-backed smart asset. If unset, assumed to be same
        // asset as amount_to_claim is denominated in, such as would be the case when claiming
        // market fees. If set, validation requires it to be a different asset_id than
        // amount_to_claim (else there would exist two ways to form the same request).
        @SerialName("claim_from_asset_id") val claimFromAssetId: Optional<AssetIdType> = optional()
    ) : Extension<Extensions>
}

/* 44 */ 
@Serializable
data class FbaDistributeOperation(
    @SerialName("fee") val fee: Asset, // always zero
    @SerialName("account_id") val account: AccountIdType,
    @SerialName("fba_id") val fba: ObjectIdType, // We use object_id_type because this is an implementaton object, and therefore is not known to the protocol library
    @SerialName("amount") val amount: ShareType,
) : Operation()       // Virtual

/* 45 */ 
@Serializable
data class BidCollateralOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("bidder") val account: AccountIdType, // pays fee and additional collateral
    @SerialName("additional_collateral") val additionalCollateral: Asset, // the amount of collateral to bid for the debt
    @SerialName("debt_covered") val debtCovered: Asset, // the amount of debt to take over
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 46 */ 
@Serializable
data class ExecuteBidOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("bidder") val account: AccountIdType,
    @SerialName("debt") val debt: Asset,
    @SerialName("collateral") val collateral: Asset,
) : Operation() // Virtual

/* 47 */ 
@Serializable
data class AssetClaimPoolOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType,
    @SerialName("asset_id") val asset: AssetIdType, // fee.asset_id must != asset_id
    @SerialName("amount_to_claim") val amount: Asset, // core asset
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 48 */ 
@Serializable
data class AssetUpdateIssuerOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("issuer") val account: AccountIdType,
    @SerialName("asset_to_update") val asset: AssetIdType,
    @SerialName("new_issuer") val newIssuer: AccountIdType,
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 49 */ 
@Serializable
data class HtlcCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("from") val from: AccountIdType, // where the held monies are to come from
    @SerialName("to") val to: AccountIdType, // where the held monies will go if the preimage is provided
    @SerialName("amount") val amount: Asset, // the amount to hold
    @SerialName("preimage_hash") val preimageHash: HtlcHash, // the (typed) hash of the preimage
    @SerialName("preimage_size") val preimageSize: UInt16, // the size of the preimage
    @SerialName("claim_period_seconds") val claimPeriodSeconds: UInt32, // The time the funds will be returned to the source if not claimed
    @SerialName("extensions") val extensions: Extensions, // additional extensions
) : Operation() {
    @Serializable
    data class Extensions(
        @SerialName("memo") val memo: Optional<MemoData> = optional(),
    ) : Extension<Extensions>
}

/* 50 */ 
@Serializable
data class HtlcRedeemOperation(
    @SerialName("fee") val fee: Asset, // paid to network
    @SerialName("htlc_id") val htlc: HtlcIdType, // the object we are attempting to update
    @SerialName("redeemer") val redeemer: AccountIdType, // who is attempting to update the transaction
    @SerialName("preimage") val preimage: BinaryData, // the preimage (not used if after epoch timeout)
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // for future expansion
) : Operation()

/* 51 */ 
@Serializable
data class HtlcRedeemedOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("htlc_id") val htlc: HtlcIdType,
    @SerialName("from") val from: AccountIdType,
    @SerialName("to") val to: AccountIdType,
    @SerialName("redeemer") val redeemer: AccountIdType,
    @SerialName("amount") val amount: Asset,
    @SerialName("htlc_preimage_hash") val preimageHash: HtlcHash,
    @SerialName("htlc_preimage_size") val preimageSize: UInt16,
    @SerialName("preimage") val preimage: BinaryData,
) : Operation()         // Virtual

/* 52 */ 
@Serializable
data class HtlcExtendOperation(
    @SerialName("fee") val fee: Asset, // paid to network
    @SerialName("htlc_id") val htlcId: HtlcIdType, // the object we are attempting to update
    @SerialName("update_issuer") val updateIssuer: AccountIdType, // who is attempting to update the transaction
    @SerialName("seconds_to_add") val secondsToAdd: UInt32, // how much to add
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // for future expansion
) : Operation()

// TODO: 2022/4/5
/* 53 */ 
@Serializable
data class HtlcRefundOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("htlc_id") val htlc: HtlcIdType, // of the associated htlc object; it is deleted during emittance of this operation
    @SerialName("to") val to: AccountIdType,
    @SerialName("original_htlc_recipient") val originalRecipient: AccountIdType,
    @SerialName("htlc_amount") val amount: Asset,
    @SerialName("htlc_preimage_hash") val preimageHash: HtlcHash,
    @SerialName("htlc_preimage_size") val preimageSize: UInt16,
) : Operation()           // Virtual

/* 54 */ 
@Serializable
data class CustomAuthorityCreateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("account") val account: AccountIdType, // Account which is setting the custom authority; also pays the fee
    @SerialName("enabled") val enabled: Boolean, // Whether the custom authority is enabled or not
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("valid_from") val validFrom: Instant, // Date when custom authority becomes active
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("valid_to") val validTo: Instant, // Expiration date for custom authority
    @SerialName("operation_type") val operationType: UnsignedInt, // Tag of the operation this custom authority can authorize
    @SerialName("auth") val auth: Authority, // Authentication requirements for the custom authority
    @SerialName("restrictions") val restrictions: List<Restriction>, // Restrictions on operations this custom authority can authenticate
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 55 */ 
@Serializable
data class CustomAuthorityUpdateOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("account") val account: AccountIdType, // Account which owns the custom authority to update; also pays the fee
    @SerialName("authority_to_update") val authorityToUpdate: CustomAuthorityIdType, // ID of the custom authority to update
    @SerialName("new_enabled") val newEnabled: Optional<Boolean> = optional(), // Change to whether the custom authority is enabled or not
    @SerialName("new_valid_from") val newValidFrom: Optional<@Serializable(with = TimePointSecSerializer::class) Instant> = optional(), // Change to the custom authority begin date
    @SerialName("new_valid_to") val newValidTo: Optional<@Serializable(with = TimePointSecSerializer::class) Instant> = optional(), // Change to the custom authority expiration date
    @SerialName("new_auth") val newAuth: Optional<Authority> = optional(), // Change to the authentication for the custom authority
    @SerialName("restrictions_to_remove") val restrictionsToRemove: FlatSet<UInt16>, // Set of IDs of restrictions to remove
    @SerialName("restrictions_to_add") val restrictionsToAdd: List<Restriction>, // Vector of new restrictions
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()
/* 56 */ 
@Serializable
data class CustomAuthorityDeleteOperation(
    @SerialName("fee") val fee: Asset,
    @SerialName("account") val account: AccountIdType, // Account which owns the custom authority to update; also pays the fee
    @SerialName("authority_to_delete") val authorityToDelete: CustomAuthorityIdType, // ID of the custom authority to delete
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),
) : Operation()

/* 57 */ 
@Serializable
data class TicketCreateOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who creates the ticket
    @SerialName("target_type") val targetType: UnsignedInt, // The target ticket type, see @ref ticket_type
    @SerialName("amount") val amount: Asset, // The amount of the ticket
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 58 */ 
@Serializable
data class TicketUpdateOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("ticket") val ticket: TicketIdType, // The ticket to update
    @SerialName("account") val account: AccountIdType, // The account who owns the ticket
    @SerialName("target_type") val targetType: UnsignedInt, // New target ticket type, see @ref ticket_type
    @SerialName("amount_for_new_target") val amountForNewTarget: Optional<Asset> = optional(), // The amount to be used for the new target
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 59 */ 
@Serializable
data class LiquidityPoolCreateOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who creates the liquidity pool
    @SerialName("asset_a") val assetA: AssetIdType, // Type of the first asset in the pool
    @SerialName("asset_b") val assetB: AssetIdType, // Type of the second asset in the pool
    @SerialName("share_asset") val shareAsset: AssetIdType, // Type of the share asset aka the LP token
    @SerialName("taker_fee_percent") val takerFeePercent: UInt16, // = 0 // Taker fee percent
    @SerialName("withdrawal_fee_percent") val withdrawalFeePercent: UInt16, // = 0 // Withdrawal fee percent
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 60 */ 
@Serializable
data class LiquidityPoolDeleteOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who owns the liquidity pool
    @SerialName("pool") val pool: LiquidityPoolIdType, // ID of the liquidity pool
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()


/* 61 */ 
@Serializable
data class LiquidityPoolDepositOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who deposits to the liquidity pool
    @SerialName("pool") val pool: LiquidityPoolIdType, // ID of the liquidity pool
    @SerialName("amount_a") val amountA: Asset, // The amount of the first asset to deposit
    @SerialName("amount_b") val amountB: Asset, // The amount of the second asset to deposit
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.

) : Operation()
/* 62 */ 
@Serializable
data class LiquidityPoolWithdrawOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who withdraws from the liquidity pool
    @SerialName("pool") val pool: LiquidityPoolIdType, // ID of the liquidity pool
    @SerialName("share_amount") val shareAmount: Asset, // The amount of the share asset to use
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 63 */ 
@Serializable
data class LiquidityPoolExchangeOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who exchanges with the liquidity pool
    @SerialName("pool") val pool: LiquidityPoolIdType, // ID of the liquidity pool
    @SerialName("amount_to_sell") val amountToSell: Asset, // The amount of one asset type to sell
    @SerialName("min_to_receive") val minToReceive: Asset, // The minimum amount of the other asset type to receive
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 64 */ 
@Serializable
data class SametFundCreateOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("owner_account") val account: AccountIdType, // Owner of the fund
    @SerialName("asset_type") val asset: AssetIdType, // Asset type in the fund
    @SerialName("balance") val balance: ShareType,// Usable amount in the fund
    @SerialName("fee_rate") val feeRate: UInt32, // = 0 // Fee rate, the demominator is GRAPHENE_FEE_RATE_DENOM
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 65 */ 
@Serializable
data class SametFundDeleteOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("owner_account") val account: AccountIdType, // The account who owns the SameT Fund object
    @SerialName("fund_id") val fund: SametFundIdType, // ID of the SameT Fund object
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 66 */ 
@Serializable
data class SametFundUpdateOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("owner_account") val account: AccountIdType, // Owner of the fund
    @SerialName("fund_id") val fund: SametFundIdType, // ID of the SameT Fund object
    @SerialName("delta_amount") val deltaAmount: Optional<Asset> = optional(), // Delta amount, optional
    @SerialName("new_fee_rate") val newFeeRate: Optional<UInt32> = optional(), // New fee rate, optional
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 67 */ 
@Serializable
data class SametFundBorrowOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("borrower") val account: AccountIdType, // The account who borrows from the fund
    @SerialName("fund_id") val fund: SametFundIdType, // ID of the SameT Fund
    @SerialName("borrow_amount") val amount: Asset, // The amount to borrow
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 68 */ 
@Serializable
data class SametFundRepayOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who repays to the SameT Fund
    @SerialName("fund_id") val fund: SametFundIdType, // ID of the SameT Fund
    @SerialName("repay_amount") val amount: Asset, // The amount to repay
    @SerialName("fund_fee") val fundFee: Asset, // Fee for using the fund
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.

) : Operation()


/* 69 */ 
@Serializable
data class CreditOfferCreateOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("owner_account") val account: AccountIdType, // Owner of the credit offer
    @SerialName("asset_type") val asset: AssetIdType, // Asset type in the credit offer
    @SerialName("balance") val balance: ShareType, // Usable amount in the credit offer
    @SerialName("fee_rate") val feeRate: UInt32, // = 0 // Fee rate, the demominator is GRAPHENE_FEE_RATE_DENOM
    @SerialName("max_duration_seconds") val maxDurationSeconds: UInt32, // = 0 // The time limit that borrowed funds should be repaid
    @SerialName("min_deal_amount") val minDealAmount: ShareType, // Minimum amount to borrow for each new deal
    @SerialName("enabled") val enabled: Boolean, // = false // Whether this offer is available
    @Serializable(with = TimePointSecSerializer::class)
    @SerialName("auto_disable_time") val autoDisableTime: Instant, // The time when this offer will be disabled automatically
    @SerialName("acceptable_collateral") val acceptableCollateral: FlatMap<AssetIdType, PriceType>, // Types and rates of acceptable collateral
    @SerialName("acceptable_borrowers") val acceptableBorrowers: FlatMap<AccountIdType, ShareType>, // Allowed borrowers and their maximum amounts to borrow. No limitation if empty.
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 70 */ 
@Serializable
data class CreditOfferDeleteOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("owner_account") val account: AccountIdType, // The account who owns the credit offer
    @SerialName("offer_id") val offer: CreditOfferIdType, // ID of the credit offer
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(),// Unused. Reserved for future use.
) : Operation()

/* 71 */ 
@Serializable
data class CreditOfferUpdateOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("owner_account") val account: AccountIdType, // Owner of the credit offer
    @SerialName("offer_id") val offer: CreditOfferIdType, // ID of the credit offer
    @SerialName("delta_amount") val deltaAmount: Optional<Asset> = optional(), // Delta amount, optional
    @SerialName("fee_rate") val feeRate: Optional<UInt32> = optional(), // New fee rate, optional
    @SerialName("max_duration_seconds") val maxDurationSeconds: Optional<UInt32> = optional(), // New repayment time limit, optional
    @SerialName("min_deal_amount") val minDealAmount: Optional<ShareType> = optional(), // Minimum amount to borrow for each new deal, optional
    @SerialName("enabled") val enabled: Optional<Boolean> = optional(), // Whether this offer is available, optional
    @SerialName("auto_disable_time") val autoDisableTime: Optional<@Serializable(with = TimePointSecSerializer::class) Instant>, // New time to disable automatically, optional
    @SerialName("acceptable_collateral") val acceptableCollateral: Optional<FlatMap<AssetIdType, PriceType>> = optional(), // New types and rates of acceptable collateral, optional
    @SerialName("acceptable_borrowers") val acceptableBorrowers: Optional<FlatMap<AccountIdType, ShareType>> = optional(), // New allowed borrowers and their maximum amounts to borrow, optional
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 72 */ 
@Serializable
data class CreditOfferAcceptOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("borrower") val account: AccountIdType, // The account who accepts the offer
    @SerialName("offer_id") val offer: CreditOfferIdType, // ID of the credit offer
    @SerialName("borrow_amount") val amount: Asset, // The amount to borrow
    @SerialName("collateral") val collateral: Asset, // The collateral
    @SerialName("max_fee_rate") val maxFeeRate: UInt32, // = 0 // The maximum acceptable fee rate
    @SerialName("min_duration_seconds") val minDurationSeconds: UInt32, // = 0 // The minimum acceptable duration
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 73 */ 
@Serializable
data class CreditDealRepayOperation(
    @SerialName("fee") val fee: Asset, // Operation fee
    @SerialName("account") val account: AccountIdType, // The account who repays to the credit offer
    @SerialName("deal_id") val deal: CreditDealIdType, // ID of the credit deal
    @SerialName("repay_amount") val amount: Asset, // The amount to repay
    @SerialName("credit_fee") val creditFee: Asset, // The credit fee relative to the amount to repay
    @SerialName("extensions") val extensions: FutureExtensions = emptyExtension(), // Unused. Reserved for future use.
) : Operation()

/* 74 */ 
@Serializable
data class CreditDealExpiredOperation(
    @SerialName("fee") val fee: Asset, // Only for compatibility, unused
    @SerialName("deal_id") val deal: CreditDealIdType, // ID of the credit deal
    @SerialName("offer_id") val offer: CreditOfferIdType, // ID of the credit offer
    @SerialName("offer_owner") val owner: AccountIdType, // Owner of the credit offer
    @SerialName("borrower") val borrower: AccountIdType, // The account who repays to the credit offer
    @SerialName("unpaid_amount") val unpaidAmount: Asset, // The amount that is unpaid
    @SerialName("collateral") val collateral: Asset, // The collateral liquidated
    @SerialName("fee_rate") val feeRate: UInt32, // = 0 // Fee rate, the demominator is GRAPHENE_FEE_RATE_DENOM
) : Operation()    // Virtual