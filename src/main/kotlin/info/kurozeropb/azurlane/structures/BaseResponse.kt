package info.kurozeropb.azurlane.structures

interface IBaseResponse {
    val statusCode: Int
    val statusMessage: String
    val message: String
}

data class BaseResponse(
    override val statusCode: Int,
    override val statusMessage: String,
    override val message: String
) : IBaseResponse

data class IndexResponse(
    override val statusCode: Int,
    override val statusMessage: String,
    override val message: String,
    val routes: List<String>
) : IBaseResponse
