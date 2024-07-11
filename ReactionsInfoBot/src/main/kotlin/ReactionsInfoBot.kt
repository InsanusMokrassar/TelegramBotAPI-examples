import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.setMessageReaction
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMessageReactionUpdatedByUser
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMessageReactionsCountUpdated
import dev.inmo.tgbotapi.types.chat.ExtendedChat
import dev.inmo.tgbotapi.types.reactions.Reaction
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
            setMessageReaction(
                it.chat.id,
                it.messageId,
                "✍"
            )
            val replyResult = reply(
                it.chat.id,
                it.messageId,
                replyInChatId = it.reactedUser.id
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
            setMessageReaction(
                it.chat.id,
                it.messageId,
            )
        }
        onChatMessageReactionsCountUpdated {
            val extendedChat: ExtendedChat = getChat(it.chat)
            println(extendedChat)
            println(it)
        }
    }.join()
}
