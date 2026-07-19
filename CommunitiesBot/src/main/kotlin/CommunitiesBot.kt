import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCommunityChatAdded
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommunityChatAdded
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommunityChatRemoved
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/**
 * This bot demonstrates Communities support introduced in Telegram Bot API 10.2.
 *
 * A community groups several chats together. When a chat is added to (or removed from) a community, the bot
 * receives a service event in that chat.
 *
 * Key concepts demonstrated:
 * - [onCommunityChatAdded] — trigger for the `community_chat_added` service event. The handler receives a
 *   [dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage] carrying a
 *   [dev.inmo.tgbotapi.types.communities.CommunityChatAdded] whose `community` is the
 *   [dev.inmo.tgbotapi.types.communities.Community] (`id`: [dev.inmo.tgbotapi.types.communities.CommunityId],
 *   `name`) the chat was added to
 * - [onCommunityChatRemoved] — trigger for the fieldless `community_chat_removed` service event
 * - [waitCommunityChatAdded] — expectation returning a flow of [dev.inmo.tgbotapi.types.communities.CommunityChatAdded]
 * - [dev.inmo.tgbotapi.types.chat.ExtendedChat.community] — the community a chat belongs to
 *   (`ChatFullInfo.community`), available directly from [getChat] without any cast
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

        // community_chat_added: the chat was added to a community
        onCommunityChatAdded { message ->
            val community = message.chatEvent.community
            println("Chat ${message.chat.id} was added to community '${community.name}' (id=${community.id.long})")
            send(message.chat.id, "This chat has joined the community: ${community.name}")

            // community is exposed on ExtendedChat itself (ChatFullInfo.community) — no cast needed
            val extended = getChat(message.chat.id)
            println("getChat().community = ${extended.community?.name} / ${extended.community?.id?.long}")
        }

        // community_chat_removed: a fieldless event — the chat left its community
        onCommunityChatRemoved { message ->
            println("Chat ${message.chat.id} was removed from its community")
            send(message.chat.id, "This chat has left its community")
        }

        // Inspect the current chat's community on demand
        onCommand("community") {
            val community = getChat(it.chat.id).community
            reply(
                it,
                if (community != null) {
                    "Community: ${community.name} (id=${community.id.long})"
                } else {
                    "This chat is not part of any community"
                }
            )
        }

        // waitCommunityChatAdded expectation: suspend until this chat is added to a community
        onCommand("wait_community") {
            reply(it, "Waiting for this chat to be added to a community...")
            val event = waitCommunityChatAdded().first()
            reply(it, "Chat added to community: ${event.community.name} (id=${event.community.id.long})")
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
