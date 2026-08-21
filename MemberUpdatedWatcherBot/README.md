# MemberUpdatedWatcherBot

A long-polling example that watches Telegram `my_chat_member` and `chat_member` updates, logs membership transitions, and posts human-readable notifications in the affected chat. It has no commands and does not respond to ordinary messages.

## Behavior

The bot handles these transitions:

- **Joined:** logs the old and new member-state types and sends `Welcome <first name>`.
- **Left or removed:** logs the transition and sends `Goodbye <first name>`.
- **Promoted:** logs the new administrator title and announces it. A promotion also matches the administrator-permissions-change handler, so it produces a second permissions-change notification.
- **Demoted:** logs the transition and announces that the user was demoted back to member.
- **Administrator permissions/title changed:** logs and sends the old and new member-state types.
- **Newly restricted or restrictions changed:** logs and sends the old and new member-state types. Removing all restrictions is not handled separately.

The bot also identifies updates about itself:

- when added, it asks the chat to grant it administrator permissions;
- when promoted, it confirms that it can now watch other users;
- when demoted, it warns that it can no longer watch other users.

The general handlers do not exclude the bot's own updates. Adding, promoting, or demoting the bot can therefore also produce the corresponding generic welcome, promotion, permissions-change, or demotion messages.

Event details are always written to standard output with the `ChatMemberUpdates` log tag. Debug mode additionally routes the library's default KSLog output to standard output.

## Telegram setup, permissions, and privacy

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Add it to the group or supergroup that should be watched.
3. Promote it to administrator. Telegram exposes updates about the bot's own membership without this step, but delivers `chat_member` updates about other users only to administrators. The long-polling setup requests that update type.
4. Ensure the bot can send messages in the chat. If using the example in a channel, it also needs permission to post messages.

BotFather privacy mode may remain enabled: privacy mode controls which messages a bot receives in groups, not member-status updates. The bot does not need access to ordinary group messages.

The example uses long polling and automatically removes an existing webhook at startup. Run only one update consumer for the bot token at a time.

## Launch

From the repository root, pass the bot token as the first application argument:

```bash
./gradlew :MemberUpdatedWatcherBot:run --args="<BOT_TOKEN>"
```

Add an argument exactly equal to `debug` after the token to enable formatted library logging. Other additional arguments are ignored.

```bash
./gradlew :MemberUpdatedWatcherBot:run --args="<BOT_TOKEN> debug"
```
