import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.common.filesize
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocumentsGroup
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.files.DocumentFile
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.mediaCountInMediaGroup
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath

private const val command = "send_file"

/**
 * This bot will send files inside of working directory OR from directory in the second argument.
 * You may send /send_file command to this bot to get random file from the directory OR
 * `/send_file $number` when you want to receive required number of files. For example,
 * /send_file and `/send_file 1` will have the same effect - bot will send one random file.
 * But if you will send `/send_file 5` it will choose 5 random files and send them as group
 */
fun main(args: Array<String>) = runBlocking {
    val botToken = args.first()
    val directoryOrFile = args.getOrNull(1) ?.toPath() ?: "".toPath()

    fun pickFile(currentRoot: MPPFile = directoryOrFile): MPPFile? {
        if (FileSystem.SYSTEM.exists(currentRoot) && FileSystem.SYSTEM.listOrNull(currentRoot) == null) {
            return currentRoot
        } else {
            return pickFile(FileSystem.SYSTEM.list(currentRoot).takeIf { it.isNotEmpty() } ?.random() ?: return null)
        }
    }

    suspend fun TelegramBot.sendFiles(chat: Chat, files: List<MPPFile>) {
        when (files.size) {
            1 -> {
                sendDocument(
                    chat.id,
                    files.first().asMultipartFile(),
                    protectContent = true
                )
            }
            else -> sendDocumentsGroup(
                chat,
                files.map { TelegramMediaDocument(it.asMultipartFile()) },
                protectContent = true
            )
        }
    }

    val bot = telegramBot(botToken)

    bot.buildBehaviourWithLongPolling (defaultExceptionsHandler = { it.printStackTrace() }) {
        onCommandWithArgs(command) { message, args ->
            withUploadDocumentAction(message.chat) {
                val count = args.firstOrNull() ?.toIntOrNull() ?: 1
                var sent = false

                var left = count
                val chosen = mutableListOf<MPPFile>()

                while (left > 0) {
                    val picked = pickFile() ?.takeIf { it.filesize > 0 } ?: continue
                    chosen.add(picked)
                    left--
                    if (chosen.size >= mediaCountInMediaGroup.last) {
                        runCatchingSafely {
                            sendFiles(message.chat, chosen)
                        }.onFailure {
                            it.printStackTrace()
                        }
                        chosen.clear()
                        sent = true
                    }
                }

                if (chosen.isNotEmpty()) {
                    sendFiles(message.chat, chosen)
                    sent = true
                }

                if (!sent) {
                    reply(message, "Nothing selected :(")
                }
            }
        }

        setMyCommands(
            BotCommand(command, "Send some random file in picker directory")
        )

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }

        println(getMe())
    }.join()
}
