# RightsChangerBot

A long-polling/FSM example for changing a member's chat permissions and a channel administrator's rights with inline keyboards. The bot registers its commands in the all-group-chats scope and prints every received update to standard output.

## Commands and callbacks

### Member permissions

Run these commands in a group or supergroup as a reply to a non-administrator's message:

- `/simple` shows toggles for polls, other messages, and web-page previews. Changes use Telegram's dependent/common permission model (`useIndependentChatPermissions = false`), so related permissions may change together.
- `/granular` shows independent toggles for text messages, other messages, audio, voice notes, video, video notes, photos, web-page previews, polls, and documents (`useIndependentChatPermissions = true`).

The keyboard shows `✅` for allowed, `❌` for denied, and no suffix when Telegram reports no explicit value. Clicking a button restricts the replied-to member and edits the keyboard with the refreshed state. If the command is not a valid reply, or the target is an administrator/owner rather than a normal or restricted member, the bot usually returns without a response.

### Channel administrator rights

Send `/rights_in_channel` in a private chat with the bot. Telegram's chat/user request buttons used by this flow are available only in private chats, even though the command is registered only in the group command-menu scope.

1. Select a channel where the bot is already a member. The picker requests `can_promote_members` and `can_restrict_members` for both the bot and the selecting user.
2. Select a user, or send `/cancel` during either selection step.
3. If the selected user is currently a channel administrator, the bot sends a keyboard for refreshing or toggling post-message, edit-message, delete-message, post-story, edit-story, and delete-story rights.

Selecting a non-administrator ends the flow without a message. A stale keyboard whose target is later demoted can display **Promote to admin**; that action promotes the target with the post-messages right enabled. Telegram allows the bot to change an administrator's rights only when the bot promoted that administrator itself.

Callbacks are processed only when clicked by the configured allowed user. They are not explicitly answered with `answerCallbackQuery`, so Telegram clients may retain their loading indicator until the edit completes or the callback times out.

## Setup, permissions, and security

- Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
- Choose one trusted Telegram numeric user ID as `ALLOWED_USER_ID`. `/simple` and all permission-changing callbacks are restricted to this ID.
- Promote the bot in managed groups/channels. It needs `can_restrict_members` for member permissions and `can_promote_members` for channel administrator rights, plus enough rights to read member state and send/edit its keyboard messages. It cannot grant rights it does not possess.
- BotFather privacy mode can remain enabled because the group workflows use commands, replies, and callbacks.
- Treat console output as sensitive: the example prints every raw update, and handler failures print stack traces.

Current authorization caveats: `/rights_in_channel` has no sender filter, although its resulting mutation callbacks still require `ALLOWED_USER_ID`. `/granular` checks `ALLOWED_USER_ID` in normal public chats but accepts channel-post commands without that sender check; its callbacks remain protected. Keep the bot limited to trusted chats.

Long polling automatically removes an existing webhook at startup. Run only one update consumer for the token at a time.

## Arguments

The bot requires the token first and the allowed numeric user ID second. Debug logging is enabled only when the third argument is exactly `debug`.

## Launch

From the repository root:

```bash
./gradlew :RightsChangerBot:run --args="<BOT_TOKEN> <ALLOWED_USER_ID>"
```

```bash
./gradlew :RightsChangerBot:run --args="<BOT_TOKEN> <ALLOWED_USER_ID> debug"
```
