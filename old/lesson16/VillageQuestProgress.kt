package lesson16

import lesson14.*
import lesson15.*
import lesson12.*

class VillageQuestProgress {
    private val graph = VillageQuestGraph()

    private val stateByPlayer = mutableMapOf<String, VillageQuestState>()

    fun getState(playerId: String): VillageQuestState{
        return  stateByPlayer.getOrPut(playerId){ VillageQuestState.NOT_STARTED }
    }
    fun handle(playerId: String, event: GameEvent){
        val current = getState(playerId)
        val node = graph.getNode(current)
        val next = node.next(event)

        if (next !=null){
            println("[QUEST GRAPH] - $playerId: $current -> $next")
            stateByPlayer[playerId] = next
            EventBus.post(
                GameEvent.QuestStateChanged(
                    playerId,
                    "Village_quest",
                    current.name,
                    next.name
                )
            )
            // Ключевое, для чего это надо
            // State Graph теперь не просто меняет состояние, но теперь он ОБЯЗАН сообщить миру, что состояние
            // квеста изменилось.
        }else{
            println("[QUEST GRAPH] - $playerId:[QUEST GRAPH] - $playerId: cобытие ${event::class.simpleName} игнорировано. Игрок остался в состоянии $current")
            // event::class.simpleName -> это свойство, которое возвращает короткое имя класса( строку). Без указания пакета, в котором он лежит.
        }
    }
}