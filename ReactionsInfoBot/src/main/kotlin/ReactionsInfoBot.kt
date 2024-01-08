import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.filter.filtered
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMessageReactionUpdatedByUser
import dev.inmo.tgbotapi.types.reactions.Reaction
import dev.inmo.tgbotapi.utils.DefaultKTgBotAPIKSLog
import dev.inmo.tgbotapi.utils.customEmoji
import dev.inmo.tgbotapi.utils.regular

/**
 * This bot will send info about user reactions in his PM with reply to message user reacted to
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val isDebug = args.getOrNull(1) == "debug"

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val bot = telegramBot(botToken)

    bot.buildBehaviourWithLongPolling {
        onChatMessageReactionUpdatedByUser {
            val result = reply(
                it.chat.id,
                it.messageId,
                replyInChat = it.reactedUser.id
            ) {
                regular("Current reactions for message in reply:\n")
                it.new.forEach {
                    when (it) {
                        is Reaction.CustomEmoji -> regular("• ") + customEmoji(it.customEmojiId) + regular("(customEmojiId: ${it.customEmojiId})")
                        is Reaction.Emoji -> regular("• ${it.emoji}")
                        is Reaction.Unknown -> regular("• Unknown emoji ($it)")
                    }
                    regular("\n")
                }
            }
            println(result)
        }
    }.join()
}
