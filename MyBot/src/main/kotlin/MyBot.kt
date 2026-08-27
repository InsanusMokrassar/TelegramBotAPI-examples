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
 * Runs the MyBot profile-photo example using long polling.
 *
 * Startup bot information is printed to standard output. The bot then handles commands that replace its profile
 * photo from the next photo received in the same chat or remove its current profile photo.
 *
 * @param args the bot token first, followed optionally by `debug` for formatted KSLog output and/or `testServer` for
 * the Telegram Bot API test environment
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

            setMyProfilePhoto(
                InputProfilePhoto.Static(
                    photoFile.asMultipartFile()
                )
            )
            reply(commandMessage, "New photo have been set")
        }

        onCommand("removeMyProfilePhoto") {
            runCatchingLogging {
                removeMyProfilePhoto()
                reply(it, "Photo have been removed")
            }.onFailure { e ->
                e.printStackTrace()
                reply(it, "Something web wrong. See logs for details.")
            }
        }
    }.second.join()
}
