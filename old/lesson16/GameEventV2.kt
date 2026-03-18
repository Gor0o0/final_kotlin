package lesson16

//sealed class UiState - UiEvent
// EventBus - потоки событий -  Flow/SharedFlow - user interface реагировал на любые действия и команды
// DialogState + DialogGraph + DialogProgress
// - Nav Graph (Экраны приложения и переходы между ними)
// - Экранные состояния (что на данный момент показывает экран пользователю)
// - Сохранение прогресса (на каком экране был пользователь в последний раз, вошёл он или нет, фильтрация и настройки и тд)

sealed class GameEventV2(open val playerId: String) {
    data class QuestStateChanged(
        override val playerId: String,
        val questId: String,
        val oldState: String,
        val newState: String
    ): GameEventV2(playerId)
    // ЗАЧЕМ
    //UI узнаёт, что необходимо в нём обновить о квесте
    // NPC реагирует и узнаёт, что ему говорить
    // + к удобству сохранения прогресса
    //
}