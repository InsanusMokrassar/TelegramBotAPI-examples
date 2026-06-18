# RandomFileSenderBot

A multiplatform bot (JVM + Native) that picks random files from a directory and sends them to the
requester.

## Functionality

Picks one or more files at random from a specified directory and sends them to the user. Multiple
files are batched into a media group. Files are sent as protected content. The bot is implemented
as a shared library with separate JVM and Native launcher entry points.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |
| 2 *(optional)* | `/path/to/dir` | Directory to pick files from (defaults to the current working directory) |

Optional flags (any order):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/send_file` | Send 1 random file from the configured directory |
| `/send_file N` | Send *N* random files from the configured directory |

## Capabilities

- Platform-specific random file selection (JVM uses `java.io.File`, Native uses POSIX directory API)
- Groups multiple files into a single media group message when N > 1
- Files are sent as protected content (forwarding disabled)
- Multiplatform: shared logic in `commonMain`, launchers in `jvm_launcher` and `native_launcher`
- Runs via long polling

## Launch

### JVM

```bash
./gradlew :RandomFileSenderBot:jvm_launcher:run --args="BOT_TOKEN /optional/path"
```

### Native (after build)

```bash
./RandomFileSenderBot/native_launcher/build/bin/native/releaseExecutable/native_launcher.kexe BOT_TOKEN /optional/path
```
