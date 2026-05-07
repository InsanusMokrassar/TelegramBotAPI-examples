# ChatAvatarSetter

A bot that updates a group or channel's avatar using a photo sent to the bot.

## Functionality

When the bot receives a photo message, it downloads the highest-resolution version of the photo
and sets it as the chat photo for the chat the message was sent from. If the operation fails (e.g.,
due to missing admin rights), the bot sends an error message back to the user.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

None.

## Capabilities

- Downloads the largest available photo size from the incoming message
- Calls `setChatPhoto` to apply the downloaded image as the chat's avatar
- Returns a user-facing error message if the update fails
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```

> **Note:** The bot must be an administrator with *Change group info* permission in the target chat.
