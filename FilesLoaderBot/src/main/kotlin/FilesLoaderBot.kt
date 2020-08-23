import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.downloadFile
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.get.getFileAdditionalInfo
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.telegramBot
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.flatMap
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.safely
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.shortcuts.*
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.abstracts.MediaContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.filenameFromUrl
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
