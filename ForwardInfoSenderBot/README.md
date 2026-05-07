# ForwardInfoSenderBot

A bot that analyses the origin of forwarded messages and prints detailed information about the forwarder.

## Functionality

For every message that was forwarded to the bot, it inspects the forward metadata and sends back a
formatted reply describing who or what originally sent the message: a regular user, a bot, a channel,
or an anonymous/hidden sender. Premium status, user IDs, and usernames are included where available.

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

- Identifies forwarder type: regular user, bot, public channel, anonymous group admin, or hidden user
- Displays premium user status, numeric IDs, and usernames using `code` and hyperlink entities
- Re-sends the original message content alongside the metadata reply
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
