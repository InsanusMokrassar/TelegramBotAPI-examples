# ChatManagementBot

This long-polling example demonstrates chat-management features introduced in Telegram Bot API 10.0. It inspects a member's `can_react_to_messages` permission, includes other bot administrators in an administrator query, deletes reactions, and logs content messages received from other bots.

At startup, the bot prints its name, username, and `canReadAllGroupMessages` value returned by `getMe`. The latter indicates whether Group Privacy Mode is disabled; it does not enable bot-to-bot communication by itself.

## Commands and triggers

| Command or trigger | Behavior |
| --- | --- |
| A member becomes restricted or their restrictions change | Prints the member's new `canReactToMessages` value twice: directly from the restricted member and through the `ChatPermissions` interface. |
| `/retrieveRights` | Reply to a user-authored message. The bot calls `getChatMember` for that user and replies with their `canReactToMessages` value. It reports `null` when the returned member state is not restricted. |
| `/admins` | In a group, supergroup, or channel, lists the chat administrators. It passes `retrieveOtherBots = true`, the library equivalent of Telegram's `return_bots = true`, so other bot administrators are included. |
| `/deleteReaction` | Reply to a user-authored message in a group or supergroup. Removes that user's reaction from the replied-to message. |
| `/deleteAllReactions` | Reply to a user-authored message in a group or supergroup. Removes up to 10,000 recent reactions made by that user in the current chat. |
| A content message from another bot arrives | Prints the sender and content to standard output; messages from this bot itself are ignored. |

No command reads positional arguments. The commands that operate on a user take that user from the replied-to message. Command failures from Telegram, including missing permissions, are left to the library's normal error handling.

This is an API example, not a production moderation bot: it does not check whether the person invoking a reaction-deletion command is an administrator.

## Telegram setup and permissions

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Add the bot to the group or supergroup used for the examples and promote it to administrator. Telegram only sends `chat_member` updates about other members to administrators, and `getChatMember` is only guaranteed to work for other users when the bot is an administrator.
3. Grant the bot the **Delete messages** (`can_delete_messages`) administrator right to use either reaction-deletion command.
4. To exercise the other-bot message handler, enable **Bot-to-Bot Communication Mode** for the receiving bot in @BotFather. For ordinary messages that are neither an addressed command nor a direct reply, the receiving bot must also be a group administrator and have **Group Privacy Mode** disabled. Re-add the bot after changing Group Privacy Mode so the change takes effect.

See Telegram's documentation for [`chat_member` updates and chat-management methods](https://core.telegram.org/bots/api) and [bot-to-bot communication](https://core.telegram.org/api/bots%2Fbot-to-bot).

## Launch

From the repository root, run:

```bash
./gradlew :ChatManagementBot:run --args="<BOT_TOKEN> [debug] [testServer]"
```

The bot token must be the first argument. The optional, case-sensitive flags may follow it in either order:

- `debug` sends the library's logging to standard output.
- `testServer` uses Telegram's Bot API test environment.

For example:

```bash
./gradlew :ChatManagementBot:run --args="123456:ABCDEF debug"
```
