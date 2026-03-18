package lesson11

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
import de.fabmax.kool.modules.ui2.UiModifier.*

import kotlinx.coroutines.launch  // запускает корутину
import kotlinx.coroutines.Job     // контроллер запущенной корутины
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive // проверка жива ли ещё корутина - полезно для циклов
import kotlinx.coroutines.delay

// Flow корутины
import kotlinx.coroutines.flow.MutableSharedFlow // табло состояний
import kotlinx.coroutines.flow.SharedFlow // Только чтение для подписчиков
import kotlinx.coroutines.flow.MutableStateFlow // Радиостанция событий
import kotlinx.coroutines.flow.StateFlow // Только для чтения стостояний
import kotlinx.coroutines.flow.asSharedFlow // Отдать наружу только SharedFlow
import kotlinx.coroutines.flow.asStateFlow // Отдать только StateFlow
import kotlinx.coroutines.flow.collect // Слушать поток

import kotlinx.coroutines.flow.filter // Оставляет в потоке только то что подходит по условию
import kotlinx.coroutines.flow.map  // Преобразует каждый элемент потока (например GameEvent -> String для логирования)
import kotlinx.coroutines.flow.onEach  // Делает нужное действие для каждого элемента в потоке но не изменяет сам поток
import kotlinx.coroutines.flow.launchIn  // Запускает слушателя на фоне в нужном пространстве работы корутин
import kotlinx.coroutines.flow.flatMapLatest // Нужно для переключения игроков

// импорты Serialization
import kotlinx.serialization.Serializable // Анотация, что можно сохранять
import kotlinx.serialization.json.Json // Формат файла Json

import java.io.File

// когда событий становится слишком много появляется проблема:
// 1. Если все системы слушают все события код быстро превратится в кашу
// 2. Будет сложно понять кто на что реагирует из систем
// 3. Такие системы сложно дебажит (например почему квест не изменил состояние)
// 4. и так же надо жестко разделять события игрока Oleg от событий игрока Stas

// Для исправления данных проблем надо использовать flow-операторы
// filter - оставляет в потоке только то что подходит по условию
// map - преобразует каждый элемент потока (например GameEvent -> String для логирования)
// onEach - делает нужное действие для каждого элемента в потоке но не изменяет сам поток
// launchIn (scope) - запускает слушателя на фоне в нужном пространстве работы корутин
// flatMapLatest - нужно для переключения игроков.
//> Существует поток playerId, каждый раз когда мы меняем игрока, будет переключатся на новый поток событий
//> При этом забыв старый

@Serializable
data class PlayerSave(
    val playerId: String,
    val hp: Int,
    val gold: Int,
    val dummyHp: Int,
    val poisonTicksLeft: Int,
    val attackCooldownMsLeft: Long,
    val questState: String,
)


sealed interface GameEvent{
    val playerId: String
}
data class AttackPressed(
    override val playerId: String,
    val targetId: String
): GameEvent

data class DamageDealt(
    override val playerId: String,
    val targetId: String,
    val amount: Int
) : GameEvent

data class QuestStateChanged(
    override val playerId: String,
    val questId: String,
    val newState: String
): GameEvent
data class ChoiceSelected(
    override val playerId: String,
    val npcId: String,
    val choiceId: String
): GameEvent
data class PoisonApplied(
    override val playerId: String,
    val ticks: Int,
    val damagePerTick: Int,
    val intervalMs: Long
): GameEvent

data class  TalkedToNpc(
    override val playerId: String,
    val npcId: String,
): GameEvent

data class SaveRequested(
    override val playerId: String,
): GameEvent

data class CommandRejected(
    override val playerId: String,
    val reason: String
) : GameEvent

data class AttackSpeedBuffApplied(
    override val playerId: String,
    val ticks: Int
) : GameEvent

data class ServerMessage(
    override val playerId: String,
    val text: String
): GameEvent


class GameServer {
    private val _events = MutableSharedFlow<lesson10.GameEvent>(extraBufferCapacity = 64)
    // Дополнительный небольшой буфер, что Emit при рассылке событий чаще проходил не упираясь в ограничение буфера

    val events: SharedFlow<lesson10.GameEvent> = _events.asSharedFlow()

    private val _players = MutableStateFlow(
        mapOf(
            "Oleg" to PlayerSave("Oleg", 100, 0, 50, 0, "START"),
            "Stas" to PlayerSave("Oleg", 100, 0, 50, 0, "START")
        )
    )

    val players: StateFlow<Map<String, PlayerSave>> = _players.asStateFlow()

    fun tryPublish(event: lesson10.GameEvent): Boolean {
        return _events.tryEmit(event)
    }

    suspend fun publish(event: lesson10.GameEvent) {
        _events.emit(event)
    }

    fun updatePlayer(playerId: String, change: (PlayerSave) -> PlayerSave) {
        //change - функция, которая берёт старый PlayerSave и возращает новый

        val oldMap = _players.value
        val oldPlayer = oldMap[playerId] ?: return

        val newPlayer = change(oldPlayer)

        val newMap = oldMap.toMutableMap()
        newMap[playerId] = newPlayer

        _players.value = newMap.toMutableMap()
    }

    fun getPlayer(playerId: String): PlayerSave {
        return _players.value[playerId] ?: PlayerSave(playerId, 100, 0, 50, 0L, "START")
    }
    class CooldownSystem(
        val server: GameServer,
        private val scope: kotlinx.coroutines.CoroutineScope
    ) {
        private val cooldownJobs = mutableMapOf<String, Job>()

        fun startCooldown(playerId: String) {
            cooldownJobs[playerId]?.cancel()

            val player = server.getPlayer(playerId)

            val cooldownMs = if (player.attackSpeedBuffTicksLeft > 0) 700L else 1200L

            server.updatePlayer(playerId) {
                it.copy(attackCooldownMsLeft = cooldownMs)
            }

            val job = scope.launch {
                var waiting = cooldownMs
                val step = 100L

                while (isActive && waiting > 0L) {
                    delay(step)
                    waiting -= step
                    server.updatePlayer(playerId) {
                        it.copy(attackCooldownMsLeft = waiting)
                    }
                }

                val updatedPlayer = server.getPlayer(playerId)
                if (updatedPlayer.attackSpeedBuffTicksLeft > 0) {
                    server.updatePlayer(playerId) {
                        it.copy(attackSpeedBuffTicksLeft = (it.attackSpeedBuffTicksLeft - 1).coerceAtLeast(0))
                    }
                }
            }

            cooldownJobs[playerId] = job
        }

        fun canAttack(playerId: String): Boolean {
            return server.getPlayer(playerId).attackCooldownMsLeft <= 0L
        }
    }
}
class CooldownSystem(
    val server: GameServer,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    private val cooldownJobs = mutableMapOf<String, Job>()

    fun startCooldown(playerId: String) {
        cooldownJobs[playerId]?.cancel()

        val player = server.getPlayer(playerId)

        val cooldownMs = if (player.attackSpeedBuffTicksLeft > 0) 700L else 1200L

        server.updatePlayer(playerId) {
            it.copy(attackCooldownMsLeft = cooldownMs)
        }

        val job = scope.launch {
            var waiting = cooldownMs
            val step = 100L

            while (isActive && waiting > 0L) {
                delay(step)
                waiting -= step
                server.updatePlayer(playerId) {
                    it.copy(attackCooldownMsLeft = waiting)
                }
            }

            val updatedPlayer = server.getPlayer(playerId)
            if (updatedPlayer.attackSpeedBuffTicksLeft > 0) {
                server.updatePlayer(playerId) {
                    it.copy(attackSpeedBuffTicksLeft = (it.attackSpeedBuffTicksLeft - 1).coerceAtLeast(0))
                }
            }
        }

        cooldownJobs[playerId] = job
    }

    fun canAttack(playerId: String): Boolean {
        return server.getPlayer(playerId).attackCooldownMsLeft <= 0L
    }
}
class PoisonSystem(
    private val server: GameServer,
    private val scope: kotlinx.coroutines.CoroutineScope
){
    private val poisonJobs = mutableMapOf<String, Job>()

    fun startPoison(e: PoisonApplied, publishDamage: (GameEvent) -> Unit){
            poisonJobs[e.playerId]?.cancel()

            server.updatePlayer(e.playerId){ player ->
                player.copy(poisonTicksLeft = player.poisonTicksLeft + e.ticks)
            }
            val job = scope.launch{
                while (isActive && server.getPlayer(e.playerId).poisonTicksLeft > 0){
                    delay(e.intervalMs)

                    server.updatePlayer(e.playerId){ player ->
                        player.copy(poisonTicksLeft = (player.poisonTicksLeft - 1).coerceAtLeast(0))
                    }
                    publishDamage(DamageDealt(e.playerId, "self", e.damagePerTick))
                }
            }
            poisonJobs[e.playerId] = job
        }
    }
}

class DamageSystem(private val server: GameSErver){
    fun handleDamage(e: DamageDealt){
        server.updatePlayer(e.playerId) { player ->
            if(e.targetId == "self"){
                val newHp = (player.hp - e.amount).coerceAtLeast(0)
                player.copy(hp = newHp)
            }else{
                val newDummy = (player.dummyHp - e.amount).coerceAtLeast(0)
                player.copy(newDummy)
            }
        }
    }
}

class QuestSystem(private val server: GameServer){
    private val questId = "q_alchemist"
    private val npcId = "alchemist"

    fun handleTalk(e: TalkedToNpc, publish: (GameEvent) -> Unit){
        if (e.npcId != npcId) return

        val player = server.getPlayer(e.playerId)
        if (player.questState == "START"){
            server.updatePlayer(e.playerId){it.copy(questState = "OFFERED")}
            publish(QuestStateChanged(e.playerId, questId, "OFFERED"))
        }
    }

    fun handleChoice(e: ChoiceSelected, publish: (GameEvent) -> Unit){
        if(e:npcId != npcId) return

        val player = server.getPlayer(e.playerId)
        if(player.questState == "OFFERED"){
            val newState = if(e.choiceId == "help") "GOOD_END" else "EVIL_END"
            server.updatePlayer(e.playerId) {it.copy(questState = newState)}
            publish(Que)
        }
    }
}
class SaveSystem{
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    private fun file(playerId: String): File{
        val dir = File("saves")
        if(!dir.exists()) dir.mkdirs()
        return File(dir, "$playerId.json")
    }
    fun save(player: lesson10.PlayerSave){

        val text = json.encodeToString(lesson10.PlayerSave.serializer(), player )

        file(player.playerId).writeText(text)
    }
    fun load(playerId: String): lesson10.PlayerSave? {
        val file = file(playerId)
        if (!file.exists()) return null

        val text = file.readText()

        return try {
            json.decodeFromString(lesson10.PlayerSave.serializer(), text)
        } catch (e: Exception) {
            println("Ошибка загрузки файла $playerId")
            null

        }
    }
}

fun hudLog(hud: HudState, line: String){
    hud.log.value = (hud.log.value + line).takeLast(20)
}

fun main() = KoolApplication {
    val hud = HudState()

    addScene {

        defaultOrbitCamera()

        addColorMesh {
            generate { cube { colored() } }

            shader = KslPbrShader {
                color { vertexColor() }
                metallic(0.7f)
                roughness(0.4f)
            }
            onUpdate {
                transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
            }
        }
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }

        val server = GameServer()
        val saver = SaveSystem()
        val damage = DamageSystem(server)
        val cooldowns = CooldownSystem(server, coroutineScope)
        val poison = PoisonSystem(server, coroutineScope)
        val quests = QuestSystem(server)

        // подписка на события 1:
        // получает событие только AttackPressed -> проверяем кулдаун -> вызываем DamageDealt + запуск кулдауна по новой
        server.events
            .filter { it is AttackPressed }
            .onEach { event ->
                val e = event as AttackPressed
                // as - приведение типа (мы уверены что событием здесь будет AttackPressed так как выше - мы их отфильтровал)

                if(!cooldowns.canAttack(e.playerId)){
                    val msg = ServerMessage(e.playerId, "нильзя: кд")
                    if(!server.tryPublish(msg)){
                        coroutineScope.launch { server.publish(msg) }
                    }
                    return@onEach
                    // Выход из onEach блока но не из всех функции
                }

                val dmg = DamageDealt(e.playerId, e.targetId, 10)

                if (!server.tryPublish(dmg)){
                    coroutineScope.launch {server.publish(dmg)}
                }

                cooldowns.startCooldown(e.playerId, 1200L)
            }
            .launchIn(coroutineScope)
            // Собтрает и запускает поток в области корутин что указали coroutineScope

            // Подписка 2:
            // Подписка 2: DamageDealt вызывает DamageSystem для смены HP
        server.events
            .filter{ it is DamageDealt }
            .onEach { event ->
                val e = event as DamageDealt
                damage.handleDamage(e)
            }
            .launchIn(coroutineScope)

        // Подписка 3: PoisonApplied -> корутина тиков -> DamageDealt
        server.events
            .filter {it is PoisonApplied}
            .onEach { event ->
                val e = event as PoisonApplied

                poison.startPoison(e){ dmg ->
                    if (!server.tryPublish(dmg)){
                        coroutineScope.launch { server.publish(dmg) }
                    }
                }
            }
            .launchIn(coroutineScope)

        // Подписка 4: TalkedToNpc и ChoiceSelected -> квест
        server.events
            .filter {it is TalkedToNpc}
            .onEach { event ->
                val e = event as TalkedToNpc
                quests.handleTalk(e){ newEvent ->
                    if (!server.tryPublish(newEvent)){
                        coroutineScope.launch { server.publish(newEvent) }
                    }
                }
            }
            .launchIn(coroutineScope)

        server.events
            .filter {it is ChoiceSelected}
            .onEach { event ->
                val e = event as ChoiceSelected
                quests.handleChoice(e){ newEvent ->
                    if (!server.tryPublish(newEvent)){
                        coroutineScope.launch { server.publish(newEvent) }
                    }
                }
            }
            .launchIn(coroutineScope)

        server.events
            .filter {it is QuestStateChanged}
            .onEach { event ->
                val e = event as QuestStateChanged
                val save = SaveRequested(e.playerId)

                if (!server.tryPublish(save)){
                    coroutineScope.launch { server.publish(save) }
                }
            }
            .launchIn(coroutineScope)
        server.events
            .filter {it is SaveRequested}
            .onEach { event ->
                val e = event as SaveRequested
                val snapSpec = server.getPlayer(e.playerId)
                saver.save(snapShot)
            }
            .launchIn(coroutineScope)

        Shared.server = server
        // Учебный мост между сервером и UI
        // Связывает данные между двумя сценами
    }

    addScene {
        setupUiScene(ClearColorLoad)

        val server = Shared.server

        if (server != null) {
            coroutineScope.launch {
                server.players.collect{ playersMap ->
                    val pid = hud.activePlayerIdFlow.value
                    val p = playersMap[pid] ?: return@collect

                    hud.hp.value = p.hp
                    hud.gold.value = p.gold
                    hud.dummyHp.value = p.dummyHp
                    hud.poisonTicksLeft.value = p.poisonTicksLeft
                    hud.questState.value = p.questState
                    hud.attackCooldownMsLeft.value = p.attackCooldownMsLeft

                }
            }
            // Маршрутизация событий по активному игроку
            // activePlayerFlow -> flatMapLatest -> поток событий только для этого игрока
            hud.activePlayerIdFlow
                .flatMapLatest {pid ->
                    // flatMapLatest отзначает - "каждый раз когда pid еняется -> переключить а новый поток и перестать слушать старый"
                    server.events.filter { it.player == pid }
                    // Теперь фильтруем только по события игрока
                }
                .map{ event ->
                    // Превращает событие в строку лога
                    eventToText(event)
                }
                .onEach { line ->
                    hudLog(hud, line)
                }
                .launchIn(coroutineScope)
        }

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(16.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.6f), 14.dp))
                .padding(12.dp)

            Column {
                Text("Player: ${hud.activePlayerUi.use()}") {}
                Text("HP: ${hud.hp.use()} Gold: ${hud.gold.use()}") {
                    modifier.margin(bottom = sizes.gap)
                }
                Text("DummyHp: ${hud.dummyHp.use()}") {}

                Text("QuestState: ${hud.questState.use()}") {}
                Text("Poison ticks left: ${hud.poisonTicksLeft.use()}") {}
                Text("Attack cooldown: ${hud.attackCooldownMsLeft.use()}") {
                    modifier.margin(bottom = sizes.gap)
                }

                Row{
                    Button("Смена игрока"){
                        //Получить новый d ользователя
                        // обновляем в hud состояния activePlayerIdUi и activePlayerIdFlow
                    }
                }
            }

        }
    }
}

fun eventToText(e: GameEvent): String {
    return when (e) {
        is AttackPressed -> "[${e.playerId}] AttackPressed по ${e.targetId}"
        is DamageDealt -> "[${e.playerId}] ${e.amount}] DamageDealt по ${e.targetId}"
        is PoisonApplied -> "[${e.playerId}] ${e.ticks}] PoisonApplied"
        is SaveRequested -> "[${e.playerId}]  SaveRequested ${e.choiceId}]"
        is ServerMessage -> "[${e.playerId}]  ServerMessage ${e.text}]"
        is TalkedToNpc -> "[${e.playerId}]  TalkedToNpc ${e.npcId}]"
        is QuestStateChanged -> "[${e.playerId}]  QuestStateChanged ${e.newState}]"
    }
}

object Shared{
    var server: GameServer? = null
}