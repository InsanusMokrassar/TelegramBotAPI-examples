# UserChatShared

A bot that demonstrates the `RequestUsers` and `RequestChat` keyboard button types, letting users
share user or chat contacts with the bot.

## Functionality

On `/start`, the bot sends a reply keyboard containing various request buttons. When the user taps
one of these buttons, Telegram opens a picker and the selected user(s) or chat is shared back with
the bot. The bot then logs and replies with the received share information.

## Arguments

| Position | Value | Sample | Description |
|----------|-------|--------|-------------|
| 1 | `BOT_TOKEN` | `1234567890:AABBccDDeeFF` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Sample | Description |
|-------|--------|-------------|
| `debug` | `debug` | Enable verbose debug logging |
| `testServer` | `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Sends the reply keyboard with all request buttons |

## Capabilities

- `RequestUsers` buttons for: any single user, premium user, non-premium user, multiple users
- `RequestChat` buttons for: any channel, any group, a forum group, a group with specific privacy/creator requirements
- Buttons can optionally request the user's/chat's photo, name, and username
- Unique request IDs are assigned to each button to distinguish responses
- Handles `UserShared` and `ChatShared` service messages and replies with the received data
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
