package info.kurozeropb.azurlane.structures

data class Equipment(
    val name: String? = null,
    val icon: String? = null,
    val rarity: String? = null
)

typealias Equipments = List<Equipment>

data class EquipmentResponse(
    override val statusCode: Int,
    override val statusMessage: String,
    override val message: String,
    val equipments: Equipments
) : IBaseResponse

data class SmallEquipment(
    val name: String? = null,
    val url: String? = null
)

typealias SmallEquipments = List<SmallEquipment>

data class EquipmentsResponse(
    override val statusCode: Int,
    override val statusMessage: String,
    override val message: String,
    val equipments: SmallEquipments
) : IBaseResponse