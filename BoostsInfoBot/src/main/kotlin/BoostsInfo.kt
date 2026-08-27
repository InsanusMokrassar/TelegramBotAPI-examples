import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.get.getUserChatBoosts
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatBoostUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatShared
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatReplyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestChannelButton
import dev.inmo.tgbotapi.types.request.RequestId
import dev.inmo.tgbotapi.utils.regular
import korlibs.time.DateFormat
import korlibs.time.format

/**
 * Starts the BoostsInfoBot example using long polling.
 *
 * The `/start` command sends a channel-request keyboard button that accepts channels where this bot is already a
 * member. When Telegram returns the matching `chat_shared` service message, the bot calls [getUserChatBoosts] for
 * the selected channel and the requesting user, then replies with that user's boosts. Incoming `chat_boost` updates
 * are also printed to standard output.
 *
 * @param args the bot token as the first element and, optionally, `debug` as the second element to format and print
 * default KSLog messages to standard output
 */
suspend fun main(args: Array<String>) {
    val isDebug = args.getOrNull(1) == "debug"

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val requestChatId = RequestId(1)

    val bot = telegramBot(args.first())

    bot.buildBehaviourWithLongPolling (defaultExceptionsHandler = { it.printStackTrace() }) {
        onChatBoostUpdated {
            println(it)
        }

        onCommand("start") {
            reply(
                it,
                replyMarkup = flatReplyKeyboard {
                    requestChannelButton(
                        "Click me :)",
                        requestChatId,
                        botIsMember = true
                    )
                }
            ) {
                regular("Select chat to get know about your boosts")
            }
        }

        onChatShared(initialFilter = { it.chatEvent.requestId == requestChatId }) {
            val boostsInfoContrainer = runCatching {
                getUserChatBoosts(it.chatEvent.chatId, it.chat.id)
            }.getOrNull()

            reply(it) {
                when {
                    boostsInfoContrainer == null -> +"Unable to take info about boosts in shared chat"
                    boostsInfoContrainer.boosts.isEmpty() -> +"There is no any boosts in passed chat"
                    else -> {
                        boostsInfoContrainer.boosts.forEach {
                            regular("Boost added: ${DateFormat.FORMAT1.format(it.addDate.asDate)}; Boost expire: ${DateFormat.FORMAT1.format(it.expirationDate.asDate)}; Unformatted: $it") + "\n"
                        }
                    }
                }
            }
        }
    }.join()
}
