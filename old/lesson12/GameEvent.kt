package lesson12

import lesson16.GameEventV2

sealed class GameEvent(open val playerId: String){


    //БОЕВЫЕ СОБЫТИЯ
    data class  CharacterDied(

        val characterName: String,
        val killerName: String? = null, // персонаж может сам умереть без участия кого либо
        override val playerId: String

    ) : GameEvent(playerId)

    data class DamageDealt(

        override val playerId: String,
        val targetName: String,
        val amount: Int,

    ): GameEvent(playerId)

    data class EffectApplied(

        override val playerId: String,
        val effectName: String,


    ): GameEvent(playerId)

    data class EffectEnded(

        val characterName: String,
        val effectName: String,
        override val playerId: String
    ) : GameEvent(playerId)

    //Квесты и игровой прогресс

    data class QuestStarted(

        val questId: String,
        override val playerId: String
    ): GameEvent(playerId)

    data class QuestStepCompleted(

        val questId: String,
        val stepId: String,
        override val playerId: String
    ): GameEvent(playerId)

    data class QuestCompleted(

        val questId: String,
        override val playerId: String
    ): GameEvent(playerId)
    // NPC и диалоги
    data class DialogueStarted(
        override val playerId: String,
        val npcName: String
    ): GameEvent(playerId)

    data class  DialogueChoiceSelected(

        val npcName: String,
        val choiceId: String,
        override val playerId: String
    ): GameEvent(playerId)
    data class DialogueLineUnlocked(

        val npcName: String,
        val lineId: String,
        override val playerId: String
    ): GameEvent(playerId)
    data class AchivementUnlocked(

        val achivementId: String,
        override val playerId: String
    ): GameEvent(playerId)

    data class PlayerProgressSaved(
        override val playerId: String,
        val questId: String,
        val stepId: String
    ): GameEvent(playerId)

    data class QuestStateChanged(
        override val playerId: String,
        val questId: String,
        val oldState: String,
        val newState: String
    ): GameEvent(playerId)
}