# CommunitiesBot

This long-polling example demonstrates Communities support introduced in Telegram Bot API 10.2 and extended in 10.3: typed service events when a chat joins or leaves a community, an event when a user joins a chat from a community, and inspection of a chat's current community.

## Behavior, commands, and triggers

At startup, the bot calls `getMe` and prints its bot information. It also prints every received update to standard output.

| Command or trigger | Behavior |
| --- | --- |
| `community_chat_added` service message | `onCommunityChatAdded` logs the chat and community name/ID, sends a join notice, then calls `getChat` and logs its nullable `community`. |
| `community_chat_removed` service message | `onCommunityChatRemoved` logs the chat and sends a leave notice. This event is fieldless, so it has no former-community details. |
| `community_chat_joined` service message | `onCommunityChatJoined` logs the source community and replies with a welcome message. This means a user joined the current chat from a community; it is distinct from adding the chat itself to a community. |
| `/community` | Calls `getChat` and replies with the current community name/ID, or says the chat is not in a community. |
| `/wait_community_added` | Waits without a timeout for the next added event in the command's chat, then replies with the community name/ID. |
| `/wait_community_removed` | Waits without a timeout for the next removed event in the command's chat, then replies with the chat ID. |
| `/wait_community_joined` | Waits without a timeout for the next user-from-community join event in the command's chat, then replies with the source community name/ID. |

Commands use no positional arguments; other commands only appear in the generic update log. Each wait first sends a waiting reply, filters events with `sameChat`, and takes the first match.

## API concepts

- `CommunityChatAdded` carries a `Community` with a `CommunityId` and name; `CommunityChatRemoved` carries no fields.
- `CommunityChatJoined` carries the community through which a user joined the current chat.
- `onCommunityChatAdded`, `onCommunityChatRemoved`, and `onCommunityChatJoined` provide typed handlers for the service events.
- `getChat(...).community` exposes the nullable community on `ExtendedChat` without a subtype cast.
- `waitCommunityChatAddedEventsMessages`, `waitCommunityChatRemovedEventsMessages`, and `waitCommunityChatJoinedEventsMessages` expose typed event-message flows.

## Telegram setup and permissions

1. Create a bot with BotFather and obtain its token.
2. Add it to the target chat before changing that chat's community membership if you want to observe both service events.
3. Allow it to send messages so notifications and command replies succeed.

The bot does not create or modify communities. It calls no administrator-only method and does not inspect arbitrary user messages, so it needs neither administrator rights nor disabled Group Privacy Mode. The user changing community membership still needs the appropriate Telegram rights. API failures are left to the library's normal error handling.

## Arguments

The token is required as the first argument. Optional flags are exact and case-sensitive, may follow in either order, and unknown extra arguments are ignored.

| Argument | Effect |
| --- | --- |
| `BOT_TOKEN` | Bot token; omitting it fails before polling starts. |
| `debug` | Sends tgbotapi/KSLog diagnostics to standard output. |
| `testServer` | Uses Telegram's Bot API test environment. |

## Launch

From the repository root:

```bash
./gradlew :CommunitiesBot:run --args="BOT_TOKEN"
```
