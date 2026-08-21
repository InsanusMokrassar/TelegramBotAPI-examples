# StickerInfoBot

A multiplatform long-polling example that looks up Telegram sticker-set metadata. `StickerInfoBotLib` contains the shared JVM/JS behavior, with a browser entry point in `jsMain`; `jvm_launcher` provides the command-line entry point. The bot has no commands.

## Behavior and output

At startup, the bot calls `getMe` and reports the returned bot information through the active launcher. It then handles:

- **Sticker messages:** looks up the sticker's set and replies to the message with the set name, title, and type (`Regular`, `Mask`, `Custom emoji`, or the raw unknown type). If no set can be resolved, it replies with **Looks like this stickerset has been removed**.
- **Text messages:** shows a typing action, scans the message entities for custom emoji, resolves their stickers and sticker sets, removes duplicate sets, and replies with the same metadata for each set. Sets without a resolvable name are skipped. Text without resolvable custom emoji produces no reply.

Long output from text containing several custom-emoji sets is split into Telegram-sized messages. Every received update is printed to the JVM terminal or browser developer console, and uncaught processing errors print stack traces.

## Telegram setup and permissions

- Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
- No administrator rights are required in a private chat. The bot only needs to receive the source message and be allowed to send replies.
- To inspect arbitrary sticker and custom-emoji messages in a group, either disable privacy mode with BotFather's `/setprivacy` or promote the bot to administrator. With privacy mode enabled as a regular member, Telegram does not deliver most ordinary group messages to it.
- The example uses long polling and automatically removes an existing webhook at startup. Run only one launcher for a token at a time.

## Launchers

Run these commands from the repository root.

### JVM

The required first argument is the bot token. Additional arguments are ignored.

```bash
./gradlew :StickerInfoBot:jvm_launcher:run --args="<BOT_TOKEN>"
```

> **Known issue:** `jvm_launcher/build.gradle` declares `StickerInfoBotJvmKt`, while the current launcher filename produces `StickerInfoBotBotJvmKt`. The `run` task cannot start until that main-class setting is corrected; it is left unchanged by this documentation-only update.

The JVM launcher prints the startup `getMe` result, raw updates, and errors to standard output.

### Browser/JS

Start the Kotlin/JS browser development run:

```bash
./gradlew :StickerInfoBot:StickerInfoBotLib:jsBrowserDevelopmentRun
```

Enter the token in the bundled form and press **Start bot**. The page renders the startup `getMe` result; raw updates and errors appear in the browser developer console. The bot runs only while the page remains active, and every form submission starts another polling instance.

The browser handles the bot token in client-side code. Use this target only from a trusted local page and do not expose a token through a publicly hosted copy.
