# RightsChangerBot

A bot for managing user permissions and administrator rights in Telegram groups and channels.

## Functionality

Provides two modes of permission editing (simple / granular) for regular member restrictions, and a
full FSM-based flow for editing channel administrator rights. Changes are presented as inline
keyboards with visual ✅/❌ toggles that persist until the user is done.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |
| 2 | `ADMIN_USER_ID` | Numeric Telegram user ID allowed to use the bot |

Optional flags (any order after the required arguments):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/simple` | Show a common-permissions keyboard (send messages, polls, web previews, etc.) for the replied-to user |
| `/granular` | Show a granular-permissions keyboard (individual media types) for the replied-to user |
| `/rights_in_channel` | Start the FSM flow to pick a channel and a user, then edit that user's administrator rights in the channel |

All commands must be sent as a **reply** to a target user's message.

## Capabilities

- **Simple mode** — toggles grouped permissions: send messages, send media, send polls, send other content, add web page previews, change info, invite users, pin messages
- **Granular mode** — toggles individual media-type permissions: audios, documents, photos, videos, video notes, voice notes, stickers, animations, games, gift premiums, forward channels, forward non-channels
- **Channel admin rights** — FSM with three states:
  1. `RetrievingChannelChatState` — user picks the channel
  2. `RetrievingUserIdChatState` — user picks the member
  3. `RetrievingChatInfoDoneState` — inline keyboard for toggling admin rights (post messages, edit messages, delete messages, ban users, invite users, pin messages, manage topics, manage video chats, post stories, edit stories, delete stories, remain anonymous)
- Inline keyboard callbacks update permission state in real time
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN ADMIN_USER_ID"
```
