import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.bot.setMySettings
import dev.inmo.tgbotapi.extensions.api.deleteEphemeralMessage
import dev.inmo.tgbotapi.extensions.api.edit.caption.editEphemeralMessageCaption
import dev.inmo.tgbotapi.extensions.api.edit.media.editEphemeralMessageMedia
import dev.inmo.tgbotapi.extensions.api.edit.text.editEphemeralMessageRichText
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.replyToEphemeral
import dev.inmo.tgbotapi.extensions.api.send.sendRichMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendLivePhoto
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitLivePhotoMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitPhotoMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.fromUserMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.CallbackQueryMode
import dev.inmo.tgbotapi.types.ephemeralReplyReceiverUserIdOrNull
import dev.inmo.tgbotapi.types.media.TelegramMediaLivePhoto
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyEphemeralMessage
import dev.inmo.tgbotapi.types.rich.InputRichMessageBlocks
import korlibs.time.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Runs the ephemeral-messages example bot using long polling.
 *
 * `/ephemeral` posts an inline button whose callback sends a separate rich message visible only to
 * the user who pressed it, edits that message, and deletes it. `/ephemeral_photo` demonstrates uploading new media
 * to an ephemeral edit and moving its caption above the media. `/ephemeral_live_photo` demonstrates collecting both
 * multipart files of a Live Photo edit. Incoming [PossiblyEphemeralMessage] instances receive both an automatic
 * ephemeral [reply] and an explicit [replyToEphemeral].
 *
 * [args] must start with the bot token. The optional exact values `debug` and `testServer` respectively
 * enable console logging and select Telegram's test server.
 *
 * @throws NoSuchElementException when [args] does not contain a bot token
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

    telegramBotWithBehaviourAndLongPolling(
        botToken,
        testServer = isTestServer,
    ) {
        when {
            "inlineCallbacks" in args -> setMySettings(callbackQueryMode = CallbackQueryMode.Inline)
            "defaultCallbacks" in args -> setMySettings(callbackQueryMode = CallbackQueryMode.Default)
        }
        val me = getMe()
        println("Bot info: $me")

        // Post (in a group) a message with an inline button. Tapping it triggers an ephemeral reply that is
        // visible only to the user who tapped.
        onCommand("ephemeral") {
            reply(
                it,
                "Tap the button — the reply will be ephemeral (visible only to you).",
                replyMarkup = flatInlineKeyboard {
                    dataButton("Reveal a secret", "reveal")
                }
            )
        }

        // Telegram resolves the destination from the callback, including callbacks carrying an inline message ID.
        onDataCallbackQuery("reveal") { query ->
            val sent = sendRichMessage(
                callbackQueryId = query.id,
                richMessage = InputRichMessageBlocks {
                    paragraph {
                        plain("🔒 ${query.from.firstName}, here is your personal secret: ")
                        code("42")
                    }
                },
                replaceCallbackQueryMessage = false,
            )
            answer(query)

            val ephemeralMessageId = (sent as? PossiblyEphemeralMessage)?.ephemeralMessageId
                ?: return@onDataCallbackQuery
            delay(3.seconds)
            editEphemeralMessageRichText(
                sent.chat.id,
                query.from.id,
                ephemeralMessageId,
                InputRichMessageBlocks {
                    h2("Secret revealed")
                    paragraph {
                        plain("The answer is ")
                        code("42")
                    }
                },
            )
            delay(3.seconds)
            deleteEphemeralMessage(sent.chat.id, query.from.id, ephemeralMessageId)
        }

        // Upload a received photo again as a brand-new multipart file while editing an ephemeral media message.
        onCommand("ephemeral_photo") { origin ->
            val receiverUserId = origin.fromUserMessageOrNull()?.user?.id ?: return@onCommand
            reply(origin, "Send a photo. I will return it as ephemeral media and then re-upload it in an edit.")
            val photoMessage = waitPhotoMessage().filter {
                it.sameChat(origin) && it.fromUserMessageOrNull()?.user?.id == receiverUserId
            }.first()

            val sent = sendPhoto(
                photoMessage.chat.id,
                photoMessage.content.media.fileId,
                text = "Ephemeral photo using its existing Telegram file ID",
                receiverUserId = receiverUserId,
            )
            val ephemeralMessageId = (sent as? PossiblyEphemeralMessage)?.ephemeralMessageId
                ?: return@onCommand

            val photoBytes = downloadFile(photoMessage.content)
            editEphemeralMessageMedia(
                photoMessage.chat.id,
                receiverUserId,
                ephemeralMessageId,
                TelegramMediaPhoto(
                    photoBytes.asMultipartFile("ephemeral-photo.jpg"),
                    text = "sample"
                ),
            )
            editEphemeralMessageCaption(
                photoMessage.chat.id,
                receiverUserId,
                ephemeralMessageId,
                caption = "This caption is above newly uploaded media",
                showCaptionAboveMedia = true,
            )
        }

        // A Live Photo has a main file plus a secondary `photo` file. ktgbotapi 37.0.0 includes both multipart
        // attachments when EditEphemeralMessageMedia builds its request.
        onCommand("ephemeral_live_photo") { origin ->
            val receiverUserId = origin.fromUserMessageOrNull()?.user?.id ?: return@onCommand
            reply(origin, "Send a Live Photo. I will send it ephemerally and edit it using two new uploads.")
            val livePhotoMessage = waitLivePhotoMessage().filter {
                it.sameChat(origin) && it.fromUserMessageOrNull()?.user?.id == receiverUserId
            }.first()
            val livePhoto = livePhotoMessage.content.media

            val sent = sendLivePhoto(
                chatId = livePhotoMessage.chat.id,
                livePhoto = livePhoto,
                text = "Ephemeral Live Photo using existing Telegram file IDs",
                receiverUserId = receiverUserId,
            )
            val ephemeralMessageId = (sent as? PossiblyEphemeralMessage)?.ephemeralMessageId
                ?: return@onCommand

            val livePhotoBytes = downloadFile(livePhoto)
            val coverPhotoBytes = livePhoto.photo?.let { downloadFile(it) }
            editEphemeralMessageMedia(
                livePhotoMessage.chat.id,
                receiverUserId,
                ephemeralMessageId,
                TelegramMediaLivePhoto(
                    file = livePhotoBytes.asMultipartFile("ephemeral-live-photo.mp4"),
                    photo = coverPhotoBytes?.asMultipartFile("ephemeral-live-photo-cover.jpg")
                        ?: livePhoto.photo?.fileId
                        ?: livePhoto.fileId,
                    text = "Edited with newly uploaded main and cover files",
                ),
            )
        }

        // Incoming ephemeral messages: detect them via PossiblyEphemeralMessage, then answer them.
        onContentMessage { message ->
            val ephemeralMessageId = (message as? PossiblyEphemeralMessage)?.ephemeralMessageId
                ?: return@onContentMessage
            val receiverUserId = message.ephemeralReplyReceiverUserIdOrNull ?: return@onContentMessage

            // reply derives the receiver and ephemeral message ID from the incoming message.
            reply(message, "Got your ephemeral message — I am replying ephemerally too.")

            replyToEphemeral(
                chatId = message.chat.id,
                receiverUserId = receiverUserId,
                ephemeralMessageId = ephemeralMessageId,
                text = "Explicit ephemeral reply via replyToEphemeral",
            )
        }

        setMyCommands(
            // isEphemeral marks a command whose response is an ephemeral (personal) message
            BotCommand("ephemeral", "Post a button that reveals an ephemeral (personal) message", isEphemeral = true),
            BotCommand("ephemeral_photo", "Re-upload a photo through an ephemeral media edit", isEphemeral = true),
            BotCommand("ephemeral_live_photo", "Re-upload both files of an ephemeral Live Photo", isEphemeral = true),
        )

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
