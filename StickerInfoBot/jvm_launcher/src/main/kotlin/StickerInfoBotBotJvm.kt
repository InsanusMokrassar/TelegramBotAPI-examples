suspend fun main(args: Array<String>) {
    activateStickerInfoBot(args.first()) {
        println(it)
    }
}
