# InlineQueriesBot

This Kotlin Multiplatform example answers inline queries with generated article results. It uses long polling and can be
launched on the JVM or as a Kotlin/Native executable.

## Behavior

For every inline query, the bot:

- treats the numeric `offset` as a page number, defaulting to page `0` when the offset is absent or invalid;
- returns a full page of numbered articles whose message text includes the user's query;
- disables caching and marks the answer as personal;
- provides the next numeric offset for pagination; and
- adds a button that opens a `/start` deep link for the current page. The bot replies with that deep-link parameter.

The bot also prints its own account information at startup, logs received updates, and prints polling exceptions.

## Setup

Create a bot and obtain its token. To enable inline mode and set the search placeholder through `setMySettings`,
pass `configureInline` after the token on either launcher. Telegram saves both settings, so subsequent launches only
need the token. You can also enable inline mode through BotFather.

## Launch from the repository root

### JVM

```bash
./gradlew :InlineQueriesBot:runJvm --args="<BOT_TOKEN>"
```

To configure inline mode on the first run:

```bash
./gradlew :InlineQueriesBot:runJvm --args="<BOT_TOKEN> configureInline"
```

### Kotlin/Native

The shared native configuration supports Linux x64/Arm64 and Windows x64 hosts. Build the debug executable with Gradle,
then pass the token directly to the produced program:

```bash
./gradlew :InlineQueriesBot:linkDebugExecutableNative
./InlineQueriesBot/build/bin/native/debugExecutable/InlineQueriesBot.kexe "<BOT_TOKEN>"
```

On Windows, run `InlineQueriesBot\build\bin\native\debugExecutable\InlineQueriesBot.exe "<BOT_TOKEN>"` after the same
Gradle link task.

## Source sets

- `commonMain` contains `doInlineQueriesBot`, including the long-polling behavior and inline-query/deep-link handlers.
- `jvmMain` provides the suspending JVM entry point.
- `nativeMain` provides the native entry point and calls the shared suspending function with `runBlocking`.
