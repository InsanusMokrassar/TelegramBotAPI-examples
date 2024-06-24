import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.answers.payments.answerPreCheckoutQueryOk
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.get.getStarTransactions
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPreCheckoutQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.payButton
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.payments.LabeledPrice
import dev.inmo.tgbotapi.types.payments.stars.StarTransaction
import dev.inmo.tgbotapi.utils.*
import kotlinx.coroutines.*

/**
 * The main purpose of this bot is just to answer "Oh, hi, " and add user mention here
 */
@OptIn(PreviewFeature::class)
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val adminUserId = args.getOrNull(1) ?.toLongOrNull() ?.let(::RawChatId) ?.let(::UserId) ?: error("Pass user-admin for full access to the bot")

    val isDebug = args.any { it == "debug" }
    val isTestServer = args.any { it == "testServer" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO), testServer = isTestServer) {
        val me = getMe()

        val payload = "sample payload"
        command("start") {
            reply(
                it,
                price = LabeledPrice("1", 1L),
                title = "Sample",
                description = "Sample description",
                payload = payload,
                replyMarkup = flatInlineKeyboard {
                    payButton("Pay")
                },
            )
        }

        onPreCheckoutQuery(initialFilter = { it.invoicePayload == payload }) {
            answerPreCheckoutQueryOk(it)
        }

        val transactionsDataPrefix = "getStarTransactions"
        fun buildTransactionsData(offset: Int, limit: Int = 10) = "$transactionsDataPrefix $offset $limit"
        fun parseTransactionsData(data: String): Pair<Int, Int> = data.split(" ").drop(1).let {
            it.first().toInt() to it.last().toInt()
        }
        suspend fun buildStarTransactionsPage(offset: Int, limit: Int = 10): Pair<TextSourcesList, InlineKeyboardMarkup> {
            val transactions = getStarTransactions(offset, limit)
            return buildEntities {
                transactions.transactions.forEach {
                    regular("Transaction Id: ") + bold(it.id.string) + "\n"
                    regular("Date: ") + bold(it.date.asDate.toStringDefault()) + "\n"
                    regular("Amount: ") + bold(it.amount.toString()) + "\n"
                    when (it) {
                        is StarTransaction.Incoming -> {
                            regular("Type: ") + bold("incoming") + "\n"
                            regular("Partner: ") + bold(it.partner.type) + "\n"
                        }
                        is StarTransaction.Outgoing -> {
                            regular("Type: ") + bold("outgoing") + "\n"
                            regular("Partner: ") + bold(it.partner.type) + "\n"
                        }
                        is StarTransaction.Unknown -> {
                            regular("Type: ") + bold("unknown") + "\n"
                            regular("Partner: ") + bold(it.partner.type) + "\n"
                        }
                    }
                }
            } to inlineKeyboard {
                row {
                    val prevOffset = (offset - limit).coerceAtLeast(0)
                    if (prevOffset < offset) {
                        dataButton("<", buildTransactionsData(prevOffset, limit))
                    }
                    val nextOffset = (offset + limit)
                    dataButton(">", buildTransactionsData(nextOffset, limit))
                }
            }
        }

        onCommand("transactions", initialFilter = { it.sameChat(adminUserId) }) {
            val (text, keyboard) = buildStarTransactionsPage(0)

            reply(it, text, replyMarkup = keyboard)
        }

        onMessageDataCallbackQuery(Regex("$transactionsDataPrefix \\d+ \\d+")) {
            val (offset, limit) = parseTransactionsData(it.data)
            val (text, keyboard) = buildStarTransactionsPage(offset, limit)
            edit(
                it.message.withContentOrNull<TextContent>() ?: return@onMessageDataCallbackQuery,
                text,
                replyMarkup = keyboard,
            )
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }.second.join()
}
