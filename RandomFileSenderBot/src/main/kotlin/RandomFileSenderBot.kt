import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.requests.abstracts.toInputFile
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.coroutines.*
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

    bot.buildBehaviour(scope) {
        onCommand(command.toRegex()) { message ->
            pickFile() ?.let {
                bot.sendDocument(
                    message.chat.id,
                    it.toInputFile()
                )
            } ?: bot.reply(message, "Nothing selected :(")
        }
        setMyCommands(
            BotCommand(command, "Send some random file in picker directory")
        )
        println(getMe())
    }.join()
}
