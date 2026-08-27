import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.extensions.utils.withContent
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.CustomEmojiId
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import dev.inmo.tgbotapi.types.buttons.KeyboardButtonStyle
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.regular
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext

/**
 * Parses pagination callback data whose first two space-separated fields are the page and total page count.
 *
 * @return the parsed page and count, or `null` when either field is missing or is not an integer
 */
fun String.parsePageAndCount(): Pair<Int, Int>? {
    val (pageString, countString) = split(" ").takeIf { it.count() > 1 } ?: return null
    return Pair(
        pageString.toIntOrNull() ?: return null,
        countString.toIntOrNull() ?: return null
    )
}

/**
 * Adds the pagination controls used by command replies and inline-query results.
 *
 * The controls include nearby page callbacks, first/last-page jumps when applicable, a button that copies the
 * corresponding `/inline` command, and a button that starts inline mode for a user-selected chat.
 *
 * @param page the current page; callers should keep it within `1..count`
 * @param count the total number of pages; callers should pass a positive value
 */
fun InlineKeyboardBuilder.includePageButtons(page: Int, count: Int) {
    val numericButtons = listOfNotNull(
        page - 1,
        page,
        page + 1,
    )
    row {
        val numbersRange = 1 .. count
        numericButtons.forEach {
            if (it in numbersRange) {
                dataButton(it.toString(), "$it $count")
            }
        }
    }
    row {
        copyTextButton("Command copy button", "/inline $page $count")
    }

    row {
        if (page - 1 > 2) {
            dataButton("<<", "1 $count", style = KeyboardButtonStyle.Danger)
        }
        if (page - 1 > 1) {
            dataButton("<", "${page - 2} $count", style = KeyboardButtonStyle.Primary)
        }

        if (page + 1 < count) {
            dataButton(">", "${page + 2} $count", style = KeyboardButtonStyle.Success)
        }
        if (page + 2 < count) {
            dataButton(">>", "$count $count", style = KeyboardButtonStyle.Danger)
        }
    }
    row {
        inlineQueryInChosenChatButton(
            "Send somebody page",
            query = "$page $count",
            allowUsers = true,
            allowBots = true,
            allowGroups = true,
            allowChannels = true,
        )
    }
}

/**
 * Creates and runs the shared KeyboardsBot behavior using long polling.
 *
 * The bot serves `/inline` pagination keyboards, edits them in response to callback queries, answers compatible
 * inline queries, offers an `/inline` reply-keyboard button for unhandled commands, and logs every received update.
 *
 * @param token the Telegram bot token
 * @param print receives the bot information returned by the startup `getMe` request
 */
@OptIn(PreviewFeature::class)
suspend fun activateKeyboardsBot(
    token: String,
    print: (Any) -> Unit
) {
    val bot = telegramBot(token)

    print(bot.getMe())

    bot.buildBehaviourWithLongPolling(CoroutineScope(currentCoroutineContext() + SupervisorJob())) {
        onCommandWithArgs("inline") { message, args ->
            val numberArgs = args.mapNotNull { it.toIntOrNull() }
            val numberOfPages = numberArgs.getOrNull(1) ?: numberArgs.firstOrNull() ?: 10
            val page = numberArgs.firstOrNull()?.takeIf { numberArgs.size > 1 }?.coerceAtLeast(1) ?: 1
            reply(
                message,
                replyMarkup = inlineKeyboard {
                    includePageButtons(page, numberOfPages)
                }
            ) {
                regular("Your inline keyboard with $numberOfPages pages")
            }
        }

        onMessageDataCallbackQuery {
            val (page, count) = it.data.parsePageAndCount() ?: it.let {
                answer(it, "Unsupported data :(")
                return@onMessageDataCallbackQuery
            }

            edit(
                it.message.withContent<TextContent>() ?: it.let {
                    answer(it, "Unsupported message type :(")
                    return@onMessageDataCallbackQuery
                },
                replyMarkup = inlineKeyboard {
                    includePageButtons(page, count)
                }
            ) {
                regular("This is $page of $count")
            }
            answer(it)
        }
        onInlineMessageIdDataCallbackQuery {
            val (page, count) = it.data.parsePageAndCount() ?: it.let {
                answer(it, "Unsupported data :(")
                return@onInlineMessageIdDataCallbackQuery
            }

            editMessageText(
                it.inlineMessageId,
                replyMarkup = inlineKeyboard {
                    includePageButtons(page, count)
                }
            ) {
                regular("This is $page of $count")
            }
            answer(it)
        }

        onBaseInlineQuery {
            val page = it.query.takeWhile { it.isDigit() }.toIntOrNull() ?: return@onBaseInlineQuery
            val count = it.query.removePrefix(page.toString()).dropWhile { !it.isDigit() }.takeWhile { it.isDigit() }
                .toIntOrNull() ?: return@onBaseInlineQuery

            answer(
                it,
                results = listOf(
                    InlineQueryResultArticle(
                        InlineQueryId(it.query),
                        "Send buttons",
                        InputTextMessageContent("It is sent via inline mode inline buttons"),
                        replyMarkup = inlineKeyboard {
                            includePageButtons(page, count)
                        }
                    )
                )
            )
        }

        onUnhandledCommand {
            reply(
                it,
                replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                    row {
                        simpleButton("/inline", style = KeyboardButtonStyle.Primary)
                    }
                }
            ) {
                +"Use " + botCommand("inline") + " to get pagination inline keyboard"
            }
        }

        setMyCommands(BotCommand("inline", "Creates message with pagination inline keyboard"))

        allUpdatesFlow.subscribeLoggingDropExceptions(scope = this) {
            println(it)
        }
    }.join()
}
