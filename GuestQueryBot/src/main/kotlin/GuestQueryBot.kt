import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGuestRequestMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.guest_bot_caller_chat
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.guest_bot_caller_user
import dev.inmo.tgbotapi.extensions.utils.publicChatOrNull
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * This bot demonstrates guest mode support introduced in Telegram Bot API.
 *
 * Guest mode allows bots to receive messages and reply within chats they are not a member of.
 * To enable guest queries for your bot, set `supports_guest_queries` in BotFather settings.
 *
 * Key concepts demonstrated:
 * - `supportsGuestQueries` field on the bot itself (via getMe())
 * - `GuestMessageUpdate` — a new update type for messages sent in guest mode
 * - `guestQueryId` — unique ID used to answer the guest query
 * - `guestBotCallerUser` — the user who initiated the guest query
 * - `guestBotCallerChat` — the chat from which the guest query was sent
 * - `answerGuestQuery` / `reply(GuestMessage, InlineQueryResult)` — how to respond
 * - `SentGuestMessage` — the result returned after answering, containing the inline_message_id
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
        testServer = isTestServer
    ) {
        val me = getMe()
        println("Bot info: $me")
        // supportsGuestQueries reflects the supports_guest_queries field from the Telegram API
        println("Supports guest queries: ${me.supportsGuestQueries}")

        onGuestRequestMessage { message ->
            println("=== Guest message received ===")
            // guestQueryId is the unique ID required to answer this guest query
            println("  guestQueryId:       ${message.guestQueryId}")
            println("  from:               ${message.from}")
            println("  chat:               ${message.chat}")
            println("  content:            ${message.content}")

            // reply() on GuestMessage calls answerGuestQuery internally and returns SentGuestMessage
            val sentGuestMessage = reply(
                message,
                InlineQueryResultArticle(
                    id = InlineQueryId(message.guestQueryId.string),
                    title = "Guest reply",
                    inputMessageContent = InputTextMessageContent(
                        buildEntities {
                            +"Guest mode reply"
                            +"\nQuery ID: "
                            +message.guestQueryId.string
                        }
                    ),
                    description = "Reply to guest query from ${message.from.firstName}"
                )
            )
            // SentGuestMessage contains the inline_message_id of the sent reply
            println("  SentGuestMessage:   $sentGuestMessage")
        }

        onContentMessage {
            println(it)
            val userCalledGuestMessage = it.guest_bot_caller_user
            val chatCalledGuestMessage = it.guest_bot_caller_chat ?.publicChatOrNull()
            if (userCalledGuestMessage != null) {
                reply(it) {
                    +"User called guest bot: ${userCalledGuestMessage.lastName + " " + userCalledGuestMessage.firstName}"
                }
            }
            if (chatCalledGuestMessage != null) {
                reply(it) {
                    +"Chat called guest bot: ${chatCalledGuestMessage.title}"
                }
            }
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(scope = this) {
            println(it)
        }
    }.second.join()
}
