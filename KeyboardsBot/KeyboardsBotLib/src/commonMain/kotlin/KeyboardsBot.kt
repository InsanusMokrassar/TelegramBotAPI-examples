import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.extensions.utils.withContent
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.*
import kotlinx.coroutines.*

private const val nextPageData = "next"
private const val previousPageData = "previous"

fun String.parsePageAndCount(): Pair<Int, Int>? {
    val (pageString, countString) = split(" ").takeIf { it.count() > 1 } ?: return null
    return Pair(
        pageString.toIntOrNull() ?: return null,
        countString.toIntOrNull() ?: return null
    )
}

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
        if (page - 1 > 2) {
            dataButton("<<", "1 $count")
        }
        if (page - 1 > 1) {
            dataButton("<", "${page - 2} $count")
        }

        if (page + 1 < count) {
            dataButton(">", "${page + 2} $count")
        }
        if (page + 2 < count) {
            dataButton(">>", "$count $count")
        }
    }
}

suspend fun activateKeyboardsBot(
    token: String,
    print: (Any) -> Unit
) {
    val bot = telegramBot(token)

    print(bot.getMe())

    bot.buildBehaviourWithLongPolling(CoroutineScope(currentCoroutineContext() + SupervisorJob())) {
        onCommandWithArgs("inline") { message, args ->
            val numberOfPages = args.firstOrNull() ?.toIntOrNull() ?: 10
            reply(
                message,
                replyMarkup = inlineKeyboard {
                    row {
                        includePageButtons(1, numberOfPages)
                    }
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
                    row {
                        includePageButtons(page, count)
                    }
                }
            ) {
                regular("This is $page of $count")
            }
            answer(it)
        }

        onUnhandledCommand {
            reply(
                it,
                replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                    row {
                        simpleButton("/inline")
                    }
                }
            ) {
                +"Use " + botCommand("inline") + " to get pagination inline keyboard"
            }
        }

        setMyCommands(BotCommand("inline", "Creates message with pagination inline keyboard"))

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.join()
}
