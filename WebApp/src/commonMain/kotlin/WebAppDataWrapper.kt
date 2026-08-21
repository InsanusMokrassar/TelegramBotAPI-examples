import kotlinx.serialization.Serializable

/**
 * JSON body used by server routes that validate Telegram Web App initialization.
 *
 * @property data raw `initData` received by the browser Web App.
 * @property hash hash exposed by the parsed, unsafe initialization data.
 */
@Serializable
data class WebAppDataWrapper(
    val data: String,
    val hash: String
)
