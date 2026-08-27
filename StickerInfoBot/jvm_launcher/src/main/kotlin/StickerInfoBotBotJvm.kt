/**
 * Runs [activateStickerInfoBot] on the JVM and prints its startup bot information to standard output.
 *
 * @param args the Telegram bot token as the first element; additional elements are ignored
 */
suspend fun main(args: Array<String>) {
    activateStickerInfoBot(args.first()) {
        println(it)
    }
}
