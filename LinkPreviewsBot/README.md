# LinkPreviewsBot

A bot that demonstrates all `LinkPreviewOptions` variants by replying with multiple messages, each
using a different link preview style.

## Functionality

When the user sends a message containing a URL, the bot extracts the URL and sends several reply
messages, each with a different `LinkPreviewOptions` configuration: disabled, small preview above
text, large preview above text, small preview below text, large preview below text, and the default
(no explicit options).

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

- Extracts URLs from the text entities of incoming messages
- Sends one reply per `LinkPreviewOptions` variant:
  - Preview disabled
  - Small image, positioned above text
  - Large image, positioned above text
  - Small image, positioned below text
  - Large image, positioned below text
  - Default (Telegram-chosen behaviour)
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
