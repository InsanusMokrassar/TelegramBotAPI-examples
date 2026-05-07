# TopicsHandling

A bot that demonstrates full forum-topic management for Telegram supergroups with forum mode enabled.

## Functionality

Provides commands to create, edit, close, reopen, and delete forum topics. Also handles the general
topic (hide/unhide, close/reopen, pin/unpin messages) and listens for all topic lifecycle events,
logging each one.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start_test_topics` | Runs a full test sequence: creates a topic, edits its name and icon, pins a message, closes and reopens it, then deletes it |
| `/delete_topic` | Deletes the forum topic in which the command was sent |
| `/unpin_all_forum_topic_messages` | Unpins all messages in the current forum topic |

## Capabilities

- Creates colour-coded forum topics with a custom emoji icon
- Edits topic name and icon
- Closes and reopens topics
- Deletes topics
- Manages the general (default) topic: hide, unhide, close, reopen
- Pins and unpins messages within a topic
- Detects and logs topic creation, editing, closure, reopening, and general-topic visibility changes
- Detects private forum support and enables private topics when available
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```

> **Note:** The bot must be an administrator with *Manage Topics* permission in the target supergroup.
