# TopicsHandling

A long-polling example for Telegram forum-topic and private-chat-topic APIs. It executes topic-management commands, reports selected topic service events, and prints raw updates.

## Commands and actions

The commands take no arguments:

- `/start_test_topics` runs a timed topic-management sequence. It creates a green **Test** topic, renames it to **Test 01**, and deletes it. In a forum supergroup it also closes/reopens the test topic; hides/unhides and closes/reopens the General topic; renames the General topic to a random 10-character value; and finally renames it to **Main topic**. Status replies are sent after each successful action.
- `/delete_topic` deletes the forum topic containing the command. Outside a forum topic it returns silently, and it asks for no confirmation.
- `/unpin_all_forum_topic_messages` unpins every pinned message in the topic containing the command. Outside a forum topic it returns silently and sends no success reply. Its registered command-menu description currently repeats the delete-topic description.

In a private chat, `/start_test_topics` first checks the bot's `has_topics_enabled` flag. If private topics are disabled, it logs a warning and returns without replying. If enabled, it performs only the create, rename, and delete steps because private topics cannot be closed/reopened and have no General-topic sequence. The commands are registered only in the all-group-chats menu scope, so the private-chat command must be typed manually.

## Event triggers

The bot replies to these service events:

- forum topic created or edited;
- private topic created or edited;
- forum topic reopened;
- General topic hidden or unhidden.

There is no topic-closed or topic-deleted reply handler. General-topic edits and reopens can match the generic edited/reopened handlers above, while hiding and unhiding use their dedicated handlers. All incoming updates are nevertheless logged. The source currently subscribes to the raw update flow twice, so each update is normally printed twice.

## Setup, permissions, and safety

- Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
- For group use, add it to a forum-enabled supergroup and promote it to administrator with `can_manage_topics`. It also needs permission to send messages; `/unpin_all_forum_topic_messages` additionally requires `can_pin_messages`.
- Enable private-chat topics for the bot if the private variant of `/start_test_topics` should work.
- BotFather privacy mode may remain enabled because the group interactions are commands and topic service events, and the bot must already be an administrator.

There is no sender allowlist or administrator check in the command handlers. Any user whose command reaches the bot can make it delete a topic, remove topic pins, or run the General-topic mutation sequence using the bot's administrator rights. Use a disposable/trusted test chat or add authorization before deployment. The test sequence does not restore the General topic's previous name; it leaves it open and named **Main topic**.

At startup the bot flushes accumulated updates before registering its handlers, so pending updates are discarded. Long polling also removes an existing webhook. Handler failures print stack traces.

## Arguments

The first argument is the required bot token. Additional arguments are ignored; there is no debug option.

## Launch

From the repository root:

```bash
./gradlew :TopicsHandling:run --args="<BOT_TOKEN>"
```
