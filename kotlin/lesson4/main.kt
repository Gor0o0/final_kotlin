package lesson4

import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)
import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.*                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.scene.*                   // scene.* - Scene, defaultOrbitCamera, addColorMesh, lighting и т.д.
import de.fabmax.kool.modules.ksl.KslPbrShader  // KslPbrShader - готовый PBR-шейдер (материал)
import de.fabmax.kool.util.Color                // Color - цвет (RGBA)
import de.fabmax.kool.util.Time                 // Time.deltaT - сколько секунд прошло между кадрами
import de.fabmax.kool.pipeline.ClearColorLoad   // ClearColorLoad - режим: "не очищай экран, оставь то что уже нарисовано"
import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf
import jdk.jfr.internal.consumer.EventLog
import lesson2.Item
import lesson2.ItemStack
import lesson2.ItemType
import lesson3.WOOD_SWORD

enum class ItemType{
    WEAPON,
    ARMOR,
    POTION
}


class  GameState{
    val playerId = mutableStateOf("Player")
    val hp = mutableStateOf(100)
    val gold = mutableStateOf(0)
    val poisonTicksLeft = mutableStateOf(0)
    val regenTicksLeft = mutableStateOf(0)
    val dummyHp = mutableStateOf(50)
    val hotbar = mutableStateOf(
        List<ItemStack?>(9){null}
        //список из 9 пустых ячеек хотбара
    )
    val selectedSlot = mutableStateOf(0)

    val eventLog = mutableStateListOf<List<String>>(emptyList())
}
val HEALING_POTION = Item(
    "potion_heal",
    "Healing Potion",
    ItemType.POTION,
    12
)

val WOOD_SWORD = Item(
    "sword_wood",
    "Wood sword",
    ItemType.WEAPON,
    1
)

// Наша игра будет состоять из связки
// Event System -> Quest System -> HUB log + progress
// Почему это вообще надо
// Сейчас кнопки напрямую меняют состония hp, hotbar, dummyHp
// Если бы мы остановились при написании игры на этой систме то:
// 1. Кнопка удар, напрямую бы вычитала Hp у моба
// 2. Квесты, NPC и тд не знали бы что удар по мобу произошёл
// 3. Система сохраений не знала бы что шаг произошёл и его надо зафиксировать
// События решают проблему: кнопка\логика пуюликует "произошло Х"? а другие системы(npc, log, quest)
// Подписаны и в зависимости от внутренней логики - реагируют на эти события

// Система событий
// Создаём интерфейс что-бы все наши события имели playerId
sealed interface GameEvent{
    val playerId: String
}

// События для квестов и логов
// data class - просто удобство, он хранит даныве как пакет и автоматически применяет toString

data class ItemAdded(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int,
    val leftOver: Int
): GameEvent

data class ItemUsed(
    override val playerId: String,
    val itemId: String,
): GameEvent

data class DamageDealt(
    override val playerId: String,
    val targetId: String,
    val amount: Int
): GameEvent

data class EffectApplied(
    override val playerId: String,
    val effectId: String,
    val tick: Int
): GameEvent

data class QuestStepCompleted(
    override val playerId: String,
    val questId: String,
    val stepIndex: Int
): GameEvent

class EventBus{
    typealias Listener = (GameEvent) -> Unit
    // Функция принимающая GameEvent возвращат пустоту (ничего по умолчанию)

    private val listeners = mutableListOf<Listener>()

    fun subscribe(listener: Listener){
        listeners.add(listener)
    }

    fun publish(event: GameEvent){
        for(listener in listeners){
            listener(event)
        }
    }
}

// Квестовая система

class QuestSystem(
    private val bus: EventBus // шина событий - через неё будет подписываться и читать события
){
    val questId = "q_training"

    val progressByPlayer = mutableStateOf<Map<String, Int>>(emptyMap())

    init {
        bus.subscribe { event ->
            handleEvent(event)
        }
    }

    private fun getStep(playerId: String): Int{
        return progressByPlayer.value[playerId] ?: 0
        // ?: - если ключа не найлется вернуть 0 (вместо null)
    }

    private fun setStep(playerId: String, step: Int){
        val newMap = progressByPlayer.value.toMutableMap()
        //Создаём новый словать чтобы состоние изменилось и UI его прочитал
        newMap[playerId] = step
        progressByPlayer.value = newMap.toMap()
    }

    private fun completeStep(playerId: String, stepIndex: Int){
        setStep(playerId, stepIndex + 1)
        // Публикует событие "шаг квеста выполнен"
        bus.publish(
            QuestStepCompleted(
                playerId,
                questId,
                stepIndex
            )
        )
    }

    private fun handleEvent(event: GameEvent){
        // Решаем влияет ли событие на квест (реагирует ли на событие)
        val player = event.playerId
        val step = getStep(player)

        // Если квест уже выполен уже выполнен
        if(step >= 2) return

        when(event){
            is ItemAdded -> {
                // Шаг квеста: 0
                if (step == 0 && event.itemId == WOOD_SWORD.id){
                    completeStep(player, 0)
                }
            }

            is DamageDealt -> {
                // шаг квеста 1 ударить манекен мечом
                if (step == 1 && event.targetId == "dummy" && event.amount >=10){
                    completeStep(player, 1)
                }
            }
            else -> {}
        }

    }
}

// Функции инвентаря


fun putIntoSlot(
    slots: List<ItemStack?>,  // принимает текущие слоты инвентаря
    slotIndex: Int,   // Индекс слота, в который мы кладём предмет
    item: Item,
    addCount: Int
): Pair<List<ItemStack?>, Int>{
    // Pair передаёт:
    // 1 - новый изменённый список слотов (но с уже положенным в него предметом)
    // 2 - число, сколько предметов НЕ ВЛЕЗЛО В ЯЧЕЙКУ(остаток)

    val newSlots = slots.toMutableList()
    // копия списка для изменений

    val current =  newSlots[slotIndex]
    // current - сохраняем, информацию о том что сейчас лежит в слоте
    if (current == null){
        val countToPlace = minOf(addCount, item.maxStack)
        //
        newSlots[slotIndex] = ItemStack(item, countToPlace)

        val leftOver = addCount - countToPlace
        // сколько ещё предметов не влезло
        return Pair(newSlots, leftOver)
    }
    // Если слот не пустой и предмет, что в нем лежит совпадает по id с тем, который мы в него кладём
    // и если maxStack > 1
    if (current.item.id == item.id && item.maxStack > 1){
        val freeSpace = item.maxStack - current.count
        val toAdd = minOf(addCount, freeSpace)

        newSlots[slotIndex] = ItemStack(item, current.count + toAdd)

        val leftOver = addCount - toAdd
        return Pair(newSlots, addCount)

    }
    return Pair(newSlots, addCount)
    // Если предмет ни один не стакается - ничего не меняем, возвращаем всё как было

}

fun useSelected(
    slots: List<lesson2.ItemStack?>,
    slotIndex: Int
): Pair<List<lesson2.ItemStack?>, lesson2.ItemStack?>{
    //Pair - создаёт пару значений (новые слоты, и что использовали уже в слотах)
    val  newSlots = slots.toMutableList()
    val current = newSlots[slotIndex] ?: return  Pair(newSlots, null)

    val newCount = current.count - 1

    if(newCount <= 0){
        //Если слот после использования предмета стал пуст
        newSlots[slotIndex] = null
    }else{
        newSlots[slotIndex] = ItemStack(current.item, newCount)
        // Eсли после использования предмета стак не закончился - обновляем стак
    }
    return Pair(newSlots, current)
}

fun pushLog(game: GameState, text: String){
    val old = ggame.eventLog.value

    val updated = old + text

    game.eventLog.value = updated.takeLast(20)
    // takeLast - обрезает список и оставляет только n строк