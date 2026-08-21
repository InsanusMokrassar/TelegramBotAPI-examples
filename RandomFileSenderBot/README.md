# RandomFileSenderBot

This Kotlin Multiplatform example sends randomly selected local files in response to a Telegram command. It uses long
polling and can run on the JVM or as a Kotlin/Native executable.

## Behavior

The bot registers one command:

- `/send_file` requests one file;
- `/send_file N` requests `N` files when `N` is a positive integer; and
- a missing or non-numeric count defaults to one. Zero and negative counts select nothing and receive
  `Nothing selected :(`.

For each requested file, the picker starts at the configured root. A file root is selected directly; a directory root
is searched by choosing one random child at each level until a file is reached. This is not a uniform choice among all
files in an uneven directory tree, and the same file may be selected more than once. Zero-byte files and unsuccessful
selections are retried. Consequently, a positive request can keep retrying indefinitely when no non-empty file is
reachable.

One file is sent as a document. Multiple files are sent as document media groups, split at Telegram's maximum media
group size. All sends enable Telegram's protected-content flag. The bot also prints its own account information at
startup and prints polling exceptions.

## Setup and security

Create a bot with BotFather and obtain its token. Give the process read access to a dedicated directory containing only
files that every bot user may receive, and pass that directory explicitly. The bot has no user or chat allowlist and no
file-name or file-type filter; anyone able to send it the command can request files reachable through the configured
tree. Protected content is not an access-control mechanism.

Keep the token private. These launchers accept it on the command line, where it may be retained in shell history or be
visible to other local processes. Also avoid roots containing secrets or links to locations outside the intended tree.

## Arguments

Both launchers interpret arguments in the same order:

1. `BOT_TOKEN` (required). Omitting it fails immediately.
2. `ROOT_PATH` (optional in code), either a file or directory. Relative paths are resolved from the process working
   directory; use an explicit absolute path for predictable behavior. The launchers pass an empty path when this
   argument is omitted, whose filesystem behavior differs by platform and is not a reliable working-directory default.

Additional arguments are ignored.

## Launch from the repository root

### JVM

```bash
./gradlew :RandomFileSenderBot:runJvm --args="<BOT_TOKEN> /absolute/path/to/files"
```

The JVM picker uses `java.io.File`. A missing, empty, or unreadable directory produces no selection and therefore causes
a positive request to keep retrying.

### Kotlin/Native

The shared native configuration selects Linux x64, Linux Arm64, or Windows x64 for the current host. macOS is not
configured. Link the debug executable with Gradle, then pass the arguments directly to the generated program:

```bash
./gradlew :RandomFileSenderBot:linkDebugExecutableNative
./RandomFileSenderBot/build/bin/native/debugExecutable/RandomFileSenderBot.kexe "<BOT_TOKEN>" "/absolute/path/to/files"
```

On Windows, run
`RandomFileSenderBot\build\bin\native\debugExecutable\RandomFileSenderBot.exe "<BOT_TOKEN>" "C:\path\to\files"`
after the same Gradle link task. The native picker uses Okio; unlike the JVM picker, inaccessible or invalid paths may
raise a filesystem exception that is printed by the polling exception handler.

## Source sets

- `commonMain` contains the picker contract and the long-polling bot behavior.
- `jvmMain` implements recursive selection with `java.io.File` and provides the suspending JVM entry point.
- `nativeMain` implements recursive selection with Okio and provides a `runBlocking` native entry point.
