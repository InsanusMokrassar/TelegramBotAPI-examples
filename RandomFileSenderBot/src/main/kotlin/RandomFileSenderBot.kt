import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.bot.getMe
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.bot.setMyCommands
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.media.sendDocument
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.sendTextMessage
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.telegramBot
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.safely
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.filterExactCommands
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import com.github.insanusmokrassar.TelegramBotAPI.requests.abstracts.toInputFile
import com.github.insanusmokrassar.TelegramBotAPI.types.BotCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File

private const val command = "send_file"

/**
 * This bot will send files inside of working directory OR from directory in the second argument
 */
suspend fun main(args: Array<String>) {
    val botToken = args.first()
    val directoryOrFile = args.getOrNull(1) ?.let { File(it) } ?: File("")

    fun pickFile(currentRoot: File = directoryOrFile): File? {
        if (currentRoot.isFile) {
            return currentRoot
        } else {
            return pickFile(currentRoot.listFiles() ?.random() ?: return null)
        }
    }

    val bot = telegramBot(botToken)
    val scope = CoroutineScope(Dispatchers.Default)

    bot.startGettingFlowsUpdatesByLongPolling(scope = scope) {
        messageFlow.filterExactCommands(Regex(command)).onEach { message ->
            safely {
                pickFile() ?.let {
                    bot.sendDocument(
                        message.chat.id,
                        it.toInputFile()
                    )
                } ?: bot.sendTextMessage(message.chat.id, "Nothing selected :(")
            }
        }.launchIn(scope)
    }

    safely {
        bot.setMyCommands(
            BotCommand(command, "Send some random file in picker directory")
        )
        println(bot.getMe())
    }

    scope.coroutineContext[Job]!!.join()
}
