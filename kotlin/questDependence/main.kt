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

enum class QuestStatus{
    ACTIVE,
    COMPLETED,
    LOCKED,
    FAILED
}

enum class QuestMarker{
    NEW,
    PINNED,
    COMPLETED,
    LOCKED,
    NONE
}

enum class QuestBranch{
    NONE,
    HELP,
    THREAT
}

data class QuestStateOnServer(
    val questId: String,
    val title: String,
    val status: QuestStatus,
    val step: Int,
    val branch: QuestBranch,
    val progressCurrent: Int,
    val progressTarget: Int,
    val isNew: Boolean
    val isPinned: Boolean,
    val unlockRequiredQuestId: String? = null,
)
data class QuestJournalEntry(
    val questId: String,
    val title: String,
    val status: QuestStatus,
    val objectiveText: String,
    val progressText: String,
    val progressBar: String,
    val marker: QuestMarker,
    val markerHint: String,
    val branchText: String,
    val lockedReason: String
)

// ------------------- СОБЫТИЯ, что будет влиять на UI и другие системы

sealed interface GameEvent{
    val playerId: String
}

data class QuestBranchChosen(
    override val playerId: String,
    val questId: String,
    val branch: QuestBranch
): GameEvent

data class ItemCollected(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int
): GameEvent

data class GoldTurnedIn(
    override playerId: String,
    val questId: String,
    val amount: Int
): GameEvent

data class QuestCompleted(
    override val playerId: String,
    val questId: String
): GameEvent

data class QuestUnlocked(
    override val playerId: String,
    val questId: String
): GameEvent

data class QuestJournalUpdated(
    override val playerId: String
): GameEvent

// Игрок открыл квест - поменять маркер NEW
data class QuestOpened(
    override val playerId: String,
    val questId: String
): GameEvent

data class QuestPinned(
    override val playerId: String,
    val questId: String
): GameEvent

data class QuestProggressed(
    override val playerId: String,
    val questId: String
): GameEvent

// ------------- Команды UI -> Сервер

sealed interface GameCommand{
    val playerId: String
}

data class CmdChooseBranch(
    override val playerId: String,
    val questId: String,
    val branch: QuestBranch
): GameCommand

data class CmdCollectItem(
    override val playerId: String,
    val itemId: String,
    val countAdded: Int
): GameCommand

data class CmdGiveGoldDebug(
    override playerId: String,
    val amount: Int
): GameCommand

data class CmdFinishQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class QuestJournalUpdated(
    override val playerId: String
): GameCommand

// Игрок открыл квест - поменять маркер NEW
data class QuestOpened(
    override val playerId: String,
    val questId: String
): GameCommand

data class QuestPinned(
    override val playerId: String,
    val questId: String
): GameCommand

data class QuestProggressed(
    override val playerId: String,
    val questId: String
): GameCommand

// Игрок открыл квест - поменять маркер NEW
data class CmdOpenQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class CmdPinQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class CmdProggressQuest(
    override val playerId: String,
    val questId: String
): GameCommand

data class CmdSwitchPlayer(
    override val playerId: String,
    val questId: String
): GameCommand

data class PlayerData(
    val playerId: String,
    val gold: Int,
    val inventory: Map<String, Int>

)

class QuestSystem {
    // Здесь прописываем текст целей квестов по шагам на каждого квеста

    fun objectiveFor(q: QuestStateOnServer): String {
        if (q.status == QuestStatus.LOCKED){
            return "нет"
        }

        if (q.questId == "q_alchemit"){
            return when(q.step){
                0 -> "Поговори с Алхимкой"
                1 -> {
                    when(q.branch){
                        QuestBranch.NONE -> "Выбери пусть help ли threat"
                        QuestBranch.HELP -> "собери траву ${q.progressCurrent} / ${q.progressTarget}"
                        QuestBranch.THREAT -> "собери золото ${q.progressCurrent} / ${q.progressTarget}"
                    }
                }
                2 -> "вернись к алхимику"
                else -> "квест завершён"
            }

        }
        if (q.questId == "q_guard"){
            return when(q.step){
                0 -> "Поговори с гвардом"
                1 -> "заплати ему: ${q.progressCurrent} / ${q.progressTarget}"
                2 -> "сдай квест`"
                else -> "квест завершён"
            }
        }

        return when (questId) {
            "q_alchemist" -> when (step) {
                0 -> "Поговорить с алхимиком"
                1 -> "Собери траву"
                2 -> "Принеси траву"
                else -> "Квест завершён"
            }

            "q_guard" -> when (step) {
                0 -> "поговорить стражем этой двери"
                1 -> "Заплатить 10 золота"
                else -> "Проход открыт"
            }

            else -> "Неизвестный квест"
        }
    }

    // Подсказки куда идти - в будущем используем для карты в компассе
    private fun markerHintFor(q: QuestStateOnServer): String {
        if (q.status == QuestStatus.LOCKED){
            return "Сначала разблокируй"
        }

        if (q.questId == "q_alchemist"){
            return when (q.step){
                0 -> "NPC: алхимик"
                1 -> {
                    when (q.branch){
                        QuestBranch.NONE -> "Выбери вариант"
                        QuestBranch.HELP -> "собери траву"
                        QuestBranch.THREAT -> "найди золото"
                    }
                }
                2 -> "NPC: Алхимик"
                else -> "Готово"
            }
        }

        if (q.questId == "q_guard"){
            return when (q.step){
                0 -> "NPC: guard"
                1 -> "найди золото"
                2 -> "сдай квест"
                else -> "Готово"
            }
        }
        return ""
    }
    fun branchTextFor(branch: QuestBranch): String {
        return when(branch){
            QuestBranch.NONE -> "Путь не выбран"
            QuestBranch.HELP -> "Путь помощи"
            QuestBranch.THREAT -> "Путь угрозы"
        }
    }

    fun lockedReasonFor(q: QuestStateOnServer): String {
        if (q.status != QuestStatus.LOCKED) return ""

        return if(q.unlockRequiredQuestId == null){
            "Причина блокировки неизвестна"
        }else{
            "you need complete quest ${q.unlockRequiredQuestId}"
        }
    }

    fun markerFor(q: QuestStateOnServer): QuestMarker {
        return when{
            q.status == QuestStatus.LOCKED -> QuestMarker.LOCKED
            q.status == QuestStatus.COMPLETED -> QuestMarker.COMPLETED
            q.isPinned -> QuestMarker.PINNED
            q.isNew -> QuestMarker.NEW
            else -> QuestMarker.NONE
        }
    }

    fun progressBarText(current: Int, target: Int, blocks: Int = 10): String {
        if (target <= 0) return ""

        val ratio = current.toFloat() / target.toFloat()
        // ratio = отношение прогресса к цели

        val filled = (ratio*blocks).toInt().coerceIn(0, blocks)
        //coerceIn - ограничивает от 0 до ... blocks (10) исла

        val empty = blocks - filled

        return "0".repeat(filled) + "1".repeat(empty)
    }

    fun toJournalEntry(q: QuestStateOnServer): QuestJournalEntry{
        val progressText = if(q.progressTarget > 0) "${q.progressCurrent} / ${q.progressTarget}" else ""

        val progressBar = if(q.progressTarget > 0) progressBarText(q.progressCurrent, q.progressTarget) else ""

        return QuestJournalEntry(
            q.questId,
            q.title,
            q.status,
            objectiveFor(q),
            progressText,
            progressBar,
            markerFor(q),
            markerHintFor(q),
            branchTextFor(q.branch),
            lockedReasonFor(q)
        )
    }

    fun applyEvent(quests: List<QuestStateOnServer>, event: GameEvent): List<QuestStateOnServer>{
        val copy = quests.toMutableList()

        for (i in copy.indices){
            val q = copy[i]

            if (q.status == QuestStatus.LOCKED) continue
            if (q.status == QuestStatus.COMPLETED) continue

            if (q.questId == "q_alchemist"){
                copy[i] = updateAlchemist(q, event)
            }

            if (q.questId == "q_guard"){
                copy[i] = updateGuard(q, event)
            }
        }
        return copy.toList()
    }

    private fun updateAlchemist(q: QuestStateOnServer, event: GameEvent): QuestStateOnServer{
        if(q.step == 0 && event is QuestBranchChosen && event.questId == q.questId){
            return when(event.branch){
                QuestBranch.HELP -> q.copy(
                    step = 1,
                    branch = QuestBranch.HELP,
                    progressCurrent = 0,
                    progressTarget = 3,
                    isNew = false
                )
                QuestBranch.THREAT -> q.copy(
                    step = 1,
                    branch = QuestBranch.THREAT,
                    progressCurrent = 0,
                    progressTarget = 10,
                    isNew = false
                )

                QuestBranch.NONE -> q
            }
        }

        if (q.step == 1 && q.branch == QuestBranch.HELP && event is ItemCollected && event.itemId == "Herb"){
            val newCurrent = (q.progressCurrent + event.countAdded).coerceAtMost(q.progressCurrent)
            val update = q.copy(progressCurrent = newCurrent, isNew = false)

            if(newCurrent >= q.progressTarget){
                return update.copy(step = 2, progressCurrent = 0, progressTarget = 0)
            }
            return update
        }
        if(q.step == 1 && q.branch == QuestBranch.THREAT && event is GoldTurnedIn && event.questId == q.questId){
            val newCurrent = (q.progressCurrent + event.amount).coerceAtMost(q.progressCurrent)
            val update = q.copy(progressCurrent = newCurrent, isNew = false)

            if(newCurrent >= q.progressTarget){
                return update.copy(step = 2, progressCurrent = 0, progressTarget = 0)
            }
            return update
        }
    }
}