import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.business.getBusinessAccountGiftsFlow
import dev.inmo.tgbotapi.extensions.api.gifts.getChatGiftsFlow
import dev.inmo.tgbotapi.extensions.api.gifts.getUserGiftsFlow
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.withTypingAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUniqueGiftSentOrReceived
import dev.inmo.tgbotapi.types.chat.BusinessChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.chat.UnknownChatType
import dev.inmo.tgbotapi.types.gifts.OwnedGift
import dev.inmo.tgbotapi.types.message.textsources.splitForText
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regular
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Starts a long-polling bot whose standalone `/start` command lists all owned gifts for the current chat.
 *
 * Business chats are queried through their business connection, private chats through their user ID, and public or
 * unknown chat types through their chat ID. Regular and unique gifts are rendered as formatted text and long results
 * are split across replies. Unique-gift service messages also expose the Bot API 10.3 text, entities and privacy flag.
 * The bot prints its [getMe] result at startup.
 *
 * @param args the bot token followed by optional, case-sensitive `debug` and `testServer` flags; unknown trailing
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

    telegramBotWithBehaviourAndLongPolling(botToken, testServer = isTestServer) {
        // start here!!
        val me = getMe()
        println(me)

        onUniqueGiftSentOrReceived { message ->
            val uniqueGiftInfo = message.chatEvent
            println(
                "Unique gift ${uniqueGiftInfo.gift.name.value}: " +
                    "text=${uniqueGiftInfo.text}, textSources=${uniqueGiftInfo.textSources}, " +
                    "isPrivate=${uniqueGiftInfo.isPrivate}"
            )
            reply(
                message,
                buildEntities {
                    bold("Unique gift") + ": ${uniqueGiftInfo.gift.name.value}\n"
                    bold("Private") + ": ${uniqueGiftInfo.isPrivate}\n"
                    bold("Text") + ": "
                    if (uniqueGiftInfo.textSources.isEmpty()) {
                        regular(uniqueGiftInfo.text ?: "(None)")
                    } else {
                        +uniqueGiftInfo.textSources
                    }
                }
            )
        }

        onCommand("start") {
            val giftsFlow = when (val chat = it.chat) {
                is BusinessChat -> {
                    getBusinessAccountGiftsFlow(
                        chat.id.businessConnectionId
                    )
                }
                is PrivateChat -> {
                    getUserGiftsFlow(it.chat.id)
                }
                is UnknownChatType,
                is PublicChat -> {
                    getChatGiftsFlow(it.chat.id)
                }
            }

            withTypingAction(it.chat) {
                val texts = buildEntities {
                    giftsFlow.collect { ownedGifts ->
                        ownedGifts.gifts.forEach {
                            when (it) {
                                is OwnedGift.Regular.Common -> {
                                    bold("Type") + ": Regular common\n"
                                    bold("Id") + ": ${it.gift.id.string}\n"
                                    bold("Text") + ": ${it.text ?: "(None)"}\n"
                                    bold("Stars cost") + ": ${it.gift.starCount}\n"
                                }
                                is OwnedGift.Unique.Common -> {
                                    bold("Type") + ": Unique common\n"
                                    bold("Id") + ": ${it.gift.id ?.string ?: "(None)"}\n"
                                    bold("Name") + ": ${it.gift.name.value}\n"
                                    bold("Model") + ": ${it.gift.model.name}\n"
                                    bold("Number") + ": ${it.gift.number}\n"
                                }
                                is OwnedGift.Regular.OwnedByBusinessAccount -> {
                                    bold("Type") + ": Regular owned by business\n"
                                    bold("Id") + ": ${it.gift.id.string}\n"
                                    bold("Text") + ": ${it.text ?: "(None)"}\n"
                                    bold("Stars cost") + ": ${it.gift.starCount}\n"
                                }
                                is OwnedGift.Unique.OwnedByBusinessAccount -> {
                                    bold("Type") + ": Unique owned by business\n"
                                    bold("Id") + ": ${it.gift.id ?.string ?: "(None)"}\n"
                                    bold("Name") + ": ${it.gift.name.value}\n"
                                    bold("Model") + ": ${it.gift.model.name}\n"
                                    bold("Number") + ": ${it.gift.number}\n"
                                }
                            }
                        }
                    }
                }
                val preparedTexts = texts.splitForText()
                if (preparedTexts.isEmpty()) {
                    reply(it, "This chat have no any gifts")
                } else {
                    preparedTexts.forEach { preparedText -> reply(it, preparedText) }
                }
            }
        }

//        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
//            println(it)
//        }
    }.second.join()
}
