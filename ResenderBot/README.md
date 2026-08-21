# ResenderBot

A multiplatform long-polling example that recreates received content in the same
chat. Shared behavior lives in `ResenderBotLib`; JVM, browser/JS, and native entry
points start it in platform-specific ways.

## Resend behavior

The bot defines no commands. For each content message, it shows a typing action and
uses `createResend` to send equivalent content back to the originating chat. When
present, it carries over the replied-to message metadata, text quote entities and
position, and the message effect ID.

Business content sent by the business-connection owner is ignored; other delivered
content messages are eligible. Processing is separated by chat. The bot prints each
received update and each resend result, and reports its `getMe` result through the
launcher's output callback.

## Source sets and launchers

- `ResenderBotLib/commonMain` provides the public `activateResenderBot` function and
  all Telegram handlers for JVM, JS, and native consumers.
- `ResenderBotLib/jsMain` provides a browser form. Each submission starts another
  bot and displays callback output in its own page element; console output remains
  in the browser developer tools.
- `jvm_launcher` provides a suspending CLI entry point and optional debug logging.
- `native_launcher` wraps the shared suspending function in `runBlocking`. Its
  shared native template selects Linux x64/Arm64 or Windows x64 for the host.

## Setup and permissions

Create a bot, obtain its token, and keep it private. Start a private chat with the
bot or add it to a chat where Telegram will deliver the desired content. The bot
must be allowed to send each content type it should reproduce; no administrator-only
methods are used. Long polling is used, so run only one launcher per token.

The browser form handles the token in client-side code and uses a plain text input.
Use it only on a trusted local page, submit once, and close the page when finished.
Linux native builds also require the repository's documented libcurl dependency.

## Run from the repository root

### JVM

The first argument is the required token. `debug` is recognized only as the second
argument; later arguments are ignored.

```bash
./gradlew :ResenderBot:jvm_launcher:run --args="BOT_TOKEN"
./gradlew :ResenderBot:jvm_launcher:run --args="BOT_TOKEN debug"
```

### Browser/JS

```bash
./gradlew :ResenderBot:ResenderBotLib:jsBrowserDevelopmentRun
```

Enter the token in the page form and select **Start bot**. There are no browser
command-line arguments.

### Kotlin/Native

Build the host-specific debug executable, then pass the required token directly to
it. Additional native arguments are ignored.

```bash
./gradlew :ResenderBot:native_launcher:linkDebugExecutableNative
./ResenderBot/native_launcher/build/bin/native/debugExecutable/native_launcher.kexe "BOT_TOKEN"
```

On Windows, run
`ResenderBot\\native_launcher\\build\\bin\\native\\debugExecutable\\native_launcher.exe "BOT_TOKEN"`
after the same Gradle link task. macOS hosts are not configured by the native
launcher template.
