import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath

/** Kotlin/Native entry point; [args] contains the bot token followed by an optional picker root. */
fun main(args: Array<String>) {
    runBlocking {
        doRandomFileSenderBot(args.first(), args.getOrNull(1) ?.toPath() ?: "".toPath())
    }
}
