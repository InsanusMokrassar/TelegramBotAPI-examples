# ChecklistsBot

A bot that handles Telegram premium checklist messages and tracks task completion events.

## Functionality

Listens for messages containing a checklist. When a checklist message is received, the bot sends
a formatted reply showing all tasks with their completion status. It also reacts to task-level
events: when a task is marked as done or a new task is added to an existing checklist, the bot
sends an update reply referencing the affected task.

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

None.

## Capabilities

- Detects `ChecklistContent` messages (Telegram Premium feature)
- Formats checklist tasks with ✅ (completed) and ⬜ (pending) indicators
- Handles `ChecklistTasksDone` events — replies when a task is marked complete
- Handles `ChecklistTasksAdded` events — replies when new tasks are appended
- Uses rich text message building for formatted output
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
