import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.business.getBusinessAccountGiftsFlow
import dev.inmo.tgbotapi.extensions.api.gifts.getChatGiftsFlow
import dev.inmo.tgbotapi.extensions.api.gifts.getUserGiftsFlow
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.withTypingAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCompleted
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayContent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCreated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayWinners
import dev.inmo.tgbotapi.types.chat.BusinessChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.chat.UnknownChatType
import dev.inmo.tgbotapi.types.gifts.OwnedGift
import dev.inmo.tgbotapi.types.message.textsources.splitForText
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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

//        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
//            println(it)
//        }
    }.second.join()
}
