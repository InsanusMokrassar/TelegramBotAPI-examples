import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.textLinkTextSourceOrNull
import dev.inmo.tgbotapi.extensions.utils.uRLTextSourceOrNull
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.message.content.TextedContent
import dev.inmo.tgbotapi.utils.regular

/**
 * This bot will reply with the same
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val isDebug = args.getOrNull(1) == "debug"

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val bot = telegramBot(botToken)

    bot.buildBehaviourWithLongPolling {
        onContentMessage { contentMessage ->
            val url = contentMessage.withContentOrNull<TextedContent>() ?.let { message ->
                message.content.textSources.firstNotNullOfOrNull {
                    it.textLinkTextSourceOrNull() ?.url ?: it.uRLTextSourceOrNull() ?.source
                }
            } ?: null.apply {
                reply(contentMessage) {
                    regular("I am support only content with text contains url only")
                }
            } ?: return@onContentMessage
            contentMessage.withContentOrNull<TextedContent>() ?.let {
                send(
                    it.chat,
                    it.content.textSources,
                    linkPreviewOptions = LinkPreviewOptions.Disabled
                )
                send(
                    it.chat,
                    it.content.textSources,
                    linkPreviewOptions = LinkPreviewOptions.Large(url, showAboveText = true)
                )
                send(
                    it.chat,
                    it.content.textSources,
                    linkPreviewOptions = LinkPreviewOptions.Large(url, showAboveText = false)
                )
                send(
                    it.chat,
                    it.content.textSources,
                    linkPreviewOptions = LinkPreviewOptions.Small(url, showAboveText = true)
                )
                send(
                    it.chat,
                    it.content.textSources,
                    linkPreviewOptions = LinkPreviewOptions.Small(url, showAboveText = false)
                )
                send(
                    it.chat,
                    it.content.textSources,
                    linkPreviewOptions = LinkPreviewOptions.Default(url, showAboveText = true)
                )
                send(
                    it.chat,
                    it.content.textSources,
                    linkPreviewOptions = LinkPreviewOptions.Default(url, showAboveText = false)
                )
            }
        }
    }.join()
}
