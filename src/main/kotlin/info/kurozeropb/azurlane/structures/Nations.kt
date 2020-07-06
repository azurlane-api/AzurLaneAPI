package info.kurozeropb.azurlane.structures

data class Nation(
    val name: String? = null,
    val prefix: String? = null,
    val icon: String? = null
)

typealias Nations = List<Nation>

data class NationsResponse(
    override val statusCode: Int,
    override val statusMessage: String,
    override val message: String,
    val nations: Nations
) : IBaseResponse