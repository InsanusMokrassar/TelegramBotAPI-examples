# ChatAvatarSetter

This Kotlin/JVM example changes a chat's avatar to the photo sent in that chat. It receives updates by long
polling and demonstrates the Telegram Bot API [`setChatPhoto`](https://core.telegram.org/bots/api#setchatphoto)
method with a multipart file upload.

## Behavior

- There is no command: every photo message received by the bot is a trigger.
- The bot downloads the photo, uploads it as `sample.jpg`, and uses it as the avatar of the same chat.
- After a successful update, it replies `Done`. If `setChatPhoto` fails, it logs the exception and replies
  `Something went wrong (see logs)`.
- Non-photo messages are ignored. In particular, an image sent as a document is not a photo message.

The implementation does not restrict who may trigger it. In a group or supergroup, any member whose photo
message reaches the bot can cause an avatar-change attempt.

## Telegram setup and permissions

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Add the bot to the group, supergroup, or channel whose avatar it should manage.
3. Promote the bot to administrator and grant it the **Change chat info** permission
   (`can_change_info`). It must also be able to send messages in a group or posts in a channel to deliver its
   status reply.
4. Send a photo directly in that chat.

Telegram does not allow `setChatPhoto` to change private-chat photos. The target chat is always taken from the
incoming photo message, so a photo sent to the bot privately cannot be used to update another chat.

## Arguments

| Position | Argument | Required | Description |
| --- | --- | --- | --- |
| 1 | `BOT_TOKEN` | Yes | Bot API token issued by BotFather. |

## Launch

From the repository root, run:

```bash
./gradlew :ChatAvatarSetter:run --args="<BOT_TOKEN>"
```

The process continues polling until it is stopped.
