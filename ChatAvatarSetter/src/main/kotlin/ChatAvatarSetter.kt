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

suspend fun main(args: Array<String>) {
    val bot = telegramBot(args.first())

    bot.buildBehaviourWithLongPolling(scope = CoroutineScope(Dispatchers.IO)) {
        onPhoto {
            val bytes = downloadFile(it.content)
            runCatchingSafely {
                setChatPhoto(
                    it.chat.id,
                    bytes.asMultipartFile("sample.jpg")
                )
            }.onSuccess { b ->
                if (b) {
                    reply(it, "Done")
                } else {
                    reply(it, "Something went wrong")
                }
            }.onFailure { e ->
                e.printStackTrace()

                reply(it, "Something went wrong (see logs)")
            }
        }
    }.join()
}
