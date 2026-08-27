import kotlinx.coroutines.runBlocking

/** Kotlin/Native entry point; [args] must contain the bot token as its first element. */
fun main(args: Array<String>) {
    runBlocking {
        doInlineQueriesBot(args.first())
    }
}
