import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.polls.sendQuizPoll
import dev.inmo.tgbotapi.extensions.api.send.polls.sendRegularPoll
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollAnswer
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollOptionAdded
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollOptionDeleted
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollUpdates
import dev.inmo.tgbotapi.extensions.utils.accessibleMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.chatContentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.customEmojiTextSourceOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithArgsSources
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.PollId
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.media.TelegramMediaLink
import dev.inmo.tgbotapi.types.media.TelegramMediaLocation
import dev.inmo.tgbotapi.types.media.TelegramMediaSticker
import dev.inmo.tgbotapi.types.media.TelegramMediaVenue
import dev.inmo.tgbotapi.types.message.content.StickerContent
import dev.inmo.tgbotapi.types.polls.InputPollOption
import dev.inmo.tgbotapi.types.polls.PollAnswer
import dev.inmo.tgbotapi.types.polls.QuizPoll
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
 * This bot demonstrates poll features including the new API additions:
 *
 * * `/anonymous` — anonymous regular poll
 * * `/public` — public regular poll with option adding
 * * `/quiz` — quiz poll with random correct answer
 * * `/media_poll` — poll with [TelegramMediaLocation] as poll media (InputMediaLocation),
 *   and [TelegramMediaVenue] as option media (InputMediaVenue / InputPollOptionMedia)
 * * `/quiz_media` — quiz poll with [TelegramMediaLocation] as `media` and [TelegramMediaVenue]
 *   as `explanationMedia` (new [QuizPoll.explanationMedia] field)
 * * `/members_only` — poll with `membersOnly = true` (new [dev.inmo.tgbotapi.types.polls.Poll.membersOnly] field)
 * * `/country_codes` — poll with `countryCodes` (new [dev.inmo.tgbotapi.types.polls.Poll.countryCodes] field)
 * * `/single_option` — poll with just 1 option (minimum options count decreased from 2 to 1)
 * * `/link_poll` — poll whose options carry a [TelegramMediaLink] (InputMediaLink / Bot API 10.1
 *   [dev.inmo.tgbotapi.types.Link]) as [dev.inmo.tgbotapi.types.media.InputPollOptionMedia]
 *
 * [onPollUpdates] prints [dev.inmo.tgbotapi.types.polls.Poll.media], [dev.inmo.tgbotapi.types.polls.Poll.membersOnly],
 * [dev.inmo.tgbotapi.types.polls.Poll.countryCodes], [QuizPoll.explanationMedia], and
 * [dev.inmo.tgbotapi.types.polls.PollOption.media] for each option.
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
                replyParameters = ReplyParameters(it),
                allowAddingOptions = true,
                hideResultsUntilCloses = true,
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
            val correctAnswer = mutableListOf<Int>()
            (1 until Random.nextInt(9)).forEach {
                val option = Random.nextInt(10)
                if (correctAnswer.contains(option)) return@forEach
                correctAnswer.add(option)
            }
            val sentPoll = sendQuizPoll(
                it.chat.id,
                questionEntities = buildEntities {
                    regular("Test quiz poll")
                    if (customEmoji != null) {
                        customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                    }
                },
                descriptionTextSources = buildEntities {
                    regular("Test quiz poll description:")
                    if (customEmoji != null) {
                        customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                    }
                },
                options = (1 .. 10).map {
                    InputPollOption {
                        regular(it.toString()) + " "
                        if (customEmoji != null) {
                            customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                        }
                    }
                },
                isAnonymous = false,
                replyParameters = ReplyParameters(it),
                correctOptionIds = correctAnswer.sorted(),
                allowsMultipleAnswers = correctAnswer.size > 1,
                allowsRevoting = true,
                shuffleOptions = true,
                hideResultsUntilCloses = true,
                explanationTextSources = buildEntities {
                    regular("Random solved it to be ") + underline((correctAnswer + 1).toString()) + " "
                    if (customEmoji != null) {
                        customEmoji(customEmoji.customEmojiId, customEmoji.subsources)
                    }
                }
            )
            println("Sent poll data: $sentPoll")
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        // TelegramMediaLocation implements InputPollMedia and InputPollOptionMedia (InputMediaLocation)
        // TelegramMediaVenue implements InputPollMedia and InputPollOptionMedia (InputMediaVenue)
        // Both can be used as poll question media or as option media
        onCommand("media_poll") {
            val replySticker = it.replyTo ?.contentMessageOrNull() ?.withContentOrNull<StickerContent>() ?.content ?.media
            val sentPoll = sendRegularPoll(
                it.chat.id,
                buildEntities { regular("Which venue would you visit?") },
                listOfNotNull(
                    // InputPollOptionMedia via TelegramMediaVenue (InputMediaVenue)
                    InputPollOption(
                        media = TelegramMediaVenue(
                            latitude = 48.8566,
                            longitude = 2.3522,
                            title = "Eiffel Tower",
                            address = "Champ de Mars, Paris"
                        )
                    ) { regular("Eiffel Tower") },
                    // InputPollOptionMedia via TelegramMediaLocation (InputMediaLocation)
                    InputPollOption(
                        media = TelegramMediaLocation(latitude = 51.5007, longitude = -0.1246)
                    ) { regular("Big Ben") },
                    InputPollOption { regular("Neither") },
                    replySticker ?.let {
                        InputPollOption(media = TelegramMediaSticker(replySticker.fileId)) {
                            regular("Your sticker")
                        }
                    }
                ),
                isAnonymous = false,
                // InputMediaLocation as InputPollMedia — poll question media
                media = TelegramMediaLocation(latitude = 48.8566, longitude = 2.3522),
                replyParameters = ReplyParameters(it)
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        // Demonstrates InputPollMedia on quiz + new QuizPoll.explanationMedia field
        onCommand("quiz_media") {
            val sentPoll = sendQuizPoll(
                it.chat.id,
                questionEntities = buildEntities { regular("Where is the Eiffel Tower?") },
                options = listOf(
                    InputPollOption { regular("Paris") },
                    InputPollOption { regular("London") },
                    InputPollOption { regular("Berlin") },
                ),
                correctOptionIds = listOf(0),
                explanation = "The Eiffel Tower is in Paris, France.",
                isAnonymous = false,
                // InputMediaLocation as InputPollMedia — poll question media (new Poll.media field)
                media = TelegramMediaLocation(latitude = 48.8566, longitude = 2.3522),
                // explanationMedia is new on QuizPoll — media shown with quiz explanation
                explanationMedia = TelegramMediaVenue(
                    latitude = 48.8566,
                    longitude = 2.3522,
                    title = "Eiffel Tower",
                    address = "Champ de Mars, 5 Av. Anatole France, Paris"
                ),
                replyParameters = ReplyParameters(it)
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        // Demonstrates Poll.membersOnly and the membersOnly sendPoll parameter
        onCommand("members_only") {
            val sentPoll = sendRegularPoll(
                it.chat.id,
                buildEntities { regular("Members-only poll") },
                listOf(
                    InputPollOption { regular("Yes") },
                    InputPollOption { regular("No") },
                ),
                isAnonymous = true,
                membersOnly = true,
                replyParameters = ReplyParameters(it)
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        // Demonstrates Poll.countryCodes and the countryCodes sendPoll parameter
        onCommand("country_codes") {
            val sentPoll = sendRegularPoll(
                it.chat.id,
                buildEntities { regular("Country-targeted poll (US, DE, JP)") },
                listOf(
                    InputPollOption { regular("Option A") },
                    InputPollOption { regular("Option B") },
                ),
                isAnonymous = true,
                countryCodes = listOf("US", "DE", "JP"),
                replyParameters = ReplyParameters(it)
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        // Demonstrates that minimum poll options count is now 1 (was 2 before)
        onCommand("single_option") {
            val sentPoll = sendRegularPoll(
                it.chat.id,
                buildEntities { regular("Acknowledge this notice") },
                listOf(
                    InputPollOption { regular("Got it") },
                ),
                isAnonymous = false,
                replyParameters = ReplyParameters(it)
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        // Demonstrates TelegramMediaLink (InputMediaLink, Bot API 10.1) as poll option media.
        // Link is the only new poll media type in 10.1 and is allowed only as InputPollOptionMedia.
        onCommand("link_poll") {
            val sentPoll = sendRegularPoll(
                it.chat.id,
                buildEntities { regular("Pick your favourite resource") },
                listOf(
                    // InputPollOptionMedia via TelegramMediaLink (InputMediaLink)
                    InputPollOption(
                        media = TelegramMediaLink("https://core.telegram.org/bots/api")
                    ) { regular("Bot API docs") },
                    InputPollOption(
                        media = TelegramMediaLink("https://github.com/InsanusMokrassar/ktgbotapi")
                    ) { regular("ktgbotapi") },
                    InputPollOption { regular("None of these") },
                ),
                isAnonymous = false,
                replyParameters = ReplyParameters(it)
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

            // Poll.media — PollMedia attached to the poll question (new field)
            // Poll.membersOnly — whether poll is restricted to channel members (new field)
            // Poll.countryCodes — country restriction list (new field)
            // QuizPoll.explanationMedia — PollMedia attached to quiz explanation (new field)
            // PollOption.media — PollMedia attached to each option (new field)
            val pollInfo = buildString {
                append("[onPollUpdates] anonymous=${it.isAnonymous}")
                append(" | media=${it.media}")
                append(" | membersOnly=${it.membersOnly}")
                append(" | countryCodes=${it.countryCodes}")
                if (it is QuizPoll) {
                    append(" | explanationMedia=${it.explanationMedia}")
                }
                append("\n  options:")
                it.options.forEach { option ->
                    append("\n    ${option.text}: votes=${option.votes}, media=${option.media}")
                }
            }
            send(chatId, pollInfo)
        }

        onPollOptionAdded {
            it.chatEvent.pollMessage ?.accessibleMessageOrNull() ?.chatContentMessageOrNull() ?.let { pollMessage ->
                reply(pollMessage) {
                    +"Poll option added: \n"
                    +it.chatEvent.optionTextSources
                }
            }
        }
        onPollOptionDeleted {
            it.chatEvent.pollMessage ?.accessibleMessageOrNull() ?.chatContentMessageOrNull() ?.let { pollMessage ->
                reply(pollMessage) {
                    +"Poll option deleted: \n"
                    +it.chatEvent.optionTextSources
                }
            }
        }

        onContentMessage {
            val replyPollOptionId = it.replyInfo ?.pollOptionId ?: return@onContentMessage
            it.replyTo ?.accessibleMessageOrNull() ?.chatContentMessageOrNull() ?.let { replied ->
                reply(replied, pollOptionId = replyPollOptionId) {
                    +"Reply to poll option"
                }
            }
        }

        setMyCommands(
            BotCommand("anonymous", "Create anonymous regular poll"),
            BotCommand("public", "Create non anonymous regular poll"),
            BotCommand("quiz", "Create quiz poll with random right answer"),
            BotCommand("media_poll", "Poll with location/venue media on question and options"),
            BotCommand("quiz_media", "Quiz with media and explanationMedia on question/explanation"),
            BotCommand("members_only", "Poll restricted to channel members only (membersOnly)"),
            BotCommand("country_codes", "Poll targeted to US, DE, JP users (countryCodes)"),
            BotCommand("single_option", "Poll with 1 option (minimum is now 1, not 2)"),
            BotCommand("link_poll", "Poll with link media (TelegramMediaLink) on options"),
        )

        allUpdatesFlow.subscribeLoggingDropExceptions(scope = this) {
            println(it)
        }
    }.second.join()
}
