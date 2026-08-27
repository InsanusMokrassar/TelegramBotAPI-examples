import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.chat.modify.setChatPhoto
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPhoto
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Starts a long-polling bot that uses each incoming photo as the avatar of the chat where it was sent.
 *
 * The bot downloads the photo and uploads it with `setChatPhoto`. It replies `Done` when the avatar is updated;
 * if that API call fails, it logs the exception and replies with an error message. The target must be a
 * non-private chat where the bot is an administrator allowed to change chat information.
 *
 * @param args command-line arguments whose first value is the Bot API token
 */
suspend fun main(args: Array<String>) {
    val bot = telegramBot(args.first())

    bot.buildBehaviourWithLongPolling(scope = CoroutineScope(Dispatchers.IO)) {
        onPhoto {
            val bytes = downloadFile(it.content)
            runCatchingLogging {
                setChatPhoto(
                    it.chat.id,
                    bytes.asMultipartFile("sample.jpg")
                )
            }.onSuccess { _ ->
                reply(it, "Done")
            }.onFailure { e ->
                e.printStackTrace()

                reply(it, "Something went wrong (see logs)")
            }
        }
    }.join()
}
