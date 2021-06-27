import dev.inmo.tgbotapi.extensions.api.downloadFile
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviour
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
    val directoryOrFile = args.getOrNull(1) ?.let { File(it) } ?: File("")
    directoryOrFile.mkdirs()

    telegramBotWithBehaviour(botToken, CoroutineScope(Dispatchers.IO)) {
        onMedia(includeMediaGroups = true) {
            val pathedFile = bot.getFileAdditionalInfo(it.content.media)
            val file = File(directoryOrFile, pathedFile.filePath.filenameFromUrl).apply {
                createNewFile()
                writeBytes(bot.downloadFile(pathedFile))
            }
            reply(it, "Saved to ${file.absolutePath}")
        }
        onContentMessage { println(it) }
    }.second.join()
}
