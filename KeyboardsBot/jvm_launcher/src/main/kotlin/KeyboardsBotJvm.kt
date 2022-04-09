import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun main(args: Array<String>) {
    withContext(Dispatchers.IO) { // IO for inheriting of it in side of activateKeyboardsBot
        activateKeyboardsBot(args.first()) {
            println(it)
        }
    }
}
