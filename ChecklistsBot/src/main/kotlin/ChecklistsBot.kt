import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.getMyStarBalance
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.resend
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.suggested.approveSuggestedPost
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitSuggestedPostApproved
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitSuggestedPostDeclined
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChannelDirectMessagesConfigurationChanged
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChecklistContent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChecklistTasksAdded
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChecklistTasksDone
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostApprovalFailed
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostApproved
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostDeclined
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostPaid
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostRefunded
import dev.inmo.tgbotapi.extensions.utils.channelDirectMessagesContentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.previewChannelDirectMessagesChatOrNull
import dev.inmo.tgbotapi.extensions.utils.suggestedChannelDirectMessagesContentMessageOrNull
import dev.inmo.tgbotapi.types.checklists.ChecklistTaskId
import dev.inmo.tgbotapi.types.message.SuggestedPostParameters
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.ChecklistContent
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.code
import dev.inmo.tgbotapi.utils.firstOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

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

    telegramBotWithBehaviourAndLongPolling(
        botToken,
        CoroutineScope(Dispatchers.Default),
        testServer = isTestServer,
    ) {
        // start here!!
        val me = getMe()
        println(me)

        fun ChecklistContent.textBuilderTextSources(): TextSourcesList {
            return buildEntities {
                +checklist.textSources + "\n\n"
                checklist.tasks.forEach { task ->
                    +"• "
                    code(
                        if (task.completionDate != null) {
                            "[x] "
                        } else {
                            "[ ] "
                        }
                    )

                    bold(task.textSources) + "\n"
                }
            }
        }

        onChecklistContent { messageWithContent ->
            reply(messageWithContent) {
                +messageWithContent.content.textBuilderTextSources()
            }
        }

        onChecklistTasksDone { eventMessage ->
            reply(
                eventMessage,
                checklistTaskId = eventMessage.chatEvent.markedAsDone ?.firstOrNull()
            ) {
                eventMessage.chatEvent.checklistMessage.content.checklist
                +eventMessage.chatEvent.checklistMessage.content.textBuilderTextSources()
            }
        }

        onChecklistTasksAdded { messageWithContent ->
            reply(
                messageWithContent.chatEvent.checklistMessage,
                checklistTaskId = messageWithContent.chatEvent.tasks.firstOrNull() ?.id
            ) {
                +messageWithContent.chatEvent.checklistMessage.content.textBuilderTextSources()
            }
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
