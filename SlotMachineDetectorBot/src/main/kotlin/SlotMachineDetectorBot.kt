import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.shortcuts.filterContentMessages
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import dev.inmo.tgbotapi.types.dice.SlotMachineDiceAnimationType
import dev.inmo.tgbotapi.types.message.content.DiceContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

suspend fun main(args: Array<String>) {
    val bot = telegramBot(args.first())

    val scope = CoroutineScope(Dispatchers.Default)
    bot.startGettingFlowsUpdatesByLongPolling(scope = scope) {
        filterContentMessages<DiceContent>(scope).onEach {
            val content = it.content
            val dice = content.dice
            val diceType = dice.animationType

            safely ({ it.printStackTrace() }) {
                if (diceType == SlotMachineDiceAnimationType) {
                    val result = dice.calculateSlotMachineResult() ?: return@safely
                    bot.reply(it, "${result.leftReel}|${result.centerReel}|${result.rightReel}")
                } else {
                    bot.reply(it, "There is no slot machine dice in message")
                }
            }
        }.launchIn(scope)
    }

    scope.coroutineContext[Job]!!.join()
}