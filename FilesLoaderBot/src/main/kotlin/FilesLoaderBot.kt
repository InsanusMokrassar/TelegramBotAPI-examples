import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMedia
import dev.inmo.tgbotapi.utils.filenameFromUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * This bot will download incoming files
 */
suspend fun main(args: Array<String>) {
    val botToken = args.first()
    val directoryOrFile = args.getOrNull(1) ?.let { File(it) } ?: File("/tmp/")
    directoryOrFile.mkdirs()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        onMedia(initialFilter = null) {
            val pathedFile = bot.getFileAdditionalInfo(it.content.media)
            val outFile = File(directoryOrFile, pathedFile.filePath.filenameFromUrl)
            runCatching {
                bot.downloadFile(it.content.media, outFile)
            }.onFailure {
                it.printStackTrace()
            }
            reply(it, "Saved to ${outFile.absolutePath}")
        }
        onContentMessage { println(it) }
    }.second.join()
}
