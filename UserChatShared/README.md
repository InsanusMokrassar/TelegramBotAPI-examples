# UserChatShared

A long-polling example of Telegram reply-keyboard buttons that request users, bots, groups, forums, or channels. It
also shows how the resulting `users_shared` and `chat_shared` service messages can be handled.

## Behavior

1. Open a private chat with the bot and send `/start` (the command takes no arguments).
2. The bot sends a persistent, resized reply keyboard. Pressing a request button opens Telegram's native peer
   picker with that button's filters.
3. After you confirm a selection, Telegram sends a `users_shared` or `chat_shared` service message containing the
   request ID and selected peer data. These buttons do not produce callback queries or callback data.
4. The bot uses the request ID to describe the selection, calls `getChat` as a best-effort lookup, and replies with
   the identifier and lookup result. A failed lookup is shown as `null` rather than stopping the bot.

The user/bot part of the keyboard provides:

- one user or bot;
- one non-Premium user, any user, one Premium user, or one bot;
- multiple users or bots; and
- multiple non-Premium users, any users, Premium users, or bots, up to the library's current
  `keyboardButtonRequestUserLimit` maximum.

Every user/bot button asks Telegram to include the selected peer's name, username, and photo. The handler replies
once per selected ID. Its descriptive labels cover the single-selection request IDs; selections from the
multiple-selection buttons use the fallback label `somebody O.o`.

The chat part provides an unfiltered chat request plus these filtered requests:

| Kind | Available filters |
| --- | --- |
| Channel | any, public, private, or owned by the selecting user |
| Group | any, public, private, or owned by the selecting user |
| Forum group | any, public, private, or owned by the selecting user |

Here, public/private means with/without a public username. Every chat button asks Telegram to include the title,
username, and photo. This example uses only the ID from the shared event for its `getChat` lookup and response; it
does not print the requested snapshot fields directly.

## Telegram setup and permissions

- Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
- Send `/start` in a private chat. The command handler intentionally ignores `/start` in groups and channels, and
  Telegram exposes user/chat request buttons only in private chats.
- The buttons do not require the bot to be a member or administrator of a selected chat, and they request no user
  or bot administrator rights. Consequently, sharing a peer does not guarantee that `getChat` can access it. Add
  the bot to a selected group or channel when you want that lookup to succeed reliably.
- The example uses long polling. Do not run another polling or webhook consumer with the same bot token at the same
  time.

## Arguments

The first application argument is the required bot token. If the optional second argument is exactly `debug`, the
example formats and prints the library's default KSLog output to standard output. Other extra arguments are ignored.

## Launch

From the repository root:

```bash
./gradlew :UserChatShared:run --args="<BOT_TOKEN>"
```

To enable debug logging:

```bash
./gradlew :UserChatShared:run --args="<BOT_TOKEN> debug"
```
