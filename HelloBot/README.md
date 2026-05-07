# HelloBot

A minimal bot that responds whenever someone mentions the bot's username in a chat.

## Functionality

Listens for any message that contains the bot's username mention. When triggered, replies with
`Oh, hi, ` followed by a mention of the sender (or the group/channel name for non-private chats).
Uses MarkdownV2 formatting and adapts the reply text based on the chat type.

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

None. The bot is triggered by username mentions, not commands.

## Capabilities

- Detects mentions of the bot username in all chat types (private, group, supergroup, channel, business)
- Builds a MarkdownV2-formatted reply that links back to the sender
- For public chats the reply contains a clickable mention link; for private chats it uses a text mention
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
