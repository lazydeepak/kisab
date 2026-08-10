package com.susankhya.kisab.domain

/**
 * The role a party plays in the farm's business. The role is the stable anchor
 * for future hisab/khata records: a CUSTOMER's future hisab holds money the
 * farm is owed, a SUPPLIER's holds money the farm owes. M5-01 stores the role
 * only; no balances are derived from it yet.
 */
enum class PartyRole {
    CUSTOMER,
    SUPPLIER,
    OTHER
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
