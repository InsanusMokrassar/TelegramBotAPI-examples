# DeepLinksBot

A bot that generates and handles Telegram deep links.

## Functionality

Generates a deep link to the bot when the user sends any text message. When a deep link is followed
(i.e., the `/start` command is received with a payload), the bot confirms what payload was received.

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

| Command | Description |
|---------|-------------|
| `/start` | Displays a help/welcome message; also handles deep-link payloads |

## Capabilities

- Requires a registered bot username (validates that `getMe` returns a username)
- Generates a `t.me/<username>?start=<payload>` deep link from any incoming text message
- Subscribes to deep-link follow events with `waitDeepLinks()` and confirms the received payload
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
