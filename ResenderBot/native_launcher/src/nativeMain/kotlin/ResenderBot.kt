import kotlinx.coroutines.runBlocking

fun main(vararg args: String) {
    runBlocking {
        activateResenderBot(args.first()) {
            println(it)
        }
    }
}
