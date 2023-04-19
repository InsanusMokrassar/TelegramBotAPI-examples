import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath

fun main(args: Array<String>) {
    runBlocking {
        doRandomFileSenderBot(args.first(), args.getOrNull(1) ?.toPath() ?: "".toPath())
    }
}
