import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDeepLink
import dev.inmo.tgbotapi.requests.answers.InlineQueryResultsButton
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.inlineQueryAnswerResultsLimit
import dev.inmo.tgbotapi.utils.buildEntities

/**
 * This bot will send files inside of working directory OR from directory in the second argument.
 * You may send /send_file command to this bot to get random file from the directory OR
 * `/send_file $number` when you want to receive required number of files. For example,
 * /send_file and `/send_file 1` will have the same effect - bot will send one random file.
 * But if you will send `/send_file 5` it will choose 5 random files and send them as group
 */
suspend fun doInlineQueriesBot(token: String) {
    val bot = telegramBot(token)

    bot.buildBehaviourWithLongPolling(
        defaultExceptionsHandler = { it.printStackTrace() },
    ) {
        onBaseInlineQuery {
            val page = it.offset.toIntOrNull() ?: 0
            val results = (0 until inlineQueryAnswerResultsLimit.last).map {
                (page * inlineQueryAnswerResultsLimit.last) + it
            }

            answer(
                it,
                results = results.map { resultNumber ->
                    val resultAsString = resultNumber.toString()
                    InlineQueryResultArticle(
                        resultAsString,
                        "Title $resultNumber",
                        InputTextMessageContent(
                            buildEntities {
                                +"Result text of " + resultNumber.toString() + " result:\n"
                                +it.query
                            }
                        ),
                        description = "Description of $resultNumber result"
                    )
                },
                cachedTime = 0,
                isPersonal = true,
                button = InlineQueryResultsButton.Start(
                    "Text of button with page $page",
                    "deep_link_for_page_$page"
                ),
                nextOffset = (page + 1).toString()
            )
        }

        onDeepLink { (message, deepLink) ->
            reply(message, deepLink)
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }

        println(getMe())
    }.join()
}
