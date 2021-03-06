import dev.inmo.micro_utils.coroutines.safely
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.shortcuts.filterContentMessages
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.longPolling
import dev.inmo.tgbotapi.types.dice.SlotMachineDiceAnimationType
import dev.inmo.tgbotapi.types.message.content.DiceContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

suspend fun main(args: Array<String>) {
    val bot = telegramBot(args.first())

    val scope = CoroutineScope(Dispatchers.Default)
    bot.longPolling(scope = scope) {
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