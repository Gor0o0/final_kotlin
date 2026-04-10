package playerMovement

//-=-=| База сцен
import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)
import de.fabmax.kool.input.KeyCode
import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.deg                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.scene.*                   // scene.* - Scene, defaultOrbitCamera, addColorMesh, lighting и т.д.
import de.fabmax.kool.modules.ksl.KslPbrShader  // KslPbrShader - готовый PBR-шейдер (материал)
import de.fabmax.kool.util.Color                // Color - цвет (RGBA)
import de.fabmax.kool.util.Time                 // Time.deltaT - сколько секунд прошло между кадрами
import de.fabmax.kool.pipeline.ClearColorLoad   // ClearColorLoad - режим: "не очищай экран, оставь то что уже нарисовано"
import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf
import de.fabmax.kool.modules.ui2.UiModifier.*

//-=-=| База корутин
import kotlinx.coroutines.launch                    // запуск корутин
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

//-=-=| Flow корутины
import kotlinx.coroutines.flow.MutableSharedFlow    // радиостанция событий
import kotlinx.coroutines.flow.SharedFlow           // чтение для подписчиков
import kotlinx.coroutines.flow.asSharedFlow         // отдать наружу только SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow     // табло состояний
import kotlinx.coroutines.flow.StateFlow            // только для чтения
import kotlinx.coroutines.flow.asStateFlow          // отдать только StateFlow

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn

//-=-=| Особые импорты
import java.awt.KeyEventDispatcher      // Перехватчик событий с клавиатуры
import java.awt.KeyboardFocusManager    // Диспетчер фокуса окна
import java.awt.event.KeyEvent          // Само событие нажатия на клавишу

import kotlin.math.abs          // Модуль числа
import kotlin.math.atan2        // Угол по X/Z
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max          // Выбор большего из двух чисел
import kotlin.math.sqrt         // Квадрат числа

// -=-=|Реализация
// Движение игрока с помощью назначенных клавиш на клавиатуре
// Свободное перемещение а не по клеткам
// Поворот игрока по направлению движения
// Тестовый объект для взаимодействия игрока с ним
// follow-camera эффект чтобы игрок оставался в центре сцены на экране
// -=-=-=-

//-=-=| Desktop Keyboard Bridge |=-=-
//> Слушаем нажатие клавиш через AWT и каждый кадр читаем текущее состояние клавиатур
object DesktopKeyBoardState{
    private val pressedKeys = mutableSetOf<Int>() //|> SetOf чтоб не было повторений
    //> Набор кодов клавиш которые сейчас зажаты

    private val justPressedKeys = mutableSetOf<Int>()
    //> Набор клавиш которые были нажаты только 1 раз

    private var isInstalled = false //> Проверка установленного диспатчера | флаг-подсказка чтоб не устанавливать один и тот же dispatcher дважды

    // Метод установки перехватчика клавиатуры
    fun install(){
        if (isInstalled) return

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(
                object : KeyEventDispatcher {
                    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
                        when(e.id){
                            KeyEvent.KEY_PRESSED ->{
                                //> Если клавиша ранее не была зажата, значит это "новое нажатие"
                                //>> и можно добавить её в justPressedKeys
                                if(pressedKeys.contains(e.keyCode)){
                                    justPressedKeys.add(e.keyCode)
                                }
                                pressedKeys.add(e.keyCode)
                            }
                            KeyEvent.KEY_RELEASED ->{
                                //> Когда клавишу отпускают - удаляем её из общих наборов клавиш
                                pressedKeys.remove(e.keyCode)
                                justPressedKeys.remove(e.keyCode)
                            }
                        }
                        return false //> не блокировать дальнейшую обработку
                    }
                }
            )
        isInstalled = true
    }

    fun isDown(keyCode: Int): Boolean{
        //> Проверка зажата ли клавиша прямо сейчас
        return keyCode in pressedKeys
    }

    fun consumeJustPressed(keyCode: Int): Boolean{
        //> Один раз поймать новое нажатие
        //>> т.е. если клавиша есть в ustPressedKeys - тогда вернём true и сразу удалим её оттуда
        //>>> Так клавиши требующие одиночног взаимодействия, будут работать правильно а не как полемет

        return if(keyCode in justPressedKeys){
            justPressedKeys.remove(keyCode)
            true
        }else{false}
    }
}

enum class QuestState{
    START,
    WAIT_HERB,
    GOOD_END,
    EVIL_END
}
enum class WorldObjectType{
    ALCHEMIST,
    HERB_SOURCE,
    CHEST,
    DOOR
}

data class WorldObjectDef(
    val id: String,
    val type: WorldObjectType,
    val worldX: Float,
    val worldZ: Float,
    val interactRadius: Float
)

data class ObstacleDef(
    val centerX: Float,
    val centerZ: Float,
    val halfSize: Float //> Половина размера квадрата препятствия
)

data class NpcMemory(
    val hasMet: Boolean,
    val timesTalked: Int,
    val receivedHerb: Boolean
)

data class PlayerState(
    val playerId: String,
    val worldX: Float,
    val worldZ: Float,

    val yawDeg: Float,   //> Куда смотрит игрок (угол по y)

    val moveSpeed: Float,
    val questState: QuestState,
    val inventory: Map<String, Int>,
    val gold: Int,

    val alchemistMemory: NpcMemory,
    val chestLooted: Boolean,
    val doorOpened: Boolean,

    val currentFocusId: String?, //> На кого смотрит игрок из доступных для взаимодействия
    val pinnedQuestEnable: Boolean,
    val pinnedTargetId: String
)