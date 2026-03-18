package lesson13
import lesson12.EventBus
import lesson12.GameEvent

class TrainingStateSystem {
    private val progress = TrainingProgress()

    fun register(){
        EventBus.subscribe { event ->
            when (event) {
                is GameEvent.DialogueStarted -> {
                    progress.handleEvent(event.playerId, event)
                }
                is GameEvent.DialogueChoiceSelected -> {
                    progress.handleEvent(event.playerId, event)
                }
                is GameEvent.CharacterDied -> {
                    progress.handleEvent(event.playerId, event)
                }
                else -> {}
            }
        }
    }
}