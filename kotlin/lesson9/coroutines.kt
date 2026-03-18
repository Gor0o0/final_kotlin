package lesson9

import de.fabmax.kool.KoolApplication               // Запускает Kool-приложение
import de.fabmax.kool.addScene                      // функция - добавить сцену (UI, игровой мир и тд)
import de.fabmax.kool.math.Vec3f                    // 3D - вектор (x,y,z)
import de.fabmax.kool.math.deg                      // deg - превращение числа в градусы
import de.fabmax.kool.scene.*                       // Сцена, камера, источники света и тд
import de.fabmax.kool.modules.ksl.KslPbrShader      // готовый PBR Shader - материал
import de.fabmax.kool.modules.ksl.KslPbrSplatShader
import de.fabmax.kool.util.Color                    // Цветовая палитра
import de.fabmax.kool.util.Time                     // Время deltaT - сколько прошло секунд между двумя кадрами
import de.fabmax.kool.pipeline.ClearColorLoad       // Режим говорящий не очищать экран от элементов (нужен для UI)
import de.fabmax.kool.modules.ui2.*                 // импорт всех компонентов интерфейса, вроде text, button, Row....
import de.fabmax.kool.modules.ui2.UiModifier.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

// -=-=-=-=-= Корутины =-=-=-=-=- //
import kotlinx.coroutines.launch                    // Запускает корутину
import kotlinx.coroutines.job                       // контроллер управления корутиной
import kotlinx.coroutines.isActive                  // проверка жива ли ещё корутина - полезно для циклов
import kotlinx.coroutines.delay                     // заставляет корутинку ждать ограниченное число времени но не замораживает саму игру

// В любой игре есть много процессов опирающихся на время
// Например:
//> Яд тикает 1 раз в секунду
//> Кд удара 1.5 секунды
//> Задержка сети 300мс
//> Квест с событием открывает дверь через 5 секунд
//> и тд

// Если все эти процессы делать через onUpdate и таймер вручную - это быстро превращается в кашу

// Корутины решают эту проблему
//> 1. Позволяют писать время как обычный код: подождал -> сделал действие -> подождал -> действие
//> 2. В Процессе выполнения не замораживает игру и UI
//> 3. Удобно отменяются (напримр яд перезапускается если наложили новый а старый яд отменяем

// Корутина - легковесная задача котопая может выполняться параллельно другим задачам и основному потоку

// Основные команды корутин:
//> launch{ ... } - запускает корутинку (включить поток)
//> delay(ms) - заставляет корутинку ждать ограниченное число времени но не замораживает саму игру
//> job + cancel() | job - контроллер управления корутиной | cancel() - остановить выполнние корутины (например снять эффект яда)

// Функция delay не будет работать за пределами корутины launch
// Потому что delay это suspend-функция

// suspend fun - функция которая может приостанавливаться (ждать) - обычные функции так не умеют
// suspen функцию можно вызвать только внутри запущенной корутины или внуттри такой же suspen функции

// scene.coroutineScope - это свой корутинный скоуп Kool внутри сцены, для чего?
// Когда сцена будет закрываться - корутины внутри этой сцены автоматичски прекратятся
// Это просто безопасее чем глобальные корутины про которое мы можем забыть или не сохранить

class GameState{
    val playerId = mutableStateOf("Oleg")

    val hp = mutableStateOf(100)
    val maxHp = 100

    val poisonTicksLeft = mutableStateOf(0)
    val regenTicksLeft = mutableStateOf(0)

    val attackCoolDownMsLeft = mutableStateOf(0L)

    val logLines = mutableStateOf<List<String>>(emptyList())
}

fun pushLog(game: GameState, text: String){
    game.logLines.value = (game.logLines.value + text).takeLast(20)
}

// -=-=-= EffectManager - система для эффектов по времени =-=-=-
class EffectManager(
    private val game: GameState,
    private val scope: kotlinx.coroutines.CoroutineScope,
    //> scope - место где и будут запускаться и жить корутины
    //> передаём сюда scene.coroutineScope чтобы всё было привязано к сцене
){
    private var poisonJob: Job? = null
    //> Job - это задача-корутина

    private var regenJob: Job? = null

    fun applyPoison(ticks: Int, damagePerTick: Int, intervalMs: Long) {
        poisonJob?.cancel()
        // если яд уже был применен - анулируем его (отменяем)
        // ?. - безопасный вывод, значит, если poisonJob == null то cancel не вызовется

        // обновляем состояние игроого числа тиков яда (добавляем к счётчику)
        game.poisonTicksLeft.value += ticks

        poisonJob = scope.launch {
            // Запуск корутинунанесения урона от яда

            // -= нужно =-
            // создать цикл while который проверит активна ли корутина и что счётчик тиков больше 0
            //> внутри цикла задержка
            //> отнятие одного тика от состояния тиков яда
            //> и отнятие здоровья с условием что оно не упадёт ниже 0
            //> публикуем лог о нанесении урона от яда

            while (isActive && game.poisonTicksLeft.value > 0) {
                delay(intervalMs)
                game.poisonTicksLeft.value -= 1

                game.hp.value = (game.hp.value - damagePerTick).coerceAtLeast(0)

                pushLog(game, "тик яда: -$damagePerTick, HP: ${game.hp.value}")

            }

            pushLog(game, "Яд нейтрализован")
        }
    }

    fun applyRegen(ticks: Int, healPerTick: Int, intervalMs: Long) {
        regenJob?.cancel()

        game.regenTicksLeft.value += ticks
        pushLog(game, "Регенерация активна на ${game.playerId}, длительность ${intervalMs}")

        regenJob = scope.launch {
            while (isActive && game.regenTicksLeft.value > 0) {
                delay(intervalMs)

                game.regenTicksLeft.value -= 1
                game.hp.value = (game.hp.value + healPerTick).coerceAtMost(game.maxHp)
                pushLog(game, "тик регена: +$healPerTick, HP: ${game.hp.value}/${game.maxHp}")
            }
            pushLog(game, "регенерация окончена")
        }
    }

    fun cancelPoison() {
        poisonJob?.cancel()
        poisonJob = null
        game.poisonTicksLeft.value = 0
        pushLog(game, "Яд снят")
    }
    fun cancelRegen() {
        regenJob?.cancel()
        regenJob = null
        game.regenTicksLeft.value = 0
        pushLog(game, "Регенерация прервана")
    }
}

class CooldownManager(
    private val game: GameState,
    private val scope: kotlinx.coroutines.CoroutineScope
){
    private var cooldownJob: Job? = null

    fun startAttackCooldown(totalMs: Long){
        cooldownJob?.cancel()

        game.attackCoolDownMsLeft.value = totalMs
        pushLog(game, "КД атаки ${totalMs}мс")

        cooldownJob = scope.launch {
            val step = 100L

            while (isActive && game.attackCoolDownMsLeft.value > 0) {
                delay(step)
                game.attackCoolDownMsLeft.value = (game.attackCoolDownMsLeft.value - step).coerceAtLeast(0)

            }
        }
    }

    fun canAttack(): Boolean {
        return game.attackCoolDownMsLeft.value <= 0L
    }
}

fun main() = KoolApplication{
    val game = GameState()

    addScene {
        defaultOrbitCamera()
        addColorMesh {
            generate { cube { colored() } }

            shader = KslPbrShader {
                color { vertexColor() }
                metallic(0.7f)
                roughness(0.4f)
            }
            3
            onUpdate {
                transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
            }
        }
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }
        val effect = EffectManager(game, coroutineScope)
        val cooldowns = CooldownManager(game, coroutineScope)

        SharedActions.effects = effect
        SharedActions.cooldown = cooldowns
    }

    addScene {
        setupUiScene(ClearColorLoad)

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(16.dp)
                .background(RoundRectBackground(Color(0f,0f,0f,0.6f), 14.dp))
                .padding(12.dp)

            Column{
                Text("HP: ${game.hp.use()}"){}
                Text("Тики яда: ${game.poisonTicksLeft.use()}"){
                    modifier.margin(bottom = sizes.gap)
                }
                Text("Тики регена: ${game.regenTicksLeft.use()}"){
                    modifier.margin(bottom = sizes.gap)
                }
                Text("Тики кд: ${game.attackCoolDownMsLeft.use()}"){
                    modifier.margin(bottom = sizes.gap)
                }

                Row{
                    Button ("Яд +5"){
                        modifier.margin(end=8.dp).onClick{
                            SharedActions.effects?.applyPoison(5,2,1000L)
                        }
                    }

                    Button ("Отмена яда"){
                        modifier.margin(end=8.dp).onClick{
                            SharedActions.effects?.cancelPoison()
                        }
                    }
                }

                Row{
                    modifier.margin(top = sizes.smallGap)
                    Button ("Реген +5"){
                        modifier.margin(end=8.dp).onClick{
                            SharedActions.effects?.applyRegen(5,2,1000L)
                        }
                    }

                    Button ("Отмена регена"){
                        modifier.margin(end=8.dp).onClick{
                            SharedActions.effects?.cancelRegen()
                        }
                    }
                }
                Row{
                    modifier.margin(top = sizes.smallGap)
                    Button ("Атаковать (кд 1200мс"){
                        modifier.margin(end=8.dp).onClick{
                            val cd = SharedActions.cooldown

                            if (cd == null){
                                pushLog(game, "CooldownManager ещё не готов")
                                return@onClick
                            }
                            if (!cd.canAttack()){
                                pushLog(game, "Атаковать нельзя: кд")
                                return@onClick
                            }

                            cd.startAttackCooldown(totalMs = 1200L)
                        }
                    }
                }

                Text("Логи: "){
                    modifier.margin(top = sizes.gap)
                }

                val lines = game.logLines.use()
                for (line in lines) {
                    Text(line){modifier.font(sizes.smallText)}
                }
            }
        }
    }
}

// -=-=-= Shared Actions | Мост между сценами =-=-=-

object SharedActions{
    var effects: EffectManager? = null
    var cooldown: CooldownManager? = null
}