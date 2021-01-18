import dev.inmo.micro_utils.coroutines.safely
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.downloadFile
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.utils.flatMap
import dev.inmo.tgbotapi.extensions.utils.shortcuts.*
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import dev.inmo.tgbotapi.types.message.content.abstracts.MediaContent
import dev.inmo.tgbotapi.utils.filenameFromUrl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * This bot will download incoming files
 */
suspend fun main(args: Array<String>) {
    val botToken = args.first()
    val directoryOrFile = args.getOrNull(1) ?.let { File(it) } ?: File("")
    directoryOrFile.mkdirs()

    val bot = telegramBot(botToken)
    val scope = CoroutineScope(Dispatchers.Default)

    bot.startGettingFlowsUpdatesByLongPolling(scope = scope) {
        val flow = merge (
            filterContentMessages<MediaContent>(),
            mediaGroupMessages().flatMap()
        )
        flow.onEach {
            safely({ it.printStackTrace() }) {
                val pathedFile = bot.getFileAdditionalInfo(it.content.media)
                File(directoryOrFile, pathedFile.filePath.filenameFromUrl).apply {
                    createNewFile()
                    writeBytes(bot.downloadFile(pathedFile))
                }
            }
        }.launchIn(scope)
    }

    scope.coroutineContext[Job]!!.join()
}
