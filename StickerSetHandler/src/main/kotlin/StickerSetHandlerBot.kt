import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.files.downloadFileToTemp
import dev.inmo.tgbotapi.extensions.api.get.getStickerSet
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.stickers.addStickerToSet
import dev.inmo.tgbotapi.extensions.api.stickers.createNewStickerSet
import dev.inmo.tgbotapi.extensions.api.stickers.deleteStickerSet
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSticker
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.sticker
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.requests.stickers.InputSticker
import dev.inmo.tgbotapi.types.StickerSetName
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.files.*
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.botCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Send sticker to this bot to form your own stickers set. Send /delete to delete this sticker set
 */
suspend fun main(args: Array<String>) {
    telegramBotWithBehaviourAndLongPolling(
        args.first(),
        scope = CoroutineScope(Dispatchers.IO),
        defaultExceptionsHandler = {
            it.printStackTrace()
        }
    ) {
        val me = getMe()
        fun Chat.stickerSetName() = StickerSetName("s${id.chatId}_by_${me.username ?.withoutAt}")
        onCommand("start") {
            reply(it) {
                botCommand("delete") + " - to clear stickers"
            }
        }
        onCommand("delete") {
            val deleted = runCatchingSafely {
                deleteStickerSet(it.chat.stickerSetName())
            }.getOrElse { false }

            if (deleted) {
                reply(it, "Deleted")
            } else {
                reply(it, "Can't delete for some of reason")
            }
        }
        onSticker {
            val stickerSetName = it.chat.stickerSetName()
            val sticker = it.content.media
            val newSticker = when (sticker) {
                is CustomEmojiSticker -> InputSticker.WithKeywords.CustomEmoji(
                    downloadFileToTemp(sticker.fileId).asMultipartFile(),
                    sticker.stickerFormat,
                    listOf(sticker.emoji ?: "\uD83D\uDE0A"),
                    emptyList()
                )
                is MaskSticker -> InputSticker.Mask(
                    downloadFileToTemp(sticker.fileId).asMultipartFile(),
                    sticker.stickerFormat,
                    listOf(sticker.emoji ?: "\uD83D\uDE0A"),
                    sticker.maskPosition
                )
                is RegularSticker -> InputSticker.WithKeywords.Regular(
                    downloadFileToTemp(sticker.fileId).asMultipartFile(),
                    sticker.stickerFormat,
                    listOf(sticker.emoji ?: "\uD83D\uDE0A"),
                    emptyList()
                )
                is UnknownSticker -> return@onSticker
            }
            runCatchingSafely {
                getStickerSet(stickerSetName)
            }.onSuccess { stickerSet ->
                runCatching {
                    addStickerToSet(it.chat.id.toChatId(), stickerSet.name, newSticker).also { _ ->
                        reply(
                            it,
                            getStickerSet(stickerSetName).stickers.last()
                        )
                    }
                }.onFailure { exception ->
                    exception.printStackTrace()
                    reply(
                        it,
                        "Unable to add sticker in stickerset"
                    )
                }
            }.onFailure { exception ->
                createNewStickerSet(
                    it.chat.id.toChatId(),
                    stickerSetName.string,
                    "Sticker set by ${me.firstName}",
                    listOf(
                        newSticker
                    ),
                    (sticker as? CustomEmojiSticker) ?.needsRepainting ?: false
                ).also { _ ->
                    reply(
                        it,
                        getStickerSet(stickerSetName).stickers.first()
                    )
                }
            }
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
