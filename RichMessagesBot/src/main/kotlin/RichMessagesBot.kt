import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendRichMessage
import dev.inmo.tgbotapi.extensions.api.send.sendRichMessageDraft
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitRichMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onRichMessage
import dev.inmo.tgbotapi.extensions.utils.baseSentMessageUpdateOrNull
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.onlyRichMessageContentMessages
import dev.inmo.tgbotapi.requests.edit.text.EditChatMessageRichText
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputRichMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import dev.inmo.tgbotapi.types.rich.InputRichMessageHTML
import dev.inmo.tgbotapi.types.rich.InputRichMessageMarkdown
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

/**
 * This bot demonstrates Rich Messages support introduced in Telegram Bot API 10.1.
 *
 * Rich messages allow bots to send highly structured text (and to stream AI-generated replies
 * with seamless rich formatting). Telegram parses the provided HTML/Markdown into a structured
 * [dev.inmo.tgbotapi.types.rich.RichMessage] made of [dev.inmo.tgbotapi.types.rich.RichBlock]s.
 *
 * Key concepts demonstrated:
 * - [dev.inmo.tgbotapi.types.rich.InputRichMessage] — describes a rich message to send. Built only via
 *   the [InputRichMessageHTML] / [InputRichMessageMarkdown] factories (exactly one format must be used)
 * - [sendRichMessage] — sendRichMessage method
 * - [sendRichMessageDraft] — sendRichMessageDraft method: stream partial rich messages by draftId
 * - [EditChatMessageRichText] — editMessageText with the new `rich_message` parameter
 * - [onRichMessage] — trigger for incoming [dev.inmo.tgbotapi.types.message.content.RichMessageContent]
 *   (the new `rich_message` field of Message)
 * - [waitRichMessage] — expectation for a rich message
 * - [onlyRichMessageContentMessages] — flow filter keeping only rich message content
 * - [InputRichMessageContent] — usable as InputMessageContent in inline query results
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val isDebug = args.any { it == "debug" }
    val isTestServer = args.any { it == "testServer" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    telegramBotWithBehaviourAndLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        testServer = isTestServer
    ) {
        // sendRichMessage with HTML-formatted content
        onCommand("rich_html") {
            sendRichMessage(
                it.chat.id,
                // InputRichMessageHTML factory — content described using HTML formatting
                InputRichMessageHTML(
                    """
                        <a name="chapter-0"></a>
                        <b>bold text</b>, <strong>bold text</strong>
                        <i>italic text</i>, <em>italic text</em>
                        <u>underlined text</u>, <ins>underlined text</ins>
                        <s>strikethrough text</s>, <strike>strikethrough text</strike>, <del>strikethrough text</del>
                        <code>inline fixed-width code</code>
                        <mark>marked text</mark>
                        <sub>subscript text</sub>
                        <sup>superscript text</sup>
                        <tg-spoiler>spoiler</tg-spoiler>
                        
                        <a href="#note-1">Reference</a>
                        <a href="https://t.me/">inline URL</a>
                        <a href="mailto:user@example.com">inline e-mail</a>
                        <a href="tel:+123456789">inline phone number</a>
                        <a href="tg://user?id=123456789">inline mention of a user</a>
                        <a href="#chapter-1">in-document link</a>
                        <a name="chapter-1"></a>
                        
                        <tg-reference name="note-1">Referenced text</tg-reference>
                        <tg-emoji emoji-id="5368324170671202286">👍</tg-emoji>
                        <img src="tg://emoji?id=5368324170671202286" alt="👍"/>
                        <tg-time unix="1647531900" format="wDT">22:45 tomorrow</tg-time>
                        <tg-math>x^2 + y^2</tg-math>
                        
                        #hashtag ${'$'}USD +12345678901, card: 4242 4242 4242 4242, https://t.me t.me a@t.me /command @username
                        
                        all the text above was on the same line
                        
                        <h1>Heading 1</h1>
                        <h2>Heading 2</h2>
                        <h3>Heading 3</h3>
                        <h4>Heading 4</h4>
                        <h5>Heading 5</h5>
                        <h6>Heading 6</h6>
                        
                        <a name="chapter-2"></a>
                        
                        <p>Paragraph text</p>
                        <pre>pre-formatted fixed-width code block</pre>
                        <pre><code class="language-python">  print('pre-formatted fixed-width code block written in the Python programming language')</code></pre>
                        <footer>Footer text</footer>
                        <hr/>
                        <ul><li>unordered list item</li></ul>
                        <ol><li>ordered list item</li></ol>
                        <ol start="3" type="a" reversed><li>ordered list item</li></ol>
                        <ol><li value="7" type="i">ordered list item with explicit number</li></ol>
                        <ul>
                        <li><input type="checkbox" checked>Checked checkbox</li>
                        <li><input type="checkbox">Unchecked checkbox</li>
                        </ul>
                        
                        <blockquote>Block quotation started<br>Block quotation continued<br>The last line of the block quotation<cite>The Author</cite></blockquote>
                        <aside>Pull quote<cite>The Author</cite></aside>
                        
                        <img src="https://telegram.org/example/photo.jpg"/>
                        <video src="https://telegram.org/example/video.mp4"></video>
                        <audio src="https://telegram.org/example/audio.mp3"></audio>
                        <audio src="https://telegram.org/example/audio.ogg"></audio>
                        <video src="https://telegram.org/example/animation.gif"></video>
                        
                        <figure><img src="https://telegram.org/example/photo.jpg" tg-spoiler/><figcaption>Photo caption<cite>Photo credit</cite></figcaption></figure>
                        <figure><video src="https://telegram.org/example/video.mp4" tg-spoiler></video><figcaption>Video caption</figcaption></figure>
                        <figure><audio src="https://telegram.org/example/audio.mp3"></audio><figcaption>Audio caption</figcaption></figure>
                        <figure><audio src="https://telegram.org/example/audio.ogg"></audio><figcaption>Voice note caption</figcaption></figure>
                        <figure><video src="https://telegram.org/example/animation.gif" tg-spoiler></video><figcaption>Animation caption</figcaption></figure>
                        
                        <tg-map lat="41.9" long="12.5" zoom="14"/>
                        <figure><tg-map lat="41.9" long="12.5" zoom="14"/><figcaption>Map caption</figcaption></figure>
                        
                        <tg-collage><img src="https://telegram.org/example/photo.jpg"/><video src="https://telegram.org/example/video.mp4"/></tg-collage>
                        <tg-collage><video src="https://telegram.org/example/video.mp4"/><img src="https://telegram.org/example/photo.jpg"/><figcaption>Collage caption</figcaption></tg-collage>
                        <tg-slideshow><img src="https://telegram.org/example/photo.jpg"/><video src="https://telegram.org/example/video.mp4"/></tg-slideshow>
                        <tg-slideshow><video src="https://telegram.org/example/video.mp4"/><img src="https://telegram.org/example/photo.jpg"/><figcaption>Slideshow caption</figcaption></tg-slideshow>
                        
                        <table><tr><th>Header 1</th><th>Header 2</th></tr><tr><td>Value 1</td><td>Value 2</td></tr></table>
                        <table bordered striped><caption>Table caption</caption>
                        <tr><td colspan="2" rowspan="2" align="left">Value</td><td align="center">Value2</td><td align="right">Value3</td></tr>
                        <tr><td valign="top">Value4</td><td valign="middle">Value5</td><td valign="bottom">Value6</td></tr>
                        <tr><td>Value7</td></tr></table>
                        
                        <details><summary>Title</summary>Content</details>
                        <details open><summary>Title</summary>Content</details>
                        <tg-math-block>E = mc^2</tg-math-block>
                    """.trimIndent()
                )
            )
        }

        // sendRichMessage with Markdown-formatted content
        onCommand("rich_markdown") {
            val sent = sendRichMessage(
                it.chat.id,
                // InputRichMessageMarkdown factory — content described using Markdown formatting
                InputRichMessageMarkdown(
                    """
                        **bold text**
                        __bold text__
                        *italic text*
                        _italic text_
                        ~~strikethrough text~~
                        `inline fixed-width code`
                        ==marked text==
                        ||spoiler||

                        [inline URL](https://t.me/)
                        [inline e-mail](mailto:user@example.com)
                        [inline phone number](tel:+123456789)
                        [inline mention of a user](tg://user?id=123456789)
                        ![👍](tg://emoji?id=5368324170671202286)
                        ![22:45 tomorrow](tg://time?unix=1647531900&format=wDT)
                        ${'$'}x^2 + y^2$
                        \#hashtag ${'$'}USD +12345678901, card: 4242 4242 4242 4242, https://t.me t.me a@t.me /command @username
                        all the text above was on the same line

                        # Heading 1
                        ## Heading 2
                        ### Heading 3
                        #### Heading 4
                        ##### Heading 5
                        ###### Heading 6

                        Paragraph text

                        ```python
                          print('pre-formatted fixed-width code block written in the Python programming language')
                        ```

                        ---

                        - unordered list item
                        * unordered list item
                        + unordered list item

                        1. ordered list item
                        2. ordered list item

                        - [ ] task list item
                        - [x] completed task list item

                        >Block quotation started
                        >
                        >Block quotation continued on the next line
                        >Block quotation continued on the same line
                        >
                        >The last line of the block quotation

                        ![](https://telegram.org/example/photo.jpg)
                        ![](https://telegram.org/example/video.mp4)
                        ![](https://telegram.org/example/audio.mp3)
                        ![](https://telegram.org/example/audio.ogg)
                        ![](https://telegram.org/example/animation.gif)

                        ![](https://telegram.org/example/photo.jpg "Photo caption")
                        ![](https://telegram.org/example/video.mp4 "Video caption")
                        ![](https://telegram.org/example/audio.mp3 "Audio caption")
                        ![](https://telegram.org/example/audio.ogg "Voice note caption")
                        ![](https://telegram.org/example/animation.gif "Animation caption")

                        | Header 1 | Header 2 |
                        |:---------|:--------:|
                        | left     | center   |

                        Text with a reference[^id1] and another one[^id2].

                        [^id1]: Definition of the first footnote.
                        [^id2]: Definition of the second footnote.

                        $${'$'}E = mc^2$$

                        ```math
                        E = mc^2
                        ```

                        ## Example Nested Syntax Report for _Q1_
                        Intro with <u>underlined text</u>, ==marked text==, and ${'$'}x^2 + y^2$.
                        **Bold _italic <u>underlined italic bold</u> italic_ bold**
                        <u>In inline tags, nested **markdown** is parsed</u>
                        >Quote with **bold text, ~~strikethrough, and <tg-spoiler>spoiler</tg-spoiler>~~**, plus [a link](https://t.me/).

                        - List item with `code`, <sup>superscript</sup>, <sub>subscript</sub>, and a footnote[^note]
                        - Another item with **bold <tg-spoiler><code>spoiler code</code></tg-spoiler>**
                        - Another item with ~~strikethrough and <ins>inserted text</ins>~~

                        | Metric | Value |
                        |:-------|------:|
                        | Speed  | **42** <sup>ms</sup> |
                        | Status | <tg-spoiler>ready</tg-spoiler> |

                        [^note]: Footnote with _italic text_ and <u>HTML underline</u>.

                        ---

                        # Details blocks can contain Markdown content:

                        <details open><summary>Summary with **bold text**</summary>

                        ### Details heading
                        - List item with _italic text_
                        - List item with <tg-spoiler>spoiler</tg-spoiler>

                        </details>

                        # Collages and slideshows can contain Markdown media blocks:

                        <tg-collage>

                        ![](https://telegram.org/example/photo.jpg)
                        ![](https://telegram.org/example/video.mp4)

                        </tg-collage>

                        <tg-slideshow>

                        ![](https://telegram.org/example/photo.jpg)
                        ![](https://telegram.org/example/video.mp4)

                        </tg-slideshow>
                    """.trimIndent()
                )
            )
            println(sent)
        }

        // sendRichMessageDraft: stream partial rich messages sharing one draftId, then finalize
        // with a full sendRichMessage. Emulates streaming of an AI-generated reply.
        onCommand("rich_draft") {
            val chatId = it.chat.id.toChatId()
            val draftId = 1L
            val parts = listOf(
                "Thinking",
                "Thinking about *rich* messages",
                "Thinking about *rich* messages and how to _stream_ them"
            )
            parts.forEach { part ->
                sendRichMessageDraft(chatId, draftId, InputRichMessageMarkdown(part))
                delay(1000)
            }
            // finalize the streamed draft with the real message
            sendRichMessage(chatId, InputRichMessageMarkdown("Done! Here is the *final* rich message."))
        }

        // EditChatMessageRichText: send a rich message, then edit it with new rich content
        onCommand("rich_edit") {
            val sent = sendRichMessage(it.chat.id, InputRichMessageMarkdown("*Original* rich message"))
            delay(2000)
            execute(
                EditChatMessageRichText(
                    chatId = sent.chat.id,
                    messageId = sent.messageId,
                    // the new rich_message parameter of editMessageText
                    richMessage = InputRichMessageMarkdown("*Edited* rich message — now _updated_")
                )
            )
        }

        // waitRichMessage expectation: wait for the user to send a rich message
        onCommand("wait_rich") {
            reply(it, "Send me a rich message now")
            val richMessageContent = waitRichMessage().first()
            reply(
                it,
                "Got rich message with ${richMessageContent.richMessage.blocks.size} block(s)"
            )
        }

        // onRichMessage trigger: incoming messages carrying the new rich_message field
        onRichMessage { message ->
            val richMessage = message.content.richMessage
            println("=== Rich message received ===")
            println("  isRtl:  ${richMessage.isRtl}")
            println("  blocks: ${richMessage.blocks.size}")
            richMessage.blocks.forEachIndexed { index, block ->
                println("  [$index] $block")
            }
            reply(message, "Received a rich message with ${richMessage.blocks.size} block(s)")
            execute(
                message.content.createResend(
                    message.chat.id,
                )
            )
        }

        // InputRichMessageContent as InputMessageContent of inline query results
        onBaseInlineQuery { query ->
            answer(
                query,
                results = listOf(
                    InlineQueryResultArticle(
                        InlineQueryId("rich_html"),
                        "Rich message (HTML)",
                        // InputRichMessageContent wraps an InputRichMessage and is a valid InputMessageContent
                        InputRichMessageContent(
                            InputRichMessageHTML("<b>Bold</b> rich message sent via inline query")
                        ),
                        description = "InputRichMessageContent built from HTML"
                    ),
                    InlineQueryResultArticle(
                        InlineQueryId("rich_markdown"),
                        "Rich message (Markdown)",
                        InputRichMessageContent(
                            InputRichMessageMarkdown("*Bold* rich message sent via inline query")
                        ),
                        description = "InputRichMessageContent built from Markdown"
                    )
                ),
                cachedTime = 0
            )
        }

        // onlyRichMessageContentMessages: filter a Flow<ContentMessage<*>> down to rich message content
        allUpdatesFlow
            .mapNotNull { it.baseSentMessageUpdateOrNull() ?.data ?.contentMessageOrNull() }
            .onlyRichMessageContentMessages()
            .subscribeLoggingDropExceptions(scope = this) { richMessageContentMessage ->
                println("[onlyRichMessageContentMessages] ${richMessageContentMessage.content.richMessage.blocks.size} blocks")
            }

        setMyCommands(
            BotCommand("rich_html", "Send a rich message described with HTML"),
            BotCommand("rich_markdown", "Send a rich message described with Markdown"),
            BotCommand("rich_draft", "Stream a rich message draft, then finalize it"),
            BotCommand("rich_edit", "Send a rich message and edit it with new rich content"),
            BotCommand("wait_rich", "Wait for you to send a rich message"),
        )

        allUpdatesFlow.subscribeLoggingDropExceptions(scope = this) {
            println(it)
        }
    }.second.join()
}
