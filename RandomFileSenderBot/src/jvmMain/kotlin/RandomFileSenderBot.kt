import dev.inmo.micro_utils.common.MPPFile

suspend fun main(args: Array<String>) {
    doRandomFileSenderBot(args.first(), MPPFile(args.getOrNull(1) ?: ""))
}
