import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.invite_links.answerChatJoinRequestQuery
import dev.inmo.tgbotapi.extensions.api.chat.invite_links.sendChatJoinRequestWebApp
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatJoinRequest
import dev.inmo.tgbotapi.requests.chat.invite_links.ChatJoinRequestQueryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * This bot demonstrates Join Request Queries support introduced in Telegram Bot API 10.1.
 *
 * A "guard bot" of a chat receives chat join requests as queries and must process them with
 * [answerChatJoinRequestQuery] or hand the user a Web App via [sendChatJoinRequestWebApp]
 * (for example, to run a captcha / verification flow before deciding).
 *
 * Your bot must be set as the guard bot of the chat and must have `can_invite_users` rights to
 * receive these requests.
 *
 * Key concepts demonstrated:
 * - [dev.inmo.tgbotapi.types.chat.ExtendedBot.supportsJoinRequestQueries] — whether the bot itself
 *   supports join request queries (from getMe(), maps `User.supports_join_request_queries`)
 * - [dev.inmo.tgbotapi.types.chat.ExtendedChat.guardBot] — the bot that processes join request
 *   queries in a chat (from getChat(), maps `ChatFullInfo.guard_bot`)
 * - [dev.inmo.tgbotapi.types.chat.ChatJoinRequest.queryId] — the [dev.inmo.tgbotapi.types.ChatJoinRequestQueryId]
 *   present when the request arrives as a query to the guard bot
 * - [answerChatJoinRequestQuery] with [ChatJoinRequestQueryResult] (Approve / Decline / Queue / Unknown)
 * - [sendChatJoinRequestWebApp] — open a Web App to process the request
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val isDebug = args.any { it == "debug" }
    val isTestServer = args.any { it == "testServer" }
    // pass a https url as the second argument to demonstrate sendChatJoinRequestWebApp
    val webAppUrl = args.getOrNull(1) ?.takeIf { it.startsWith("https://") }

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
        // supportsJoinRequestQueries reflects the supports_join_request_queries field from the Telegram API
        println("Supports join request queries: ${me.supportsJoinRequestQueries}")

        onChatJoinRequest { request ->
            println("=== Chat join request received ===")
            println("  from:        ${request.from}")
            println("  chat:        ${request.chat}")
            println("  bio:         ${request.bio}")
            // queryId is non-null only when the request arrives as a query to this bot as the guard bot
            println("  queryId:     ${request.queryId}")

            // guardBot is the bot processing join request queries in this chat (admins-only field)
            val guardBot = runCatching { getChat(request.chat).guardBot }.getOrNull()
            println("  guardBot:    $guardBot")

            val queryId = request.queryId
            if (queryId == null) {
                println("  -> request has no queryId, this bot is not the guard bot here")
                return@onChatJoinRequest
            }

            if (webAppUrl != null) {
                // sendChatJoinRequestWebApp: hand the user a Web App (e.g. captcha) instead of deciding now
                sendChatJoinRequestWebApp(request, webAppUrl)
                println("  -> sent join request Web App: $webAppUrl")
                return@onChatJoinRequest
            }

            // answerChatJoinRequestQuery with one of the ChatJoinRequestQueryResult variants:
            //   Approve  — allow the user to join
            //   Decline  — disallow the user to join
            //   Queue    — leave the decision to other administrators
            //   Unknown  — any future result not yet known to the library
            val result = if (request.bio.isNullOrBlank()) {
                // no bio -> let other admins decide
                ChatJoinRequestQueryResult.Queue
            } else {
                // has a bio -> approve
                ChatJoinRequestQueryResult.Approve
            }
            answerChatJoinRequestQuery(request, result)
            println("  -> answered with: ${result.name}")
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(scope = this) {
            println(it)
        }
    }.second.join()
}
