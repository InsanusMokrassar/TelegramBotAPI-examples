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
import dev.inmo.tgbotapi.types.InlineQueryId
import dev.inmo.tgbotapi.types.inlineQueryAnswerResultsLimit
import dev.inmo.tgbotapi.utils.buildEntities

/**
 * Thi bot will create inline query answers. You
 * should enable inline queries in bot settings
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
                    val inlineQueryId = InlineQueryId(resultNumber.toString())
                    InlineQueryResultArticle(
                        inlineQueryId,
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
