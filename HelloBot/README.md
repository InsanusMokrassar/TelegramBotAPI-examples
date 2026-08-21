# HelloBot

HelloBot is a small long-polling example that greets the chat or sender when a
message addresses the bot by username.

## Trigger and replies

There are no slash commands. The bot handles a content message only when its
text contains the bot's full username. This is a case-sensitive substring
check; messages without text or without the username are ignored.

- In a private chat, it replies with a MarkdownV2 text mention of the user.
- In a group or supergroup, it greets the group and links its title to a public
  username or invite link when one is available.
- For a message sent to a group on behalf of a channel, it greets the sender
  channel instead.
- In a channel, it greets the channel and includes the sender chat when Telegram
  supplies one.
- In a business chat, it mentions the underlying private-chat user.

Every received update is also printed to standard output for demonstration and
debugging.

## Setup

1. Create a bot with BotFather and keep its token private.
2. Add the bot to each chat where it should respond. Explicit username mentions
   work with Telegram's normal group privacy mode.
3. For channel posts, make the bot a channel administrator and allow it to post
   messages. Admin access may also make a private group invite link available;
   otherwise the group reply falls back to an unlinked title.

## Run

From the repository root, pass the token as the first positional argument:

```bash
./gradlew :HelloBot:run --args="BOT_TOKEN"
```

The token is required. Additional command-line arguments are ignored. The bot
runs until the process is stopped and uses long polling, so no webhook setup is
needed.
