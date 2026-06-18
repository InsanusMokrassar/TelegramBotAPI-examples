# MyBot

A bot that monitors messages containing its username and responds with a contextual link or mention.

## Functionality

Watches every incoming message for a mention of the bot's username. When found, it replies with a
MarkdownV2-formatted message that includes a link or text mention pointing back to the originating
chat or user, adapting the reply to the type of chat the message came from.

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

None. The bot reacts to username mentions, not to commands.

## Capabilities

- Handles all chat types: private, group, supergroup, channel, business connection chats, channel groups
- For public chats builds a `t.me/<username>` hyperlink; for private chats uses an inline text mention
- Prints information about the originating chat using `getChat`
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
