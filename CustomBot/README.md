# CustomBot

A bot that demonstrates custom middleware, custom subcontext data, and several utility features
of the TelegramBotAPI library.

## Functionality

Shows how to attach a logging middleware to every API request and how to store arbitrary data in
a per-update subcontext. Additionally demonstrates retrieving and sending a user's profile audio
playlist and querying the bot's own star balance.

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
| `/start` | Retrieves the sender's profile audio files and sends them as an audio media group |
| `/additional_command` | Demo command that accesses and prints custom subcontext data |
| `/getMyStarBalance` | Queries and replies with the bot's current Telegram Star balance |

## Capabilities

- Custom request middleware that logs every outgoing API call
- Custom `BehaviourContext` subcontext with arbitrary stored data
- Profile audio retrieval via `getUserProfilePhotos`-style API for audio
- Audio media group sending (batched uploads)
- Star balance query via `getStarTransactions`
- Channel direct-message configuration tracking via `ChatBoostUpdated` events
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
