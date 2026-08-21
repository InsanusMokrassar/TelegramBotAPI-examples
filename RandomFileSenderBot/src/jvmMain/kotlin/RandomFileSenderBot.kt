import dev.inmo.micro_utils.common.MPPFile

/** JVM entry point; [args] contains the bot token followed by an optional picker root. */
suspend fun main(args: Array<String>) {
    doRandomFileSenderBot(args.first(), MPPFile(args.getOrNull(1) ?: ""))
}
