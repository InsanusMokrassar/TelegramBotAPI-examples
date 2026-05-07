# ResenderBot

A multiplatform bot (JVM + Native + JS) that echoes every content message back to the sender.

## Functionality

For every content message the bot receives, it immediately re-sends the same content back to the
originating chat. Reply quotes and message effects are preserved. The bot is implemented as a
shared library (`ResenderBotLib`) with separate launcher modules for JVM and Native targets.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |

Optional flags (any order):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

None.

## Capabilities

- Re-sends any content message (text, photo, video, audio, document, sticker, etc.)
- Preserves `reply_to_message` quote when the original message was a reply
- Preserves `effect_id` (message effects / animations)
- Shared `commonMain` implementation across JVM, Native, and JS targets
- Runs via long polling

## Launch

### JVM

```bash
./gradlew :ResenderBot:jvm_launcher:run --args="BOT_TOKEN"
```

### Native (after build)

```bash
./ResenderBot/native_launcher/build/bin/native/releaseExecutable/native_launcher.kexe BOT_TOKEN
```
