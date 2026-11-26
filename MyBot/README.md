# MyBot

A long-polling example that prints information about the bot and lets Telegram users replace or remove its global profile photo.

## Behavior

At startup, the application prints the `BotFullInfo` returned by `getMe`, including active usernames and bot settings.
It passes that object's `id` to `getChat` to print the bot's chat profile. It then handles two commands:

- `/setMyProfilePhoto` replies with `ok, send me new photo` and waits for the first photo sent in the same chat. It streams draft progress messages while downloading the photo to a temporary file, uploads it as a static bot profile photo, and replies when the change is complete. The photo may come from any user in that chat; it is not restricted to the user who sent the command.
- `/removeMyProfilePhoto` removes the bot's current profile photo and confirms success. On failure, it prints the exception and sends a generic error reply.

There is no `/start` handler, and ordinary messages are ignored unless the bot is waiting for a photo after `/setMyProfilePhoto`.

## Setup and permissions

- Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
- No chat-administrator permission is required to change the bot's own profile photo. In a group, the bot still needs permission to send replies.
- Prefer using this example in a private chat. Telegram's draft-message API is intended for private chats, and BotFather privacy mode can prevent an unrelated group photo from reaching the bot unless it is sent as a reply or privacy mode is disabled.
- This example performs no authorization checks: anyone who can reach the commands can change or remove the bot's profile photo globally. Do not expose a production bot without adding access control.
- Long polling automatically removes an existing webhook at startup. Run only one update consumer for the token at a time.

## Arguments

The first argument is always the required bot token. Optional arguments can follow it in any order:

- `debug` enables formatted default KSLog output.
- `testServer` runs the startup diagnostics and polling against Telegram's Bot API test environment.

Other arguments are ignored. Argument matching is case-sensitive.

## Launch

Run from the repository root:

```bash
./gradlew :MyBot:run --args="<BOT_TOKEN>"
```

For example, to enable both optional modes:

```bash
./gradlew :MyBot:run --args="<BOT_TOKEN> debug testServer"
```
