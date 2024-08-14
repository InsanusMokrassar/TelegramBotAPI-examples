import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.files.downloadFileToTemp
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMedia
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.actions.*
import dev.inmo.tgbotapi.types.media.TelegramMediaAudio
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.media.TelegramMediaVideo
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.utils.filenameFromUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * This bot will download incoming files
 */
suspend fun main(args: Array<String>) {
    val botToken = args.first()
    val directoryOrFile = args.getOrNull(1) ?.let { File(it) } ?: File("/tmp/")
    directoryOrFile.mkdirs()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        onCommand("start") {
            reply(it, "Send me any media (like photo or video) to download it")
        }
        onMedia(initialFilter = null) {
            val content = it.content
            val pathedFile = bot.getFileAdditionalInfo(content.media)
            val outFile = File(directoryOrFile, pathedFile.filePath.filenameFromUrl)
            withTypingAction(it.chat.id) {
                runCatching {
                    bot.downloadFile(content.media, outFile)
                }.onFailure {
                    it.printStackTrace()
                }.onSuccess { _ ->
                    reply(it, "Saved to ${outFile.absolutePath}")
                }
            }.onSuccess { _ ->
                val action = when (content) {
                    is PhotoContent -> UploadPhotoAction
                    is AnimationContent,
                    is VideoContent -> UploadVideoAction
                    is StickerContent -> ChooseStickerAction
                    is MediaGroupContent<*> -> UploadPhotoAction
                    is DocumentContent -> UploadDocumentAction
                    is VoiceContent,
                    is AudioContent -> RecordVoiceAction
                    is VideoNoteContent -> UploadVideoNoteAction
                }
                withAction(it.chat.id, action) {
                    when (content) {
                        is PhotoContent -> replyWithPhoto(
                            it,
                            outFile.asMultipartFile()
                        )
                        is AnimationContent -> replyWithAnimation(
                            it,
                            outFile.asMultipartFile()
                        )
                        is VideoContent -> replyWithVideo(
                            it,
                            outFile.asMultipartFile()
                        )
                        is StickerContent -> replyWithSticker(
                            it,
                            outFile.asMultipartFile()
                        )
                        is MediaGroupContent<*> -> replyWithMediaGroup(
                            it,
                            content.group.map {
                                when (val innerContent = it.content) {
                                    is AudioContent -> TelegramMediaAudio(
                                        downloadFileToTemp(innerContent.media).asMultipartFile()
                                    )
                                    is DocumentContent -> TelegramMediaDocument(
                                        downloadFileToTemp(innerContent.media).asMultipartFile()
                                    )
                                    is PhotoContent -> TelegramMediaPhoto(
                                        downloadFileToTemp(innerContent.media).asMultipartFile()
                                    )
                                    is VideoContent -> TelegramMediaVideo(
                                        downloadFileToTemp(innerContent.media).asMultipartFile()
                                    )
                                }
                            }
                        )
                        is AudioContent -> replyWithAudio(
                            it,
                            outFile.asMultipartFile()
                        )
                        is DocumentContent -> replyWithDocument(
                            it,
                            outFile.asMultipartFile()
                        )
                        is VoiceContent -> replyWithVoice(
                            it,
                            outFile.asMultipartFile()
                        )
                        is VideoNoteContent -> replyWithVideoNote(
                            it,
                            outFile.asMultipartFile()
                        )
                    }
                }
            }
        }
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }.second.join()
}
