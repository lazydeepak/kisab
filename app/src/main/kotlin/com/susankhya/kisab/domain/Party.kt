package com.susankhya.kisab.domain

/**
 * The role a party plays in the farm's business. The role is the stable anchor
 * for future hisab/khata records: a CUSTOMER's future hisab holds money the
 * farm is owed, a SUPPLIER's holds money the farm owes, and a BOTH party trades
 * on either side of the farm's business. M5-01 introduced CUSTOMER/SUPPLIER/OTHER;
 * M5-02 adds BOTH so a single party need not be duplicated when it both buys
 * from and sells to the farm. Serialized role names are stable (schema-v4 party
 * data keeps decoding).
 */
enum class PartyRole {
    CUSTOMER,
    SUPPLIER,
    BOTH,
    OTHER
}

/**
 * Whether a party can be linked to a trade of the given type.
 *
 * SALE links CUSTOMER or BOTH parties (they buy the farm's produce); PURCHASE
 * links SUPPLIER or BOTH parties (they sell inputs to the farm). OTHER parties
 * are never trade counterparts. The M5-02 decision is to enforce compatibility
 * at the service boundary rather than silently convert a party's role: the
 * party editor exposes the role directly, and incompatible changes are blocked
 * while trades still reference the party.
 */
fun PartyRole.compatibleWith(type: TradeType): Boolean = when (type) {
    TradeType.SALE -> this == PartyRole.CUSTOMER || this == PartyRole.BOTH
    TradeType.PURCHASE -> this == PartyRole.SUPPLIER || this == PartyRole.BOTH
}

/**
 * A business counterpart of the farm: a buyer of produce, a supplier of inputs,
 * or another party the farmer tracks. M5-01 carries identity, role and free-text
 * contact/notes only; hisab/khata and Udhar records arrive in M5-02+.
 */
data class Party(
    val id: String,
    val name: String,
    val role: PartyRole,
    val contact: String = "",
    val notes: String = ""
)

/**
 * Input for creating or updating a [Party]. Contact and notes default to empty;
 * the name must be non-blank and the role is always explicit.
 */
data class PartyDraft(
    val name: String,
    val role: PartyRole,
    val contact: String = "",
    val notes: String = ""
)
