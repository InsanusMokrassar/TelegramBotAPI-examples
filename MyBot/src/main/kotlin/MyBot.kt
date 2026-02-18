import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.removeMyProfilePhoto
import dev.inmo.tgbotapi.extensions.api.bot.setMyProfilePhoto
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.files.downloadFileToTemp
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessageDraftFlowWithTexts
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitPhotoMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.requests.business_connection.InputProfilePhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * This is one of the easiest bots - it will just print information about itself
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val isDebug = args.any { it == "debug" }
    val isTestServer = args.any { it == "testServer" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val bot = telegramBot(botToken)

    telegramBotWithBehaviourAndLongPolling(
        botToken,
        CoroutineScope(Dispatchers.Default),
        testServer = isTestServer,
    ) {
        val me = bot.getMe()
        println(me)
        println(bot.getChat(me))

        onCommand("setMyProfilePhoto") { commandMessage ->
            reply(commandMessage, "ok, send me new photo")
            val newPhotoMessage = waitPhotoMessage().filter { potentialPhotoMessage ->
                potentialPhotoMessage.sameChat(commandMessage)
            }.first()
            val draftMessagesChannel = Channel<String>(capacity = 1)

            launchLoggingDropExceptions {
                sendMessageDraftFlowWithTexts(commandMessage.chat.id, draftMessagesChannel.consumeAsFlow())
            }.invokeOnCompletion {
                draftMessagesChannel.close(it)
            }

            draftMessagesChannel.send("Start downloading photo")
            val photoFile = downloadFileToTemp(newPhotoMessage.content)

            draftMessagesChannel.send("Photo file have been downloaded. Start set my profile photo")

            val setResult = setMyProfilePhoto(
                InputProfilePhoto.Static(
                    photoFile.asMultipartFile()
                )
            )
            if (setResult) {
                reply(commandMessage, "New photo have been set")
            }
        }

        onCommand("removeMyProfilePhoto") {
            runCatchingLogging {
                if (removeMyProfilePhoto()) {
                    reply(it, "Photo have been removed")
                }
            }.onFailure { e ->
                e.printStackTrace()
                reply(it, "Something web wrong. See logs for details.")
            }
        }
    }.second.join()
}
