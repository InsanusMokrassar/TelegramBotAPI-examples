import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDice
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.dice.SlotMachineDiceAnimationType
import kotlinx.coroutines.*

/**
 * Starts the long-polling slot-machine detector.
 *
 * [args] must contain the bot token first; later elements are ignored. Slot-machine
 * dice receive a pipe-separated three-reel reply, while other dice receive an
 * explanatory reply. An undecodable slot-machine result is silently ignored.
 */
suspend fun main(args: Array<String>) {
    val bot = telegramBot(args.first())

    bot.buildBehaviourWithLongPolling(scope = CoroutineScope(Dispatchers.IO)) {
        onDice {
            val content = it.content
            val dice = content.dice
            val diceType = dice.animationType

            if (diceType == SlotMachineDiceAnimationType) {
                val result = dice.calculateSlotMachineResult() ?: return@onDice
                reply(it, "${result.leftReel}|${result.centerReel}|${result.rightReel}")
            } else {
                reply(it, "There is no slot machine dice in message")
            }
        }
    }.join()
}
