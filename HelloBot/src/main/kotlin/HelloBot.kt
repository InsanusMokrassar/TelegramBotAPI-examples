import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMentionWithAnyContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.sender_chat
import dev.inmo.tgbotapi.extensions.utils.formatting.linkMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.ifChannelChat
import dev.inmo.tgbotapi.extensions.utils.ifFromChannelGroupContentMessage
import dev.inmo.tgbotapi.types.chat.*
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import kotlinx.coroutines.*

/**
 * The main purpose of this bot is just to answer "Oh, hi, " and add user mention here
 */
@OptIn(PreviewFeature::class)
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        val me = getMe()
        onMentionWithAnyContent(me) { message ->
            val chat = message.chat

            val answerText = when (val chat = message.chat) {
                is PreviewChannelChat -> {
                    val answer = "Hi everybody in this channel \"${chat.title}\""
                    reply(message, answer, MarkdownV2)
                    return@onMentionWithAnyContent
                }
                is PreviewPrivateChat -> {
                    reply(message, "Hi, " + "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id), MarkdownV2)
                    return@onMentionWithAnyContent
                }
                is PreviewGroupChat -> {
                    message.ifFromChannelGroupContentMessage {
                        val answer = "Hi, ${it.senderChat.title}"
                        reply(message, answer, MarkdownV2)
                        return@onMentionWithAnyContent
                    }
                    "Oh, hi, " + when (chat) {
                        is SupergroupChat -> (chat.username ?.username ?: getChat(chat).inviteLink) ?.let {
                            chat.title.linkMarkdownV2(it)
                        } ?: chat.title
                        else -> bot.getChat(chat).inviteLink ?.let {
                            chat.title.linkMarkdownV2(it)
                        } ?: chat.title
                    }
                }
                is PreviewBusinessChat -> {
                    reply(message, "Hi, " + "${chat.original.firstName} ${chat.original.lastName} (as business chat :) )".textMentionMarkdownV2(chat.original.id), MarkdownV2)
                    return@onMentionWithAnyContent
                }
                is UnknownChatType -> "Unknown :(".escapeMarkdownV2Common()
            }
            reply(
                message,
                answerText,
                MarkdownV2
            )
        }
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }.second.join()
}
