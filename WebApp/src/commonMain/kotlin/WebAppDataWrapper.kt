import kotlinx.serialization.Serializable

@Serializable
data class WebAppDataWrapper(
    val data: String,
    val hash: String
)
