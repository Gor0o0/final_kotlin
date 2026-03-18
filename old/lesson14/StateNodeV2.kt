package lesson14

import lesson12.GameEvent
import lesson12.EventBus

class StateNodeV2 (
    val state: VillageQuestState
){
    private val transitions = mutableMapOf<Class<out GameEvent>, VillageQuestState>()

    fun add(eventType: Class<out GameEvent>, next: VillageQuestState){
        transitions[eventType] = next
    }

    fun next(event: GameEvent): VillageQuestState? {
        return transitions[event::class.java]
    }
}