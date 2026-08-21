# TagsBot

A long-polling example for setting chat-member tags, delegating tag-management
rights, and reading sender tags from group messages.

## Commands

All commands target the identifiable user who sent the replied-to group content
message. Without such a reply, they silently do nothing.

- `/setChatMemberTag <tag>` calls `setChatMemberTag` for that user. The tag is the
  command's remaining text after removing at most one leading space; it is not
  otherwise trimmed.
- `/removeChatMemberTag` clears that user's tag by sending `null`.
- `/setCanManageTags true` invokes `promoteChatAdministrator` with
  `canManageTags = true`. Any remaining text other than exact, lowercase `true`
  sets that permission to `false`.

The bot sends no success reply for these operations and does not register commands
in Telegram's command menu.

## Message trigger and storage

For every delivered group content message that can be interpreted as potentially
coming from a user, the bot sends two replies:

- `Tag after casting: <tag>` using the typed `senderTag` property;
- `Tag by getting via risk API: <tag>` using the raw `sender_tag` field.

The displayed value may be `null`. Command messages also reach this content handler.
Tags and tag-management rights are stored by Telegram as chat-member state; this
example has no local map, database, or persistence layer. It prints its own bot
information at startup and logs every received update.

## Setup, permissions, and privacy

1. Create a bot, obtain its token, and keep it private.
2. Add it to a test group and allow it to send messages.
3. Grant the Telegram administrator rights needed to set member tags and promote
   members/change their `canManageTags` permission.

To inspect ordinary group messages, configure bot privacy so Telegram delivers them;
an administrator bot normally has broader visibility, but verify delivery in the
target group. The bot publicly echoes sender tags and logs full updates, so protect
the group and process output. The code configures no webhook.

## Run

From the repository root:

```bash
./gradlew :TagsBot:run --args="BOT_TOKEN"
```

The first argument is the required token. Optional exact flags may follow it in any
order: `debug` enables formatted logging, and `testServer` selects Telegram's test
environment. Other arguments are ignored.
