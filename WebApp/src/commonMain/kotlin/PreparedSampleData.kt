import dev.inmo.tgbotapi.types.buttons.PreparedKeyboardButtonId
import dev.inmo.tgbotapi.types.request.RequestId
import kotlin.random.Random
import kotlin.random.nextUInt

/** Process-local request ID used when the bot saves its sample managed-bot button. */
val preparedSampleKeyboardRequestId = RequestId(Random.nextUInt().toUShort())
