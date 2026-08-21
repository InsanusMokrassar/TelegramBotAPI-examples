# PollsBot

A long-polling showcase for regular polls, quizzes, poll media, targeting, and
poll-related updates. It registers all nine commands in Telegram's command menu.

## Commands

| Command | Behavior |
| --- | --- |
| `/anonymous` | Sends an anonymous poll with ten numbered options. |
| `/public` | Sends a nonanonymous ten-option poll, allows added options, and hides results until closure. |
| `/quiz` | Sends a nonanonymous, shuffled quiz with revoting and hidden results; it randomly collects zero to seven distinct correct options and allows multiple answers when needed. |
| `/media_poll` | Adds location media to the question and venue/location media to options; replying to a sticker adds it as another option. |
| `/quiz_media` | Asks where the Eiffel Tower is, with location question media, Paris as the answer, and venue explanation media. |
| `/members_only` | Sends an anonymous Yes/No poll restricted to members. |
| `/country_codes` | Sends an anonymous poll targeted to `US`, `DE`, and `JP`. |
| `/single_option` | Sends a nonanonymous poll containing only `Got it`. |
| `/link_poll` | Sends a nonanonymous poll whose first two options carry link media. |

`/anonymous`, `/public`, and `/quiz` may contain extra text; the first custom-emoji
entity after the command is copied into the poll's text and options.

## Poll lifecycle and triggers

The bot keeps an in-memory poll-ID-to-chat map for polls it sends. Poll answers
produce a chat notification naming the answering user or voter chat. Poll updates
report anonymity, media, member/country restrictions, quiz explanation media, and
each option's votes and media.

Added or deleted poll-option events produce replies containing the option text. A
content message associated with a poll-option reply produces `Reply to poll option`
on that option. Every received update is printed to standard output.

There is no command to close a poll. Restarting the bot clears its routing map, so
later answers and updates for earlier polls are no longer reported to their chats.

## Setup and permissions

Create a bot, keep its token private, and start it in a private chat or add it to a
group where it may send messages and polls. No administrator-only methods or webhook
configuration are used. Some poll targeting or media features require a Telegram
chat and client that support them.

## Run

The intended command from the repository root is:

```bash
./gradlew :PollsBot:run --args="BOT_TOKEN"
```

The first argument is the required token. An optional exact `debug` argument in any
later position enables formatted logging; other arguments are ignored.

Known issue: `build.gradle` currently names `HelloBotKt` as the main class, while
this source's entry point is `PollsBotKt`; the `run` task cannot start until that
Gradle setting is corrected.
