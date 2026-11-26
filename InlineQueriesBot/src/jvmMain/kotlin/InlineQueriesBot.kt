/** JVM entry point; [args] must contain the bot token as its first element. */
suspend fun main(args: Array<String>) {
    doInlineQueriesBot(args.first(), configureInline = "configureInline" in args)
}
