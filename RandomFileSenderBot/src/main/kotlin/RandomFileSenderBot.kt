import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.safely
import dev.inmo.tgbotapi.extensions.utils.updates.filterExactCommands
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import dev.inmo.tgbotapi.requests.abstracts.toInputFile
import dev.inmo.tgbotapi.types.BotCommand
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
