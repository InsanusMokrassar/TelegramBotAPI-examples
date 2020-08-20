suspend fun main(args: Array<String>) {
    activateResenderBot(args.first()) {
        println(it)
    }
}
