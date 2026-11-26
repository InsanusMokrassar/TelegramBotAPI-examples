# TelegramBotAPI examples

Runnable examples for [TelegramBotAPI](https://github.com/InsanusMokrassar/TelegramBotAPI) 38.0.0, covering Telegram Bot API 10.4 and 10.5. This branch uses `dev.inmo:tgbotapi:38.0.0-t1` from Maven Local. Each module has a README with its behavior, setup, permissions, and launch arguments.

## Running an example

If `38.0.0-t1` is missing from Maven Local, run `./gradlew -Plibrary_version=38.0.0-t1 publishToMavenLocal`
in the TelegramBotAPI `38.0.0` checkout.

When updating an existing examples checkout, run `./gradlew kotlinUpgradeYarnLock` if Kotlin/JS reports
that the local `yarn.lock` has changed.

Run commands from the repository root and replace placeholders such as `<BOT_TOKEN>` and `<ADMIN_USER_ID>`. The table uses JVM launchers for multiplatform modules; their module READMEs also document browser and native targets where available.

The shortcuts show `debug` and `testServer` where supported. Pass them as bare words inside `--args`. Omit `debug` for normal logging and `testServer` to use Telegram's production Bot API.

`InlineQueriesBot` accepts `configureInline` to enable inline mode. `EphemeralMessagesBot` accepts `inlineCallbacks` or `defaultCallbacks` to select how Telegram delivers button presses. Telegram saves these settings across runs. [Inline query setup](InlineQueriesBot/README.md#setup) · [Ephemeral callback modes](EphemeralMessagesBot/README.md#run)

Native targets on Linux require libcurl development files, for example:

```bash
sudo apt install libcurl4-gnutls-dev
```

## Modules

| Module | What it demonstrates | Launch shortcut |
| --- | --- | --- |
| [BoostsInfoBot](BoostsInfoBot/) | Requests a channel, lists the requesting user's boosts, and logs boost updates. | `./gradlew :BoostsInfoBot:run --args="<BOT_TOKEN> debug"` |
| [BotSubscriptionsBot](BotSubscriptionsBot/) | Observes recurring Telegram Stars subscription state updates. | `./gradlew :BotSubscriptionsBot:run --args="<BOT_TOKEN> debug testServer"` |
| [BusinessConnectionsBot](BusinessConnectionsBot/) | Manages a connected business account, including deletion of regular gifts by owned ID. | `./gradlew :BusinessConnectionsBot:run --args="<BOT_TOKEN> debug"` |
| [ChatAvatarSetter](ChatAvatarSetter/) | Sets a chat photo from an image sent to the bot. | `./gradlew :ChatAvatarSetter:run --args="<BOT_TOKEN>"` |
| [ChatManagementBot](ChatManagementBot/) | Exercises member permissions, administrator queries, reaction deletion, and bot-to-bot messages. | `./gradlew :ChatManagementBot:run --args="<BOT_TOKEN> debug testServer"` |
| [ChecklistsBot](ChecklistsBot/) | Receives and renders checklist messages and checklist service events. | `./gradlew :ChecklistsBot:run --args="<BOT_TOKEN> debug testServer"` |
| [CommunitiesBot](CommunitiesBot/) | Handles chat/community membership and user-from-community join events. | `./gradlew :CommunitiesBot:run --args="<BOT_TOKEN> debug testServer"` |
| [CustomBot](CustomBot/) | Provides a diagnostics playground for contexts, request logging, profile audio, and Stars balance. | `./gradlew :CustomBot:run --args="<BOT_TOKEN> debug testServer"` |
| [DeepLinksBot](DeepLinksBot/) | Generates deep links and owner invitations, handles start payloads, and inspects chat share themes. | `./gradlew :DeepLinksBot:run --args="<BOT_TOKEN>"` |
| [DraftsBot](DraftsBot/) | Streams empty or stoppable message drafts and handles generation-stopped updates. | `./gradlew :DraftsBot:run --args="<BOT_TOKEN>"` |
| [EphemeralMessagesBot](EphemeralMessagesBot/) | Sends ephemeral replies to callbacks and edits photos and Live Photos for a single recipient. | `./gradlew :EphemeralMessagesBot:run --args="<BOT_TOKEN> debug testServer"` |
| [FSMBot](FSMBot/) | Implements a conversational finite-state machine with chat-scoped in-memory state. | `./gradlew :FSMBot:run --args="<BOT_TOKEN>"` |
| [FilesLoaderBot](FilesLoaderBot/) | Downloads incoming media to disk and sends it back to the chat. | `./gradlew :FilesLoaderBot:run --args="<BOT_TOKEN> <OUTPUT_DIRECTORY>"` |
| [ForwardInfoSenderBot](ForwardInfoSenderBot/) | Reports the forward-origin metadata of received content. | `./gradlew :ForwardInfoSenderBot:run --args="<BOT_TOKEN>"` |
| [GiftsBot](GiftsBot/) | Lists owned gifts, looks up unique gifts by name, and displays gift events. | `./gradlew :GiftsBot:run --args="<BOT_TOKEN> debug testServer"` |
| [GiveawaysBot](GiveawaysBot/) | Logs giveaway creation, completion, and winner events. | `./gradlew :GiveawaysBot:run --args="<BOT_TOKEN> debug testServer"` |
| [GuestQueryBot](GuestQueryBot/) | Handles guest queries in chats where the bot is not a member. | `./gradlew :GuestQueryBot:run --args="<BOT_TOKEN> debug testServer"` |
| [HelloBot](HelloBot/) | Greets users, groups, channels, or business chats when mentioned. | `./gradlew :HelloBot:run --args="<BOT_TOKEN>"` |
| [InlineQueriesBot](InlineQueriesBot/) | Configures inline mode, answers paginated queries, and supplies a deep-link result. | `./gradlew :InlineQueriesBot:runJvm --args="<BOT_TOKEN>"` |
| [JoinRequestQueriesBot](JoinRequestQueriesBot/) | Queues or approves join requests, optionally delegating the decision to a Web App. | `./gradlew :JoinRequestQueriesBot:run --args="<BOT_TOKEN> https://example.com/verify debug testServer"` |
| [KeyboardsBot](KeyboardsBot/) | Demonstrates reply, inline, disabled, forced-reply, paged, and inline-mode keyboards. | `./gradlew :KeyboardsBot:jvm_launcher:run --args="<BOT_TOKEN> debug"` |
| [LinkPreviewsBot](LinkPreviewsBot/) | Sends the same link using multiple link-preview configurations. | `./gradlew :LinkPreviewsBot:run --args="<BOT_TOKEN> debug"` |
| [LiveLocationsBot](LiveLocationsBot/) | Sends, updates, cancels, and stops a live-location message. | `./gradlew :LiveLocationsBot:run --args="<BOT_TOKEN>"` |
| [LivePhotosBot](LivePhotosBot/) | Receives, uploads, groups, edits, and sells Telegram Live Photos. | `./gradlew :LivePhotosBot:run --args="<BOT_TOKEN> debug testServer"` |
| [ManagedBotsBot](ManagedBotsBot/) | Creates and administers managed bots and inspects personal-channel messages. | `./gradlew :ManagedBotsBot:run --args="<BOT_TOKEN> debug testServer"` |
| [MemberUpdatedWatcherBot](MemberUpdatedWatcherBot/) | Logs and reports bot/member status transitions in chats. | `./gradlew :MemberUpdatedWatcherBot:run --args="<BOT_TOKEN> debug"` |
| [MyBot](MyBot/) | Replaces or removes the bot's global profile photo and prints diagnostics. | `./gradlew :MyBot:run --args="<BOT_TOKEN> debug testServer"` |
| [PollsBot](PollsBot/) | Sends regular polls, quizzes, poll media, and handles poll updates. | `./gradlew :PollsBot:run --args="<BOT_TOKEN> debug"` |
| [RandomFileSenderBot](RandomFileSenderBot/) | Picks random local files and sends them individually or as media groups. | `./gradlew :RandomFileSenderBot:runJvm --args="<BOT_TOKEN> <FILES_DIRECTORY>"` |
| [ReactionsInfoBot](ReactionsInfoBot/) | Handles per-user reaction changes and anonymous reaction-count updates. | `./gradlew :ReactionsInfoBot:run --args="<BOT_TOKEN> debug"` |
| [ResenderBot](ResenderBot/) | Recreates received content while preserving reply, quote, effect, and business context. | `./gradlew :ResenderBot:jvm_launcher:run --args="<BOT_TOKEN> debug"` |
| [RichMessagesBot](RichMessagesBot/) | Builds expandable quotations with paragraphs and lists, and replies to rich-text selections. | `./gradlew :RichMessagesBot:run --args="<BOT_TOKEN> debug testServer"` |
| [RightsChangerBot](RightsChangerBot/) | Changes member and administrator rights, and demonstrates linked third-party verification descriptions. | `./gradlew :RightsChangerBot:run --args="<BOT_TOKEN> <ALLOWED_USER_ID> debug"` |
| [SlotMachineDetectorBot](SlotMachineDetectorBot/) | Detects slot-machine dice and decodes their reel values. | `./gradlew :SlotMachineDetectorBot:run --args="<BOT_TOKEN>"` |
| [StarTransactionsBot](StarTransactionsBot/) | Handles Stars payments, paid-message waivers, TON wallet cards, and transfer events. | `./gradlew :StarTransactionsBot:run --args="<BOT_TOKEN> <ADMIN_USER_ID> debug testServer"` |
| [StickerInfoBot](StickerInfoBot/) | Looks up sticker-set metadata for stickers and custom emoji. | `./gradlew :StickerInfoBot:jvm_launcher:run --args="<BOT_TOKEN>"` |
| [StickerSetHandler](StickerSetHandler/) | Creates and manages a sticker set owned by each private-chat user. | `./gradlew :StickerSetHandler:run --args="<BOT_TOKEN>"` |
| [SuggestedPosts](SuggestedPosts/) | Handles channel direct messages and the suggested-post lifecycle. | `./gradlew :SuggestedPosts:run --args="<BOT_TOKEN> debug testServer"` |
| [TagsBot](TagsBot/) | Sets chat-member tags, delegates tag management, and reads sender tags. | `./gradlew :TagsBot:run --args="<BOT_TOKEN> debug testServer"` |
| [TopicsHandling](TopicsHandling/) | Exercises forum-topic and private-chat-topic actions and events. | `./gradlew :TopicsHandling:run --args="<BOT_TOKEN>"` |
| [UserChatShared](UserChatShared/) | Requests users or chats through reply keyboards and handles the shared results. | `./gradlew :UserChatShared:run --args="<BOT_TOKEN> debug"` |
| [WebApp](WebApp/) | Serves a Compose Web client and demonstrates Telegram Web App integration. | `./gradlew :WebApp:runJvm --args="<BOT_TOKEN> https://webapp.example 8080 debug testServer"` |
| [WebHooks](WebHooks/) | Receives Telegram updates through a Ktor webhook server instead of long polling. | `./gradlew :WebHooks:run --args="<BOT_TOKEN> https://bot.example.com debug"` |
| [WidgetsBot](WidgetsBot/) | Sends interactive HTML dice directly or inline, answers widget queries, and updates individual or shared rolls. | `./gradlew :WidgetsBot:run --args="<BOT_TOKEN> debug testServer"` |

## Repository as a reference

The example structure can be used as a starting point, and the commit history is useful for seeing migrations between TelegramBotAPI versions. For new projects, consider the [Telegram Bot template](https://github.com/InsanusMokrassar/TelegramBotAPI-bot_template) or [Kotlin Multiplatform Project template](https://github.com/InsanusMokrassar/KotlinMultiplatformProjectTemplate).
