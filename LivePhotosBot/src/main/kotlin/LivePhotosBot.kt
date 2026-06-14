import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.edit.media.editMessageMedia
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
 * This bot demonstrates Live Photos support introduced in Telegram Bot API.
 *
 * Key concepts demonstrated:
 * - [dev.inmo.tgbotapi.types.files.LivePhotoFile] — the LivePhoto class: a photo with an attached short video
 * - [TelegramMediaLivePhoto] — InputMediaLivePhoto: used in sendMediaGroup and editMessageMedia
 * - [LivePhotoContent] — the content type carried in Message.live_photo / ExternalReplyInfo.live_photo
 * - [sendLivePhoto] — method to send a live photo
 * - [PaidMedia.LivePhoto] — PaidMediaLivePhoto: a live photo inside paid media content
 * - [TelegramPaidMediaLivePhoto] — InputPaidMediaLivePhoto: used in sendPaidMedia
 * - sendMediaGroup and editMessageMedia with live photos
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

            // InputPaidMediaLivePhoto (TelegramPaidMediaLivePhoto): send the live photo as paid media (1 star)
            sendPaidMedia(
                chatId = message.chat.id,
                starCount = 1,
                media = listOf(
                    // TelegramPaidMediaLivePhoto is InputPaidMediaLivePhoto
                    TelegramPaidMediaLivePhoto(
                        file = livePhotoFile.fileId,
                        photo = livePhotoFile.photo?.fileId ?: livePhotoFile.fileId
                    )
                ),
                text = "Paid live photo (1 star)"
            )

            // editMessageMedia with InputMediaLivePhoto (TelegramMediaLivePhoto):
            // edit the previously sent message to replace it with itself via TelegramMediaLivePhoto
            val sentAsMedia = sent.withContentOrNull<LivePhotoContent>()
            if (sentAsMedia != null) {
                editMessageMedia(
                    message = sentAsMedia,
                    // TelegramMediaLivePhoto is InputMediaLivePhoto
                    media = TelegramMediaLivePhoto(
                        file = livePhotoFile.fileId,
                        photo = livePhotoFile.photo?.fileId ?: livePhotoFile.fileId,
                        text = "Edited via editMessageMedia with TelegramMediaLivePhoto"
                    )
                )
            }
        }

        // Demonstrates: sendMediaGroup with live photos, InputMediaLivePhoto (TelegramMediaLivePhoto)
        onLivePhotoGallery { mediaGroupContent ->
            println("=== Live photo gallery received (${mediaGroupContent.group.size} items) ===")
            mediaGroupContent.group.forEach { groupMember ->
                val livePhotoFile = groupMember.content.media
                println("  - fileId: ${livePhotoFile.fileId}, ${livePhotoFile.width}x${livePhotoFile.height}")
            }

            // sendMediaGroup with TelegramMediaLivePhoto (InputMediaLivePhoto)
            sendMediaGroup(
                chatId = mediaGroupContent.group.first().sourceMessage.chat.id,
                media = mediaGroupContent.group.map { groupMember ->
                    val livePhotoFile = groupMember.content.media
                    // TelegramMediaLivePhoto is InputMediaLivePhoto — used here in sendMediaGroup
                    TelegramMediaLivePhoto(
                        file = livePhotoFile.fileId,
                        photo = livePhotoFile.photo?.fileId ?: livePhotoFile.fileId
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
