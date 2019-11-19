package info.kurozeropb.azurlane.structures

data class Patron(
    val id: String,
    val token: String,
    val amount: Int
)

data class Attributes(
    val currently_entitled_amount_cents: Int? = null,
    val full_name: String,
    val is_follower: Boolean,
    val last_charge_date: String? = null,
    val last_charge_status: String? = null,
    val lifetime_support_cents: Int,
    val note: String,
    val patron_status: String? = null,
    val pledge_relationship_start: String? = null
)

data class Data(
    val id: String,
    val type: String
)

data class Address(
    val data: Data? = null
)

data class Campaign(
    val data: Data? = null
)

data class CurrentlyEntitledTiers(
    val data: List<Any>
)

data class User(
    val data: Data? = null
)

data class Relationships(
    val address: Address,
    val campaign: Campaign,
    val currently_entitled_tiers: CurrentlyEntitledTiers,
    val user: User
)

data class PatreonData(
    val attributes: Attributes,
    val id: String,
    val relationships: Relationships,
    val type: String
)

data class PatreonBody(
    val data: PatreonData
)