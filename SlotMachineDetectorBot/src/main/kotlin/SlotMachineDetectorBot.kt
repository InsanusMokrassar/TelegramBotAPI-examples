import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDice
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.dice.SlotMachineDiceAnimationType
import kotlinx.coroutines.*

suspend fun main(args: Array<String>) {
    val bot = telegramBot(args.first())

    bot.buildBehaviourWithLongPolling(scope = CoroutineScope(Dispatchers.IO)) {
        onDice {
            val content = it.content
            val dice = content.dice
            val diceType = dice.animationType

            if (diceType == SlotMachineDiceAnimationType) {
                val result = dice.calculateSlotMachineResult() ?: return@onDice
                bot.reply(it, "${result.leftReel}|${result.centerReel}|${result.rightReel}")
            } else {
                bot.reply(it, "There is no slot machine dice in message")
            }
        }
    }.join()
}
