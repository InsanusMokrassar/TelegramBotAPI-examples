import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.MessageIsNotModifiedException
import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.api.answers.answerWidgetQuery
import dev.inmo.tgbotapi.extensions.api.edit.widgets.editMessageWidgetData
import dev.inmo.tgbotapi.extensions.api.edit.widgets.editSharedWidgetData
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendSharedWidget
import dev.inmo.tgbotapi.extensions.api.send.sendWidget
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onAnyInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSharedWidgetQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onWidgetQuery
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputWidgetMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import dev.inmo.tgbotapi.types.chat.ChatType
import dev.inmo.tgbotapi.types.widgets.WidgetQuery
import dev.inmo.tgbotapi.types.widgets.WidgetQuerySource
import dev.inmo.tgbotapi.types.widgets.WidgetQueryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

private suspend fun TelegramBot.rollDie(query: WidgetQuerySource) {
    if (query.type !is WidgetQueryType.Data || query.data != "roll") {
        answerWidgetQuery(query, "Unknown die action.")
        return
    }

    val roll = Random.nextInt(1, 7).toString()
    answerWidgetQuery(query, "You rolled $roll.")

    val widgetId = query.widgetId
    val original = query as? WidgetQuery
    val inlineMessageId = original?.inlineMessageId
    val chat = original?.chat
    val messageId = original?.messageId
    try {
        when {
            widgetId != null -> editSharedWidgetData(widgetId, publicData = roll, cacheTime = 5)
            inlineMessageId != null -> editMessageWidgetData(inlineMessageId, chatData = roll)
            chat != null && messageId != null -> editMessageWidgetData(chat, messageId, chatData = roll)
        }
    } catch (_: MessageIsNotModifiedException) {
        // Two consecutive rolls can have the same value. The widget already shows it.
    }
}

suspend fun main(vararg args: String) {
    val botToken = requireNotNull(args.firstOrNull()) { "Pass the bot token as the first argument." }
    val isTestServer = "testServer" in args
    if ("debug" in args) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }
    val dieHtml = checkNotNull(Thread.currentThread().contextClassLoader.getResource("die.html")).readText()
    val messageDie = InputWidgetMessageContent(
        html = dieHtml,
        chatData = "No rolls yet",
        allowQueries = true,
        text = "Roll this die to update its last roll in this message."
    )
    val inlineDie = InlineQueryResultArticle(
        id = InlineQueryId("die"),
        title = "Roll a die",
        inputMessageContent = messageDie
    )

    telegramBotWithBehaviourAndLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        testServer = isTestServer
    ) {
        onCommand("start") {
            reply(it, "Use /widget for a die in one message or /sharedwidget to roll the same die in two messages.")
        }
        onCommand("widget") { message ->
            sendWidget(message.chat, messageDie)
        }
        onCommand("sharedwidget") { message ->
            val first = sendWidget(
                message.chat,
                html = dieHtml,
                publicData = "No rolls yet",
                allowQueries = true,
                isShared = true,
                text = "This die shares its last roll with every copy."
            )
            val widgetId = checkNotNull(first.content.widget.id) { "Telegram did not return a shared widget ID." }
            sendSharedWidget(message.chat, widgetId, text = "Roll here and watch the other message update.")
        }
        onAnyInlineQuery { query ->
            val results = if (isTestServer && query.chatType != ChatType.Channel) {
                listOf(inlineDie)
            } else {
                emptyList()
            }
            answerInlineQuery(query, results, cachedTime = 0)
        }

        onWidgetQuery { rollDie(it) }
        onSharedWidgetQuery { rollDie(it) }
    }.second.join()
}
