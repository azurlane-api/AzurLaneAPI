package info.kurozeropb.azurlane.structures

data class Patron(
    val id: String,
    val email: String,
    val token: String,
    val enabled: Boolean
)

data class UserAttributes(
    val email: String,
    val full_name: String
)

data class User(
    val attributes: UserAttributes,
    val id: String
)

data class Attributes(
    val currently_entitled_amount_cents: Int? = null,
    val full_name: String,
    val is_follower: Boolean,
    val last_charge_date: String? = null,
    val last_charge_status: String? = null,
    val lifetime_support_cents: Int,
    val note: String? = null,
    val patron_status: String? = null,
    val pledge_relationship_start: String? = null
)

data class Data(
    val attributes: Attributes
)

data class Patreon(
    val data: Data,
    val included: List<Any>
)