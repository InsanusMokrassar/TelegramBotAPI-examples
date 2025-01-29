import dev.inmo.micro_utils.coroutines.defaultSafelyWithoutExceptionHandler
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.get.getCustomEmojiStickerOrNull
import dev.inmo.tgbotapi.extensions.api.get.getStickerSet
import dev.inmo.tgbotapi.extensions.api.get.getStickerSetOrNull
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.withTypingAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSticker
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.StickerType
import dev.inmo.tgbotapi.types.message.textsources.CustomEmojiTextSource
import dev.inmo.tgbotapi.types.message.textsources.regularTextSource
import dev.inmo.tgbotapi.types.message.textsources.separateForText
import dev.inmo.tgbotapi.types.stickers.StickerSet
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext

fun StickerSet?.buildInfo() = buildEntities {
    if (this@buildInfo == null) {
        bold("Looks like this stickerset has been removed")
    } else {
        bold("StickerSet name: ") + "${name}\n"
        bold("StickerSet title: ") + "${title}\n"
        bold(
            when (stickerType) {
                StickerType.CustomEmoji -> "Custom emoji"
                StickerType.Mask -> "Mask"
                StickerType.Regular -> "Regular"
                is StickerType.Unknown -> "Unknown type \"${stickerType.type}\""
            }
        ) + " sticker set with title " + bold(title) + " and name " + bold(name.string)
    }
}

suspend fun activateStickerInfoBot(
    token: String,
    print: (Any) -> Unit
) {
    val bot = telegramBot(token)

    print(bot.getMe())

    defaultSafelyWithoutExceptionHandler = {
        it.printStackTrace()
    }

    bot.buildBehaviourWithLongPolling(CoroutineScope(currentCoroutineContext() + SupervisorJob())) {
        onText {
            withTypingAction(it.chat) {
                it.content.textSources.mapNotNull {
                    if (it is CustomEmojiTextSource) {
                        getCustomEmojiStickerOrNull(it.customEmojiId) ?.stickerSetName
                    } else {
                        null
                    }
                }.distinct().map {
                    getStickerSet(it)
                }.distinct().flatMap {
                    it.buildInfo() + regularTextSource("\n")
                }.separateForText().map { entities ->
                    reply(it, entities)
                }
            }
        }
        onSticker {
            val stickerSetInfo = getStickerSetOrNull(it.content.media)
            reply(
                it,
                stickerSetInfo.buildInfo()
            )
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.join()
}
