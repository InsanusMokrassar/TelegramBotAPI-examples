import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.bot.getMe
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.media.sendMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.telegramBot
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.safely
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.shortcuts.*
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.abstracts.MediaGroupContent
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.abstracts.MessageContent
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.w3c.dom.*

private val scope = CoroutineScope(Dispatchers.Default)

fun main() {
    document.addEventListener(
        "DOMContentLoaded",
        {
            val botsContainer = document.getElementById("bots_container") ?: return@addEventListener

            (document.getElementById("bot_token_form") as? HTMLFormElement) ?.onsubmit = {
                val botContainer = document.createElement("div") as HTMLDivElement
                botsContainer.append(botContainer)

                val statusDiv = document.createElement("div") as HTMLDivElement
                botContainer.append(statusDiv)

                val lastRequestAnswerDiv = document.createElement("div") as HTMLDivElement
                botContainer.append(lastRequestAnswerDiv)

                val token = (document.getElementById("bot_token") as? HTMLInputElement) ?.value
                if (token != null) {
                    val bot = telegramBot(token)
                    scope.launch {
                        statusDiv.innerHTML = "Loaded bot: ${bot.getMe()}"

                        bot.startGettingFlowsUpdatesByLongPolling {
                            filterContentMessages<MessageContent>(scope).onEach {
                                it.content.createResends(it.chat.id, replyToMessageId = it.messageId).forEach {
                                    bot.executeUnsafe(it) ?.also {
                                        lastRequestAnswerDiv.innerHTML = it.toString()
                                    }
                                }
                            }.launchIn(scope)
                            filterMediaGroupMessages<MediaGroupContent>(scope).onEach {
                                safely {
                                    bot.sendMediaGroup(
                                        it.first().chat,
                                        it.map { it.content.toMediaGroupMemberInputMedia() },
                                        replyToMessageId = it.first().messageId
                                    ).also {
                                        lastRequestAnswerDiv.innerHTML = it.toString()
                                    }
                                }
                            }.launchIn(scope)
                        }
                    }
                }

                false
            }
        }
    )
}
