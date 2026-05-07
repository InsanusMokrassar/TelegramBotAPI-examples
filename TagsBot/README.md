# TagsBot

A bot that manages custom member tags in Telegram groups.

## Functionality

Allows administrators to assign custom text tags to group members, remove tags, and grant or
revoke the *manage tags* permission. All tag-related commands require the command to be sent as a
reply to the target member's message.

## Arguments

| Position | Value | Sample | Description |
|----------|-------|--------|-------------|
| 1 | `BOT_TOKEN` | `1234567890:AABBccDDeeFF` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Sample | Description |
|-------|--------|-------------|
| `debug` | `debug` | Enable verbose debug logging |
| `testServer` | `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/setChatMemberTag <tag>` | Set a custom tag on the replied-to member |
| `/removeChatMemberTag` | Remove the custom tag from the replied-to member |
| `/setCanManageTags <true\|false>` | Grant (`true`) or revoke (`false`) the *manage tags* admin right for the replied-to member |

All commands must be sent as a **reply** to the target user's message.

## Capabilities

- Sets custom tags on group members via `setChatMemberTag`
- Removes tags via `removeChatMemberTag`
- Promotes members with tag management permission via `promoteChatMember`
- Reads existing tag information through the Risk API (`getChatMember`)
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
