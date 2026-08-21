import kotlinx.coroutines.runBlocking

/**
 * Starts the native resender launcher with the first element of [args] as its bot
 * token. Later elements are ignored.
 */
fun main(vararg args: String) {
    runBlocking {
        activateResenderBot(args.first()) {
            println(it)
        }
    }
}
