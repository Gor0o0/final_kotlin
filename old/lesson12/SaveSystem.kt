package lesson12

class SaveSystem{
    private val progress: MutableMap<String, MutableMap<String, MutableSet<String>>> = mutableMapOf()
    //progress[PlayerId][questId] = набор выполненных шагов игрока
    // MutableMap<String,... > - ключ рlayerId (cловарь ключом которого будет игрок, а значения - его прогресс)
    // MutableMap<String, MutableSet<String>> - ключ questId - все квесты игрока со всеми его шагами квеста
    // MutableMap<String> - набор шагов (stepId) которые уже выполнены игроков в квесте


    fun register(){
        EventBus.subscribe { event ->
            when(event) {
                is GameEvent.PlayerProgressSaved -> {
                    saveStep(event.playerId, event.questId, event.stepId)
                }
                else ->{}
            }
        }
    }

    fun saveStep(playerId: String, questId: String, stepId: String){
        val playerData = progress.getOrPut(playerId){mutableMapOf()}
        //getOrPut(playerId) {..} - ищет ключ, если находит его достаёт его значение
        // если не находит, то задаёт ему значение которое мы положили в {...}

        val questSteps = playerData.getOrPut(questId) {mutableSetOf()}

        val wasAdded = questSteps.add(stepId)

        if (wasAdded){
            println("[SAVE] Сохранено: игрок = $playerId квест= $questId шаг = $stepId")
        }else{
            println("[SAVE] Шаг уже был сохранён ранее: игрок = $playerId квест= $questId шаг = $stepId")
        }
    }
    fun printProgress(playerId: String){
        println("===Прогресс игры для playerId=$playerId===")

        val playerData = progress[playerId]
        if (playerData == null){
            println("Прогресса нет")
            return

        }
        for ((questId, steps) in playerData) {
            println("Квест: $questId")
            println("Шаги: $steps")
            println("===============")
        }
    }
}