# LiveLocationsBot

A bot that sends a live location and updates it periodically until the user cancels.

## Functionality

On `/start`, the bot sends a live location message with an inline *Cancel* button. A coroutine then
updates the location every 3 seconds with a slightly changing coordinate. When the user presses
*Cancel* the update loop is stopped and the live location is closed.

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
| `/start` | Sends a live location message and starts the position update loop |

## Capabilities

- Sends an initial live location using `sendLiveLocation`
- Updates the location every 3 seconds via `editLiveLocation` in a background coroutine
- Inline keyboard with a *Cancel* callback button
- Handles the cancel callback to stop the update loop and close the live location
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
