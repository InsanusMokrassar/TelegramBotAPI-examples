# BoostsInfoBot

A bot that retrieves and displays the boost information for a chat.

## Functionality

On `/start`, the bot sends a reply keyboard with a *Request Channel* button. When the user selects
a channel, the bot calls `getUserChatBoosts` and replies with a formatted list of all active boosts
for that user in the selected chat, including the start and expiration dates of each boost.

## Arguments

| Position | Value | Sample | Description |
|----------|-------|--------|-------------|
| 1 | `BOT_TOKEN` | `1234567890:AABBccDDeeFF` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Sends the channel-request keyboard |

## Capabilities

- Reply keyboard with a `RequestChat` button configured for channels
- Retrieves user boost list via `getUserChatBoosts`
- Formats each boost with its add date and expiration date
- Handles `ChatShared` service messages to extract the target chat ID
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
