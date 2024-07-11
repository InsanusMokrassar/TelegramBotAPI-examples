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
            val boosts = getUserChatBoosts(it.chatEvent.chatId, it.chat.id)
            reply(
                it
            ) {
                boosts.boosts.forEach {
                    regular("Boost added: ${DateFormat.FORMAT1.format(it.addDate.asDate)}; Boost expire: ${DateFormat.FORMAT1.format(it.expirationDate.asDate)}; Unformatted: $it") + "\n"
                }
            }
        }
    }.join()
}
