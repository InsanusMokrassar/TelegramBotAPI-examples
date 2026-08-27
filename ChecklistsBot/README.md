# ChecklistsBot

This example shows how to receive Telegram checklist messages and checklist service events with
the TelegramBotAPI behaviour builder. It mirrors each checklist as a formatted text reply; it does
not create or edit checklists and stores no state.

## Behaviour

The bot uses long polling and installs these handlers:

| Incoming update | Response |
| --- | --- |
| A message containing a checklist | Replies to that message with the checklist's current contents. |
| Tasks marked as done or not done | Replies to the checklist-status service message with the full current checklist. If at least one task was newly marked done, the reply includes the first such task's ID as `checklist_task_id`; an event containing only newly reopened tasks gets a normal message-level reply. |
| Tasks added to a checklist | Replies to the original checklist and targets the first newly added task with `checklist_task_id`, then includes the full current checklist. |

The text snapshot preserves the title and task text entities. Each task is rendered on its own line
as `• [x] task` when it has a completion date or `• [ ] task` otherwise; the marker is formatted as
code and the task text as bold.

There are no bot commands. On startup the bot prints the result of `getMe`, and it prints every raw
update it receives. The optional `debug` flag additionally sends TelegramBotAPI diagnostic logs to
standard output.

## Telegram setup

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Start the example, open a private chat with the bot, and send it a checklist. Telegram Premium is
   currently required to create a checklist in Telegram clients. Use the checklist's own options if
   other participants should be allowed to add tasks or change their completion state.
3. For group testing, ensure the bot may send messages and can receive the original checklist.
   Ordinary checklist messages are hidden by the default Group Privacy Mode, so make the bot a group
   administrator or disable privacy with BotFather and re-add the bot to the group.

No Telegram Business connection or business-bot right is required: the example only receives
checklists and sends ordinary text replies. Telegram restricts the `sendChecklist` Bot API method,
which this example does not use, to connected business accounts.

## Run

From the repository root:

```bash
./gradlew :ChecklistsBot:run --args="<BOT_TOKEN>"
```

The token must be first. The remaining optional flags are exact, case-sensitive strings and may be
supplied in either order:

| Argument | Required | Meaning |
| --- | --- | --- |
| `<BOT_TOKEN>` | Yes | Bot token; it must be the first argument. |
| `debug` | No | Enables verbose TelegramBotAPI logging on standard output. |
| `testServer` | No | Uses Telegram's Bot API test environment (`/test`) instead of production. |

For example:

```bash
./gradlew :ChecklistsBot:run --args="<BOT_TOKEN> debug testServer"
```

Do not run another webhook or long-polling consumer with the same token while this example is
running.

## API concepts demonstrated

- `onChecklistContent` for checklist content messages.
- `onChecklistTasksDone` for service messages that contain both completed and reopened task IDs.
- `onChecklistTasksAdded` for task-addition service messages.
- Entity-aware output with `buildEntities`, `code`, and `bold`.
- Replies to a specific checklist item through `checklist_task_id`.

See Telegram's [Checklist and ChecklistTask objects](https://core.telegram.org/bots/api#checklist)
and [checklist launch announcement](https://telegram.org/blog/checklists-suggested-posts#checklists)
for the underlying platform behaviour.
