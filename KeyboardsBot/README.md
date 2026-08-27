# KeyboardsBot

A multiplatform long-polling example that demonstrates Telegram reply keyboards, inline keyboards, callback queries, copy-text buttons, inline-mode buttons, and keyboard button styles. The shared bot behavior lives in `KeyboardsBotLib`; the project provides a browser/JS entry point and a separate JVM launcher.

## Bot behavior

At startup, the bot calls `getMe`, reports the returned bot information through the platform launcher, registers `/inline` with Telegram, and starts long polling. Every received update is also printed to the JVM terminal or browser developer console.

### Commands

| Command | Result |
| --- | --- |
| `/inline` | Opens page `1` of a `10`-page inline keyboard. |
| `/inline <count>` | Opens page `1` with the supplied total page count. |
| `/inline <page> <count>` | Opens the supplied page with the supplied total page count. |

Only numeric command arguments are considered. Use positive integers with `page <= count`; the example does not validate the count or clamp the page to the upper bound.

The generated inline keyboard contains:

- numbered buttons for the current page and any adjacent pages that are within `1..count`;
- styled jump buttons for moving toward the first or last page when applicable;
- a **Command copy button** that copies `/inline <page> <count>`;
- a **Send somebody page** button that starts inline mode and lets the user choose a user, bot, group, or channel.

Pagination callbacks edit the original message and replace its text with `This is <page> of <count>`. This works for both ordinary bot messages and messages sent through inline mode. Unsupported callback data or an unsupported message type is answered with a callback notification instead.

Any command not handled above, including `/start`, receives a one-time reply keyboard containing a styled `/inline` button. Ordinary non-command messages are ignored.

### Inline mode

With inline mode enabled, a query beginning with a page and count, such as `@YourBot 2 10`, returns one **Send buttons** article. Sending that result posts an inline-mode message with the same pagination keyboard.

## Telegram setup

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Enable inline mode for the bot with BotFather's `/setinline` command. Direct `/inline` commands work without it, but inline queries and **Send somebody page** require it.
3. Run only one launcher for a token at a time. The bot receives updates through long polling and automatically removes an existing webhook when it starts.

The browser launcher handles the token in client-side code. Use it only from a trusted local page, do not expose the page publicly with a token filled in, and close the page when the bot should stop.

## Launch

Run the commands below from the repository root.

### JVM

The first argument is the required bot token. An optional argument exactly equal to `debug` enables formatted KSLog output; the token must remain first.

```bash
./gradlew :KeyboardsBot:jvm_launcher:run --args="<BOT_TOKEN>"
```

```bash
./gradlew :KeyboardsBot:jvm_launcher:run --args="<BOT_TOKEN> debug"
```

### Browser/JS

Start the Kotlin/JS browser development run:

```bash
./gradlew :KeyboardsBot:KeyboardsBotLib:jsBrowserDevelopmentRun
```

Enter the bot token in the displayed form and press **Start bot**. The page displays the result of `getMe`; raw updates and other console output appear in the browser developer console. Each form submission starts another bot instance, so submit the token only once.
