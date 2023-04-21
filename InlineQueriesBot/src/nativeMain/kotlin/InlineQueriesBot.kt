import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        doInlineQueriesBot(args.first())
    }
}
