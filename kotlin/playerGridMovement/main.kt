package playerGridMovement

import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)
import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.deg                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.scene.*                   // scene.* - Scene, defaultOrbitCamera, addColorMesh, lighting и т.д.
import de.fabmax.kool.modules.ksl.KslPbrShader  // KslPbrShader - готовый PBR-шейдер (материал)
import de.fabmax.kool.util.Color                // Color - цвет (RGBA)
import de.fabmax.kool.util.Time                 // Time.deltaT - сколько секунд прошло между кадрами
import de.fabmax.kool.pipeline.ClearColorLoad   // ClearColorLoad - режим: "не очищай экран, оставь то что уже нарисовано"
import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

// Flow корутины
import kotlinx.coroutines.launch                    // запуск корутин
import kotlinx.coroutines.flow.MutableSharedFlow    // радиостанция событий
import kotlinx.coroutines.flow.SharedFlow           // чтение для подписчиков
import kotlinx.coroutines.flow.MutableStateFlow     // табло состояний
import kotlinx.coroutines.flow.StateFlow            // только для чтения
import kotlinx.coroutines.flow.asSharedFlow         // отдать наружу только SharedFlow
import kotlinx.coroutines.flow.asStateFlow          // отдать только StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.flatMapLatest


enum class QuestState {
    START,
    WAIT_HERB,
    GOOD_END,
    EVIL_END
}

enum class Facing {
    LEFT,
    RIGHT,
    FORWARD,
    BACK
}

// -= Типы объектов игрового мира
enum class WorldObjectType{
    ALCHEMIST,
    HERB_SOURCE,
}

data class GridPos(
    val x: Int,
    val y: Int
)



// -= Описание объектов в игрровом мире
data class WorldObjectDef(
    val id: String,
    val type: WorldObjectType,
    val x: Float,
    val z: Float,
    val interactRadius: Float
)

data class NpcMemory(
    val hasMet: Boolean, // -= встретил или нет
    val timesTalked: Int, // -= Сколько раз уже поговорил
    val receivedHerb: Boolean,
    val sawPlayerNearSource: Boolean = false,
    val isStopped: Boolean = false,
    val posX: Float = 3f,
    val posZ: Float = 3f
)

data class PlayerState(
    val playerId: String,
    val gridX: Int,
    val gridZ: Int,
    val questState: QuestState,
    val inventory: Map<String, Int>, // -= примитивный словарь
    val gold: Int,

    val alchemistMemory: NpcMemory,
    val currentAreaId: String?,
    val hintText: String, // -= Подсказка что делать и тп

    val facing: Facing
)


// -=-=-= Вспомогательные функции =-=-=-
fun herbCount(player: PlayerState): Int{ //> даёт количество herb
    return player.inventory["herb"] ?: 0
}

fun facingToYawDeg(facing: Facing): Float{
    // Превращает направление в угол поворота по оси Y
    //> Нужно для визуального отображения поворота куба

    return when (facing){
        Facing.FORWARD -> 0f
        Facing.RIGHT -> 90f
        Facing.BACK -> 180f
        Facing.LEFT -> 270f
    }
}

fun lerp(current: Float, target: Float, t: Float): Float{
    // Линейная интерполяция
    //> Простыми словами - нужна для плавного перемещения current в сторону target
    //>> Формула = current + (target - current) * t
    return current + (target - current) * t
}

fun distance2d(ax: Float, az: Float, bx: Float, bz: Float): Float{
    // sqrt((dx * dx) + (dz * dz))
    val dx = ax - bx
    val dz = az - bz
    return kotlin.math.sqrt(dx*dx + dz*dz)
}

fun initialPlayerState(playerId: String): PlayerState {
    return if(playerId == "Stas"){
        PlayerState(
            "Stas",
            0,
            0,
            QuestState.START,
            emptyMap(),
            0,
            NpcMemory(
                false,
                0,
                false
            ),
            null,
            "Подойди к одной из локаций",
            Facing.FORWARD
        )
    }else{
        PlayerState(
            "Oleg",
            0,
            0,
            QuestState.START,
            emptyMap(),
            0,
            NpcMemory(
                false,
                0,
                false
            ),
            null,
            "Подойди к одной из локаций",
            Facing.FORWARD
        )
    }
}


data class DialogueOption(
    val id: String,
    val text: String
)

data class DialogueView(
    val npcId: String,
    val text: String,
    val options: List<DialogueOption>
)

fun buildAlchemistDialogue(player: PlayerState): DialogueView{
    val herbs = herbCount(player)
    val memory = player.alchemistMemory

    return when(player.questState){
        QuestState.START -> {
            val greeting =
                if (!memory.hasMet) {
                    "Привет, ты кто"
                }else{
                    "Ну что ${player.playerId} я жду?!"
                }
            DialogueView(
                "Алхимик",
                "$greeting\nТащи траву",
                listOf(
                    DialogueOption("accept_help", "Акей"),
                    DialogueOption("threat", "Нит, ты давай")
                )
            )
        }
        QuestState.WAIT_HERB -> {
            if (herbs < 3) {
                DialogueView(
                    "Алхимик",
                    "Мало, мне надо 4 вщто",
                    emptyList()
                )
            }else{
                DialogueView(
                    "Алхимик",
                    "спс",
                    listOf(
                        DialogueOption("give_herb", "Отдать 4 травы")
                    )
                )
            }
        }
        QuestState.GOOD_END -> {
            val text =
                if (memory.receivedHerb){
                    "Ну что, похимичим?!"
                }else{
                    "Ты завершил квест, но память не обновилась, капут"
                }
            DialogueView(
                "Алхимик",
                text,
                emptyList()
            )
        }
        QuestState.EVIL_END -> {
            DialogueView(
                "Алхимик",
                "я с тобой больше не дружу",
                emptyList()
            )
        }
    }
}


sealed interface GameCommand{
    val playerId: String
}
data class CmdMovePlayer(
    override val playerId: String,
    val stepX: Int,
    val stepZ: Int
): GameCommand

data class CmdInteract(
    override val playerId: String
): GameCommand

data class CmdChooseDialogueOption(
    override val playerId: String,
    val optionId: String
): GameCommand

data class CmdResetPlayer(
    override val playerId: String
): GameCommand

sealed interface GameEvent{
    val playerId: String
}

data class PlayerMoved(
    override val playerId: String,
    val nextGridX: Int
    val nextGridY: Int
): GameEvent

data class MovedBlocked(
    override val playerId: String,
    val blockedX: Int,
    val blockedZ: Int
): GameEvent

data class EnteredArea(
    override val playerId: String,
    val areaId: String
): GameEvent

data class LeftArea(
    override val playerId: String,
    val areaId: String
): GameEvent

data class InteractedWithNpc(
    override val playerId: String,
    val npcId: String
): GameEvent

data class InteractedWithHerbSource(
    override val playerId: String,
    val sourceId: String
): GameEvent

data class InventoryChanged(
    override val playerId: String,
    val itemId: String,
    val newCount: Int
): GameEvent

data class QuestStateChanged(
    override val playerId: String,
    val newState: QuestState
): GameEvent

data class NpcMemoryChanged(
    override val playerId: String,
    val memory: NpcMemory
): GameEvent

data class ServerMessage(
    override val playerId: String,
    val text: String
): GameEvent

data class CutSceneStarted(
    override val playerId: String,
    val cutsceneId: String
): GameEvent

data class CutSceneStep(
    override val playerId: String,
    val text: String
): GameEvent

data class CutSceneFinished(
    override val playerId: String,
    val cutsceneId: String
): GameEvent
