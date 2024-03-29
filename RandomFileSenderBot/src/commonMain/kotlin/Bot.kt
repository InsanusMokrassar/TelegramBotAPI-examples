import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.common.filesize
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocumentsGroup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.withUploadDocumentAction
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.mediaCountInMediaGroup

private const val command = "send_file"

expect fun pickFile(currentRoot: MPPFile): MPPFile?

/**
 * This bot will send files inside of working directory OR from directory in the second argument.
 * You may send /send_file command to this bot to get random file from the directory OR
 * `/send_file $number` when you want to receive required number of files. For example,
 * /send_file and `/send_file 1` will have the same effect - bot will send one random file.
 * But if you will send `/send_file 5` it will choose 5 random files and send them as group
 */
suspend fun doRandomFileSenderBot(token: String, folder: MPPFile) {
    val bot = telegramBot(token)

    suspend fun TelegramBot.sendFiles(chat: Chat, files: List<MPPFile>) {
        when (files.size) {
            1 -> sendDocument(
                chat.id,
                files.first().asMultipartFile(),
                protectContent = true
            )
            else -> sendDocumentsGroup(
                chat,
                files.map { TelegramMediaDocument(it.asMultipartFile()) },
                protectContent = true
            )
        }
    }

    bot.buildBehaviourWithLongPolling (defaultExceptionsHandler = { it.printStackTrace() }) {
        onCommandWithArgs(command) { message, args ->

            withUploadDocumentAction(message.chat) {
                val count = args.firstOrNull() ?.toIntOrNull() ?: 1
                var sent = false

                var left = count
                val chosen = mutableListOf<MPPFile>()

                while (left > 0) {
                    val picked = pickFile(folder) ?.takeIf { it.filesize > 0 } ?: continue
                    chosen.add(picked)
                    left--
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
                    reply(message, "Nothing selected :(")
                }
            }
        }

        setMyCommands(
            BotCommand(command, "Send some random file in picker directory")
        )

        println(getMe())
    }.join()
}
