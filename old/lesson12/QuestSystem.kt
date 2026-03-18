package lesson12


import kotlin.collections.iterator

//Квест = шаги выполнения (mini State Graph)
class QuestSystem {
    // Храним состояние квеста для каждого игрока
    private data class PlayerQuestState(
         var questStarted: Boolean = false,
         var stepTalking: Boolean = false,
         var stepKillKirill: Boolean = false,
         var stepReportedBack: Boolean = false,
         var questCompleted: Boolean = false
    )

    private val playerStates = mutableMapOf<String, PlayerQuestState>()
    private val questId = "simple_dimple_quest_001"
    private val progressByPlayer: MutableMap<String, PlayerQuestState> = mutableMapOf()

    private fun getPlayerState(playerName: String): PlayerQuestState {
        return playerStates.getOrPut(playerName) { PlayerQuestState() }
    }

    fun register() {
        EventBus.subscribe { event ->
            when (event) {
                is GameEvent.DialogueStarted -> {
                    if (event.npcName == "Старый") {
                        val state = getState(event.playerId)

                        if (!state.questStarted) {
                            state.questStarted = true
                            state.stepTalking = true
                            println("Квест $questId начат игроком ${event.playerId} через диалог с ${event.npcName}")

                            EventBus.post(GameEvent.QuestStarted(event.playerId, questId))
                            EventBus.post(GameEvent.PlayerProgressSaved(event.playerId, questId, "talk_to_elder"))
                            completeStep(event.playerId, "talk_to_npc")
                        }
                    }
                }

                is GameEvent.CharacterDied -> {
                    val state = getState(event.playerId)
                    if (state.questStarted &&
                        !state.stepKillKirill &&
                        event.characterName == "Kirill" &&
                        event.killerName == "Oleg"
                    ) {
                        state.stepKillKirill = true
                        println("Игрок ${event.playerId} выполнил шаг квеста $questId: убить Кирилла")
                        completeStep(event.playerId, "kill_kirill")
                    }
                }

                is GameEvent.DialogueChoiceSelected -> {
                    if (event.npcName == "Старый" && event.choiceId == "report_done") {
                        val state = getState(event.playerId)
                        if (state.questStarted && state.stepKillKirill && !state.stepReportedBack)
                            state.stepReportedBack = true
                        println("Игрок ${event.playerId} сдал квест")
                        completeStep(event.playerId, "report_back")
                    }
                }

                else -> {}
            }
            checkQuestCompletionForAllPlayers()

        }
    }

    private fun getState(playerId: String): PlayerQuestState {
        return progressByPlayer.getOrPut(playerId) { PlayerQuestState() }
    }

    private fun completeStep(playerId: String, stepId: String) {
        EventBus.post(GameEvent.QuestStepCompleted(playerId, questId, stepId))
        EventBus.post(GameEvent.PlayerProgressSaved(playerId, questId, stepId))
    }

    private fun checkQuestCompletionForAllPlayers() {
    for((playerId, state) in progressByPlayer)
    {
        if(!state.questCompleted &&
            state.questStarted &&
            state.stepTalking &&
            state.stepKillKirill &&
            state.stepReportedBack
            ){
            state.questCompleted = true
            println("Квест ${questId} завершён для игрока $playerId")
            EventBus.post(GameEvent.QuestCompleted( questId, "Oleg"))
            EventBus.post(GameEvent.PlayerProgressSaved(playerId, questId, "QUEST_COMPLETED"))
        }
    }
}
}