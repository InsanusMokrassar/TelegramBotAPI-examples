import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.files.downloadFileToTemp
import dev.inmo.tgbotapi.extensions.api.get.getStickerSet
import dev.inmo.tgbotapi.extensions.api.stickers.createNewStickerSet
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSticker
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.sticker
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.requests.stickers.InputSticker
import dev.inmo.tgbotapi.types.files.*
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

suspend fun main(args: Array<String>) {
    telegramBotWithBehaviourAndLongPolling(args.first(), scope = CoroutineScope(Dispatchers.IO)) {
        val me = getMe()
        onSticker {
            val stickerSetName = "${it.chat.id}_by_${me.username.username}"
            val sticker = it.content.media
            runCatchingSafely {
                getStickerSet(stickerSetName)
            }.getOrElse { _ ->
                createNewStickerSet(
                    it.chat.id.toChatId(),
                    stickerSetName,
                    "Sticker set by ${me.firstName}",
                    it.content.media.stickerFormat,
                    listOf(
                        when (sticker) {
                            is CustomEmojiSticker -> InputSticker.WithKeywords.CustomEmoji(
                                downloadFileToTemp(sticker.fileId).asMultipartFile(),
                                sticker.emoji ?.let(::listOf) ?: emptyList(),
                                emptyList()
                            )
                            is MaskSticker -> TODO()
                            is RegularSticker -> TODO()
                            is UnknownSticker -> TODO()
                        }
                    ),
                    sticker
                )
            }
        }
    }
}