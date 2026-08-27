# RichMessagesBot

RichMessagesBot is a long-polling showcase of Telegram rich messages. It sends
rich content from HTML, Markdown, and the typed `InputRichMessageBlocks` DSL;
streams drafts; edits rich text; handles incoming rich messages; and supplies
rich content from inline and guest queries.

## Commands

The bot installs seven commands in Telegram's default command menu. Three
additional handlers can be invoked by typing their commands manually.

| Command | In menu | Demonstration |
| --- | --- | --- |
| `/rich_html` | Yes | Sends the full HTML fixture: inline styles and links, references, emoji and time links, math, headings, lists and checkboxes, quotations, remote media and maps, collages, slideshows, tables, details, and captions. |
| `/rich_markdown` | Yes | Sends and logs the corresponding Markdown fixture, including remote photo, video, audio, voice-note, animation, collage, and slideshow markup. |
| `/rich_markdown_medialess` | No | Sends and logs the Markdown fixture without media, collages, or slideshows. |
| `/rich_markdown_blocks` | No | Sends and logs the full fixture as a typed block tree, including first-class media blocks, captions, collages, and slideshows. |
| `/rich_markdown_medialess_blocks` | No | Sends and logs the same typed block tree without its media section. |
| `/rich_blocks` | Yes | Sends a smaller, directly constructed block tree with headings, formatted paragraphs, ordered and unordered checkbox lists, a divider, preformatted Kotlin, and a quotation. |
| `/rich_draft` | Yes | Streams three Markdown revisions under draft ID `1`, one second apart, then sends a normal final rich message. |
| `/rich_blocks_draft` | Yes | Streams two draft-only `thinking()` blocks under draft ID `2`, one second apart, then sends a normal typed-block answer. |
| `/rich_edit` | Yes | Sends a Markdown rich message, waits two seconds, and replaces its rich content with `EditChatMessageRichText`. |
| `/wait_rich` | Yes | Prompts for a rich message, waits for the next matching content, and reports its block count. |

The two draft examples finalize by sending a new normal rich message; they do not
turn the draft itself into the final message. They use distinct fixed IDs (`1`
and `2`); concurrent runs of the same command in one chat reuse that command's
ID.

## Other triggers

| Trigger | Behavior |
| --- | --- |
| Any photo | Reuses the received Telegram file ID without downloading it. The bot first sends HTML whose `tg://photo?id=userphoto` reference is resolved by `InputRichMessageMedia`, then sends the same photo as a typed `photo()` block. |
| Any incoming rich message | Logs right-to-left state and every parsed block, replies with the block count, and resends the rich message with `createResend`. The `onlyRichMessageContentMessages` flow also logs its block count. |
| Any inline query | Returns uncached HTML and Markdown articles whose selected messages use `InputRichMessageContent`. |
| A text guest request containing `/rich_guest` | Returns one inline article containing the full Markdown fixture. This is a substring check, not a registered bot command. Non-text guest requests and text without that exact case-sensitive substring are ignored by this handler. |

Every received update is also printed to standard output. A rich message received
while `/wait_rich` is active can therefore be observed by the waiter, the general
rich-message trigger, and the filtered update flow.

## Media notes

The built-in HTML and Markdown fixtures refer to public files under
`https://telegram.org/example/`. The typed full fixture constructs Telegram media
from the same URLs. The photo trigger instead demonstrates reusing an existing
Telegram file ID and assigning an alias for a `tg://photo?id=...` reference.

The source also shows the two library shapes used for rich media: an
`InputRichMessageMedia` mapping for markup references and first-class photo,
video, audio, voice-note, animation, collage, and slideshow blocks. The library
can collect multipart files nested in a rich-message tree as `attach://` uploads,
although this example's running handlers use URLs or an existing file ID.

## Telegram setup and permissions

1. Create a bot with BotFather and obtain its token. Keep the token out of source
   control.
2. Start a private chat with the bot, or add it to a chat and allow it to send
   messages and media.
3. To test ordinary photo and rich-message triggers in a group, ensure Telegram
   delivers non-command messages to the bot, for example by disabling Group
   Privacy Mode or making the bot an administrator.
4. Enable Inline Mode in BotFather to exercise the inline-query results.
5. Enable guest queries for the bot to exercise the `/rich_guest` guest-request
   path. The bot does not need to be a member of the target chat for that path.

No handler requires an administrator-only Bot API method. The program does not
configure a webhook or validate chat permissions before making requests.

## Arguments

The first program argument is required and is always treated as the bot token.
Optional flags are exact and case-sensitive, may follow the token in either
order, and unknown later arguments are ignored.

| Argument | Effect |
| --- | --- |
| `<BOT_TOKEN>` | Token used to create the bot. Omitting it fails before polling starts. |
| `debug` | Enables formatted KSLog diagnostics on standard output. |
| `testServer` | Connects to Telegram's Bot API test environment. |

## Run

From the repository root:

```bash
./gradlew :RichMessagesBot:run --args="<BOT_TOKEN>"
```

For example, with both optional flags:

```bash
./gradlew :RichMessagesBot:run --args="<BOT_TOKEN> debug testServer"
```
