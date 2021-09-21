import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocumentsGroup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.withUploadDocumentAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.requests.abstracts.toInputFile
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.InputMedia.DocumentMediaGroupMemberInputMedia
import dev.inmo.tgbotapi.types.InputMedia.InputMediaDocument
import dev.inmo.tgbotapi.types.chat.abstracts.Chat
import dev.inmo.tgbotapi.types.mediaCountInMediaGroup
import kotlinx.coroutines.*
import java.io.File

private const val command = "send_file"

/**
 * This bot will send files inside of working directory OR from directory in the second argument.
 * You may send /send_file command to this bot to get random file from the directory OR
 * `/send_file $number` when you want to receive required number of files. For example,
 * /send_file and `/send_file 1` will have the same effect - bot will send one random file.
 * But if you will send `/send_file 5` it will choose 5 random files and send them as group
 */
suspend fun main(args: Array<String>) {
    val botToken = args.first()
    val directoryOrFile = args.getOrNull(1) ?.let { File(it) } ?: File("")

    fun pickFile(currentRoot: File = directoryOrFile): File? {
        if (currentRoot.isFile) {
            return currentRoot
        } else {
            return pickFile(currentRoot.listFiles() ?.takeIf { it.isNotEmpty() } ?.random() ?: return null)
        }
    }

    suspend fun TelegramBot.sendFiles(chat: Chat, files: List<File>) {
        when (files.size) {
            1 -> sendDocument(
                chat.id,
                files.first().asMultipartFile()
            )
            else -> sendDocumentsGroup(
                chat,
                files.map { InputMediaDocument(it.asMultipartFile()) }
            )
        }
    }

    val bot = telegramBot(botToken)

    bot.buildBehaviour(defaultExceptionsHandler = { it.printStackTrace() }) {
        onCommandWithArgs(command) { message, args ->

            withUploadDocumentAction(message.chat) {
                val count = args.firstOrNull() ?.toIntOrNull() ?: 1
                var sent = false

                var left = count
                val chosen = mutableListOf<File>()

                while (left > 0) {
                    left--
                    val picked = pickFile() ?: continue
                    chosen.add(picked)
                    if (chosen.size >= mediaCountInMediaGroup.last) {
                        sendFiles(message.chat, chosen)
                        chosen.clear()
                        sent = true
                    }
                }

                if (chosen.isNotEmpty()) {
                    sendFiles(message.chat, chosen)
                    sent = true
                }

                if (!sent) {
                    bot.reply(message, "Nothing selected :(")
                }
            }
        }

        setMyCommands(
            BotCommand(command, "Send some random file in picker directory")
        )

        println(getMe())
    }.join()
}
