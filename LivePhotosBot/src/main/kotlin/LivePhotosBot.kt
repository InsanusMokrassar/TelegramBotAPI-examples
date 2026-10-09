import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.edit.media.editMessageMedia
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.media.sendLivePhoto
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPaidMedia
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.replyWithLivePhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onEditedLivePhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLivePhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLivePhotoGallery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMediaGroupMessages
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPaidMediaInfoContent
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.photoContentOrNull
import dev.inmo.tgbotapi.extensions.utils.photoFileOrNull
import dev.inmo.tgbotapi.extensions.utils.videoContentOrNull
import dev.inmo.tgbotapi.extensions.utils.videoFileOrNull
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.message.content.LivePhotoContent
import dev.inmo.tgbotapi.types.message.payments.PaidMedia
import dev.inmo.tgbotapi.types.media.TelegramMediaLivePhoto
import dev.inmo.tgbotapi.types.media.TelegramPaidMediaLivePhoto
import dev.inmo.tgbotapi.types.media.toTelegramPaidMediaLivePhoto
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Starts the long-polling example for receiving, sending, grouping, editing, and selling Live Photos.
 *
 * @param args bot token followed by the optional, case-sensitive `debug` and `testServer` flags; unknown trailing
 * arguments are ignored
 */
@OptIn(RiskFeature::class)
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
        CoroutineScope(Dispatchers.IO),
        testServer = isTestServer
    ) {

        // Demonstrates: LivePhoto class (LivePhotoFile), live_photo field in Message, sendLivePhoto,
        //               InputMediaLivePhoto (TelegramMediaLivePhoto), InputPaidMediaLivePhoto (TelegramPaidMediaLivePhoto),
        //               editMessageMedia with live photo
        onLivePhoto { message ->
            // message.content is LivePhotoContent — this is the live_photo field of Message
            val content: LivePhotoContent = message.content

            // content.media is LivePhotoFile — the LivePhoto class (photo + short video in one file)
            val livePhotoFile = content.media
            println("=== Live photo received ===")
            println("  fileId:        ${livePhotoFile.fileId}")
            println("  fileUniqueId:  ${livePhotoFile.fileUniqueId}")
            println("  width:         ${livePhotoFile.width}")
            println("  height:        ${livePhotoFile.height}")
            println("  duration:      ${livePhotoFile.duration}s")
            println("  photo (thumb): ${livePhotoFile.photo?.fileId}")
            println("  mimeType:      ${livePhotoFile.mimeType}")
            println("  fileSize:      ${livePhotoFile.fileSize}")
            println("  caption:       ${content.text}")

            // sendLivePhoto: resend the received live photo back using LivePhotoFile overload
            val sent = sendLivePhoto(
                chatId = message.chat.id,
                livePhoto = livePhotoFile,
                text = "Resent via sendLivePhoto"
            )
            println("  sent message id: ${sent.messageId}")

            // Download both Live Photo components once. ktgbotapi 37.0.0 collects the secondary `photo`
            // MultipartFile alongside the main file for edits, media groups, and paid-media requests.
            val livePhotoBytes = downloadFile(livePhotoFile)
            val coverPhotoBytes = livePhotoFile.photo?.let { downloadFile(it) }

            // editMessageMedia with InputMediaLivePhoto (TelegramMediaLivePhoto):
            // edit the previously sent message using newly uploaded main and cover files.
            val sentAsMedia = sent.withContentOrNull<LivePhotoContent>()
            if (sentAsMedia != null) {
                editMessageMedia(
                    message = sentAsMedia,
                    media = TelegramMediaLivePhoto(
                        file = livePhotoBytes.asMultipartFile("edited-live-photo.mp4"),
                        photo = coverPhotoBytes?.asMultipartFile("edited-live-photo-cover.jpg")
                            ?: livePhotoFile.photo?.fileId
                            ?: livePhotoFile.fileId,
                        text = "Edited with newly uploaded Live Photo files"
                    )
                )
            }

            // InputPaidMediaLivePhoto (TelegramPaidMediaLivePhoto): upload both files as paid media (1 star).
            // Telegram currently restricts sendPaidMedia to channel chats.
            sendPaidMedia(
                chatId = message.chat.id,
                starCount = 1,
                media = listOf(
                    TelegramPaidMediaLivePhoto(
                        file = livePhotoBytes.asMultipartFile("paid-live-photo.mp4"),
                        photo = coverPhotoBytes?.asMultipartFile("paid-live-photo-cover.jpg")
                            ?: livePhotoFile.photo?.fileId
                            ?: livePhotoFile.fileId
                    )
                ),
                text = "Paid live photo uploaded as new files (1 star)"
            )
        }

        // Demonstrates: sendMediaGroup with live photos, InputMediaLivePhoto (TelegramMediaLivePhoto)
        onLivePhotoGallery { mediaGroupContent ->
            println("=== Live photo gallery received (${mediaGroupContent.group.size} items) ===")
            mediaGroupContent.group.forEach { groupMember ->
                val livePhotoFile = groupMember.content.media
                println("  - fileId: ${livePhotoFile.fileId}, ${livePhotoFile.width}x${livePhotoFile.height}")
            }

            // sendMediaGroup with newly uploaded main and cover files for every TelegramMediaLivePhoto.
            sendMediaGroup(
                chatId = mediaGroupContent.group.first().sourceMessage.chat.id,
                media = mediaGroupContent.group.mapIndexed { index, groupMember ->
                    val livePhotoFile = groupMember.content.media
                    val coverPhoto = livePhotoFile.photo
                    TelegramMediaLivePhoto(
                        file = downloadFile(livePhotoFile).asMultipartFile("gallery-live-photo-$index.mp4"),
                        photo = coverPhoto?.let {
                            downloadFile(it).asMultipartFile("gallery-live-photo-cover-$index.jpg")
                        } ?: livePhotoFile.fileId
                    )
                }
            )
        }

        // Demonstrates: PaidMediaLivePhoto (PaidMedia.LivePhoto) in received paid media content
        onPaidMediaInfoContent { message ->
            val paidMedia = message.content.paidMediaInfo.media
            val livePhotos = paidMedia.filterIsInstance<PaidMedia.LivePhoto>()
            if (livePhotos.isNotEmpty()) {
                println("=== Paid media with live photos received ===")
                livePhotos.forEach { paidLivePhoto ->
                    // paidLivePhoto is PaidMedia.LivePhoto — PaidMediaLivePhoto class
                    val livePhotoFile = paidLivePhoto.livePhoto
                    println("  - fileId: ${livePhotoFile.fileId}, ${livePhotoFile.width}x${livePhotoFile.height}")
                    println("    duration: ${livePhotoFile.duration}s")
                }
                reply(message, "Received ${livePhotos.size} paid live photo(s)")
            }
        }

        // Demonstrates: live_photo field in edited messages (EditedMessage with LivePhotoContent)
        onEditedLivePhoto { message ->
            println("=== Edited live photo received ===")
            println("  fileId:  ${message.content.media.fileId}")
            println("  caption: ${message.content.text}")
        }

        onMediaGroupMessages {
            val photo = it.content.group.firstNotNullOfOrNull {
                it.content.photoContentOrNull()
            } ?: return@onMediaGroupMessages
            val video = it.content.group.firstNotNullOfOrNull {
                it.content.videoContentOrNull()
            } ?: return@onMediaGroupMessages
            replyWithLivePhoto(
                it,
                video.media.fileId,
                photo.media.fileId
            )
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(scope = this) {
            println(it)
        }
    }.second.join()
}
