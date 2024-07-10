import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.polls.sendQuizPoll
import dev.inmo.tgbotapi.extensions.api.send.polls.sendRegularPoll
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollAnswer
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollUpdates
import dev.inmo.tgbotapi.extensions.utils.customEmojiTextSourceOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithArgsSources
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.PollId
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.polls.InputPollOption
import dev.inmo.tgbotapi.types.polls.PollAnswer
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.customEmoji
import dev.inmo.tgbotapi.utils.regular
import dev.inmo.tgbotapi.utils.underline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * This bot will answer with anonymous or public poll and send message on
 * any update.
 * 
 * * Use `/anonymous` to take anonymous regular poll
 * * Use `/public` to take public regular poll
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val isDebug = args.any { it == "debug" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        val pollToChat = mutableMapOf<PollId, IdChatIdentifier>()
        val pollToChatMutex = Mutex()

        onCommand("anonymous", requireOnlyCommandInMessage = false) {
            val customEmoji = it.content.parseCommandsWithArgsSources()
                .toList()
                .firstOrNull { it.first.command == "anonymous" }
                ?.second
                ?.firstNotNullOfOrNull { it.customEmojiTextSourceOrNull() }
            val sentPoll = sendRegularPoll(
                it.chat.id,
                buildEntities {
                    regular("Test regular anonymous poll")
                    if (customEmoji != null) {
                        customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                    }
                },
                (1 .. 10).map {
                    InputPollOption {
                        regular(it.toString()) + " "
                        if (customEmoji != null) {
                            customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                        }
                    }
                },
                isAnonymous = true,
                replyParameters = ReplyParameters(it)
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        onCommand("public", requireOnlyCommandInMessage = false) {
            val customEmoji = it.content.parseCommandsWithArgsSources()
                .toList()
                .firstOrNull { it.first.command == "public" }
                ?.second
                ?.firstNotNullOfOrNull { it.customEmojiTextSourceOrNull() }
            val sentPoll = sendRegularPoll(
                it.chat.id,
                buildEntities {
                    regular("Test regular non anonymous poll")
                    if (customEmoji != null) {
                        customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                    }
                },
                (1 .. 10).map {
                    InputPollOption {
                        regular(it.toString()) + " "
                        if (customEmoji != null) {
                            customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                        }
                    }
                },
                isAnonymous = false,
                replyParameters = ReplyParameters(it)
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        onCommand("quiz", requireOnlyCommandInMessage = false) {
            val customEmoji = it.content.parseCommandsWithArgsSources()
                .toList()
                .firstOrNull { it.first.command == "quiz" }
                ?.second
                ?.firstNotNullOfOrNull { it.customEmojiTextSourceOrNull() }
            val correctAnswer = Random.nextInt(10)
            val sentPoll = sendQuizPoll(
                it.chat.id,
                questionEntities = buildEntities {
                    regular("Test quiz poll")
                    if (customEmoji != null) {
                        customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                    }
                },
                (1 .. 10).map {
                    InputPollOption {
                        regular(it.toString()) + " "
                        if (customEmoji != null) {
                            customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                        }
                    }
                },
                isAnonymous = false,
                replyParameters = ReplyParameters(it),
                correctOptionId = correctAnswer,
                explanationTextSources = buildEntities {
                    regular("Random solved it to be ") + underline((correctAnswer + 1).toString()) + " "
                    if (customEmoji != null) {
                        customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                    }
                }
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        onPollAnswer {
            val chatId = pollToChat[it.pollId] ?: return@onPollAnswer

            when(it) {
                is PollAnswer.Public -> send(chatId, "[onPollAnswer] User ${it.user} have answered")
                is PollAnswer.Anonymous -> send(chatId, "[onPollAnswer] Chat ${it.voterChat} have answered")
            }
        }

        onPollUpdates {
            val chatId = pollToChat[it.id] ?: return@onPollUpdates

            when(it.isAnonymous) {
                false -> send(chatId, "[onPollUpdates] Public poll updated: ${it.options.joinToString()}")
                true -> send(chatId, "[onPollUpdates] Anonymous poll updated: ${it.options.joinToString()}")
            }
        }

        setMyCommands(
            BotCommand("anonymous", "Create anonymous regular poll"),
            BotCommand("public", "Create non anonymous regular poll"),
            BotCommand("quiz", "Create quiz poll with random right answer"),
        )

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }.second.join()
}
