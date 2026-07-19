import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.deleteEphemeralMessage
import dev.inmo.tgbotapi.extensions.api.edit.text.editEphemeralMessageText
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.replyToEphemeral
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyEphemeralMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

/**
 * This bot demonstrates Ephemeral Messages support introduced in Telegram Bot API 10.2.
 *
 * An ephemeral message lives inside a group chat but is shown to exactly one user (its `receiver`).
 * It is addressed not by a normal message id, but by a per-receiver [dev.inmo.tgbotapi.types.EphemeralMessageId]
 * together with that receiver's [dev.inmo.tgbotapi.types.UserId]. Bots typically send ephemeral messages in
 * response to a callback query (so a `callbackQueryId` is available) or reply ephemerally to an ephemeral
 * message the bot itself received.
 *
 * Key concepts demonstrated:
 * - [sendTextMessage] with `receiverUserId` + `callbackQueryId` — sends the outgoing message as ephemeral,
 *   visible only to `receiverUserId` in the group chat (one of the 13 ephemeral-capable send requests)
 * - [PossiblyEphemeralMessage] — the marker interface (`receiverUser` / `ephemeralMessageId`) implemented by
 *   the group-family `Common*ContentMessage` types; the way to detect that a message is ephemeral
 * - [editEphemeralMessageText] / [deleteEphemeralMessage] — edit / delete an ephemeral message addressed by
 *   `chatId` + `receiverUserId` + [dev.inmo.tgbotapi.types.EphemeralMessageId] ([deleteEphemeralMessage] also
 *   accepts a [PossiblyEphemeralMessage] directly)
 * - [reply] smart-branch — replying to an ephemeral message automatically sends the reply ephemeral to the
 *   same receiver (see [dev.inmo.tgbotapi.types.ephemeralReplyParametersOrNull])
 * - [replyToEphemeral] — the explicit form: reply to an ephemeral message by `chatId` + `receiverUserId` +
 *   `ephemeralMessageId` without needing the original [PossiblyEphemeralMessage] object
 * - [BotCommand.isEphemeral] — the new command flag marking a command whose response is ephemeral
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
        CoroutineScope(Dispatchers.IO),
        testServer = isTestServer,
    ) {
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

        // Send an ephemeral message in response to a callback query. `receiverUserId` + `callbackQueryId`
        // make the outgoing message ephemeral — Telegram shows it only to the querying user (and this also
        // serves as the answer to the callback query).
        onMessageDataCallbackQuery(Regex("reveal")) { query ->
            val chatId = query.message.chat.id
            val receiverUserId = query.from.id

            val sent = sendTextMessage(
                chatId,
                "🔒 ${query.from.firstName}, here is your personal secret: 42",
                receiverUserId = receiverUserId,
                callbackQueryId = query.id,
            )

            // Only the group-family Common*ContentMessage types implement PossiblyEphemeralMessage, so the
            // sent ephemeral message exposes its ephemeralMessageId through that interface.
            val ephemeralMessageId = (sent as? PossiblyEphemeralMessage)?.ephemeralMessageId
            if (ephemeralMessageId != null) {
                delay(3000)
                // editEphemeralMessageText: address the ephemeral message by chatId + receiverUserId + ephemeralMessageId
                editEphemeralMessageText(chatId, receiverUserId, ephemeralMessageId, "🔓 Revealed: the answer is 42")
                delay(3000)
                // deleteEphemeralMessage: same addressing (there is also a PossiblyEphemeralMessage overload)
                deleteEphemeralMessage(chatId, receiverUserId, ephemeralMessageId)
            }
        }

        // Incoming ephemeral messages: detect them via PossiblyEphemeralMessage, then answer them.
        onContentMessage { message ->
            val ephemeral = (message as? PossiblyEphemeralMessage)?.takeIf { it.ephemeralMessageId != null }
                ?: return@onContentMessage

            // reply smart-branch: because `message` is ephemeral, this reply is sent ephemeral to the same
            // receiver automatically — no ephemeral parameters needed here.
            reply(message, "Got your ephemeral message — I am replying ephemerally too.")

            // The explicit equivalent, addressing the ephemeral message by hand:
            val receiverUserId = ephemeral.receiverUser?.id
            if (receiverUserId != null) {
                replyToEphemeral(
                    message.chat.id,
                    receiverUserId,
                    ephemeral.ephemeralMessageId!!,
                    "Explicit ephemeral reply via replyToEphemeral",
                )
            }
        }

        setMyCommands(
            // isEphemeral marks a command whose response is an ephemeral (personal) message
            BotCommand("ephemeral", "Post a button that reveals an ephemeral (personal) message", isEphemeral = true),
        )

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
