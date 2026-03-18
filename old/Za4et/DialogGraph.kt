package Za4et

class DialogGraph {
    private val nodes = mutableMapOf<QuestState, DialogNode>()

    init {
        // Создаем все узлы диалога
        val start = DialogNode(
            QuestState.START,
            "Привет, подходи сюда."
        )
        val talking = DialogNode(
            QuestState.TALKING,
            "Я смотрю, ты тут впервые. Могу научить тебя сражаться. Хочешь?"
        )
        val accepted = DialogNode(
            QuestState.ACCEPTED,
            "Отлично, убей вон того манекена."
        )
        val decline = DialogNode(
            QuestState.DECLINE,
            "Тогда удачной дороги, путник!"
        )
        val dummyKilled = DialogNode(
            QuestState.DUMMY_KILLED,
            "Ты убил манекена? Подходи сюда!"
        )
        val completed = DialogNode(
            QuestState.COMPLETED,
            "Отлично! Ты доказал, что умеешь сражаться."
        )


        start.addChoice("1. Поговорить", QuestState.TALKING)


        talking.addChoice("1. Принять задание", QuestState.ACCEPTED)
        talking.addChoice("2. Отказаться", QuestState.DECLINE)


        accepted.addChoice("1. Завершить", QuestState.COMPLETED)


        decline.addChoice("1. Завершить", QuestState.COMPLETED)


        completed.addChoice("1. Начать заново", QuestState.START)

        // Добавляем все узлы в граф
        nodes[start.state] = start
        nodes[talking.state] = talking
        nodes[accepted.state] = accepted
        nodes[decline.state] = decline
        nodes[dummyKilled.state] = dummyKilled
        nodes[completed.state] = completed
    }

    fun getNode(state: QuestState): DialogNode? {
        return nodes[state]
    }

}