import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCommunityChatAddedEventsMessages
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCommunityChatJoinedEventsMessages
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCommunityChatRemovedEventsMessages
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommunityChatAdded
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommunityChatJoined
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommunityChatRemoved
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Starts a long-polling bot that demonstrates Telegram Communities.
 *
 * [onCommunityChatAdded] receives the joined [dev.inmo.tgbotapi.types.communities.Community], while
 * [onCommunityChatRemoved] receives a fieldless removal event, while [onCommunityChatJoined] reports a user joining
 * the chat from a community. `/community` reads
 * [dev.inmo.tgbotapi.types.chat.ExtendedChat.community] with [getChat]. The three wait commands use typed event-message
 * expectations to take the first same-chat event without a timeout. The bot prints its [getMe] result and every
 * received update.
 *
 * @param args the bot token followed by the optional, case-sensitive `debug` and `testServer` flags; unknown trailing
 * arguments are ignored
 * @throws NoSuchElementException when the required bot token is absent
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

        // community_chat_joined: a user joined this chat through a community
        onCommunityChatJoined { message ->
            val community = message.chatEvent.community
            println("A user joined chat ${message.chat.id} from community '${community.name}' (id=${community.id.long})")
            reply(message, "Welcome! You joined from the ${community.name} community.")
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

        // Suspend until the next community-added event message from this chat.
        onCommand("wait_community_added") { origin ->
            reply(origin, "Waiting for this chat to be added to a community...")
            val event = waitCommunityChatAddedEventsMessages().filter { it.sameChat(origin) }.first().chatEvent
            reply(origin, "Chat added to community: ${event.community.name} (id=${event.community.id.long})")
        }

        // Suspend until the next community-removed event message from this chat.
        onCommand("wait_community_removed") { origin ->
            reply(origin, "Waiting for this chat to be removed from a community...")
            waitCommunityChatRemovedEventsMessages().filter { it.sameChat(origin) }.first()
            reply(origin, "Chat removed from its community (${origin.chat.id})")
        }

        // Suspend until a user joins this chat from a community.
        onCommand("wait_community_joined") { origin ->
            reply(origin, "Waiting for somebody to join this chat from a community...")
            val event = waitCommunityChatJoinedEventsMessages().filter { it.sameChat(origin) }.first().chatEvent
            reply(origin, "A user joined from ${event.community.name} (id=${event.community.id.long})")
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
