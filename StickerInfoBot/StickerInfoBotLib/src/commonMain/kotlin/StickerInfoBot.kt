import dev.inmo.micro_utils.coroutines.defaultSafelyWithoutExceptionHandler
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.get.*
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.types.StickerFormat
import dev.inmo.tgbotapi.types.StickerType
import dev.inmo.tgbotapi.types.message.textsources.*
import dev.inmo.tgbotapi.types.stickers.AnimatedStickerSet
import dev.inmo.tgbotapi.types.stickers.CustomEmojiSimpleStickerSet
import dev.inmo.tgbotapi.types.stickers.CustomEmojiStickerSet
import dev.inmo.tgbotapi.types.stickers.CustomEmojiVideoStickerSet
import dev.inmo.tgbotapi.types.stickers.MaskSimpleStickerSet
import dev.inmo.tgbotapi.types.stickers.MaskStickerSet
import dev.inmo.tgbotapi.types.stickers.MaskVideoStickerSet
import dev.inmo.tgbotapi.types.stickers.RegularSimpleStickerSet
import dev.inmo.tgbotapi.types.stickers.RegularStickerSet
import dev.inmo.tgbotapi.types.stickers.RegularVideoStickerSet
import dev.inmo.tgbotapi.types.stickers.StickerSet
import dev.inmo.tgbotapi.types.stickers.UnknownStickerSet
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.*

fun StickerSet?.buildInfo() = buildEntities {
    if (this@buildInfo == null) {
        bold("Looks like this stickerset has been removed")
    } else {
        bold("StickerSet name: ") + "${name}\n"
        bold("StickerSet title: ") + "${title}\n"
        bold("Sticker format: ") + when (stickerFormat) {
            StickerFormat.Animated -> "Animated"
            StickerFormat.Static -> "Static"
            is StickerFormat.Unknown -> stickerFormat.type
            StickerFormat.Video -> "Video"
        } + "\n"
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
                    it.buildInfo() + regular("\n")
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
