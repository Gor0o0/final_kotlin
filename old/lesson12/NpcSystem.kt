package lesson12

class NpcSystem{
    fun register(){
        EventBus.subscribe { event ->
            when (event){
            is GameEvent.QuestStarted -> {
                println("[NPCSystem] Старый ждёт результата")
                EventBus.post(GameEvent.DialogueLineUnlocked("Старый", "remind_kill_kirill", "Oleg"))

                }
                is GameEvent.QuestStepCompleted -> {
                    if(event.stepId == "kill_kirill"){
                        println("[NPCSystem] Открыта новая реплика 'o, ну шо там с квстом, Кирилл готов?'")
                        EventBus.post(GameEvent.DialogueLineUnlocked("Старый", "ask_report", "Oleg"))
                    }
                }
                is GameEvent.QuestCompleted -> {
                    println("[NPCSystem] Квест выполнен, открываем реплику с поздравлением")
                    EventBus.post(GameEvent.DialogueLineUnlocked("Старый", "congrats", "Oleg"))
                }
                else -> {}
            }
        }
    }
}