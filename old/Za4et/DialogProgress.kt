package Za4et

class DialogProgress {
    private val graph = DialogGraph()
    private var currentState = QuestState.START

    fun start() {
        println("=== ДИАЛОГОВАЯ СИСТЕМА ===")

        while (true) {
            val currentNode = graph.getNode(currentState)
            if (currentNode == null) {
                println("Ошибка: состояние $currentState не найдено!")
                break
            }

            // Показываем текущий диалог
            currentNode.print()

            // Если нет вариантов выбора - завершаем
            if (currentNode.getChoicesCount() == 0) {
                println("Диалог завершен!")
                break
            }

            // Обрабатываем ввод пользователя
            print("> ")
            val input = readLine()?.trim() ?: continue

            if (input == "exit") {
                println("Выход...")
                break
            }

            try {
                val choice = input.toInt()
                val nextState = currentNode.getNextState(choice)

                if (nextState != null) {
                    // Обрабатываем события при переходе
                    handleEvents(currentState, nextState)
                    currentState = nextState
                } else {
                    println("Неверный выбор! Введите число от 1 до ${currentNode.getChoicesCount()}")
                }
            } catch (e: NumberFormatException) {
                println("Введите число!")
            }
        }
    }

    private fun handleEvents(fromState: QuestState, toState: QuestState) {
        println("\n--- Событие ---")

        when {
            fromState == QuestState.START && toState == QuestState.TALKING -> {
                println("Событие: Разговор начат")
            }

            fromState == QuestState.TALKING && toState == QuestState.ACCEPTED -> {
                println("Событие: Задание принято")
                // Сразу имитируем выполнение задания
                println("Событие: DUMMY_KILLED - манекен повержен")
            }

            fromState == QuestState.TALKING && toState == QuestState.DECLINE -> {
                println("Событие: Задание отклонено")
            }

            (fromState == QuestState.ACCEPTED && toState == QuestState.COMPLETED) ||
                    (fromState == QuestState.DECLINE && toState == QuestState.COMPLETED) -> {
                println("Событие: COMPLETED - квест завершен")
            }

            fromState == QuestState.COMPLETED && toState == QuestState.START -> {
                println("Событие: Начало нового диалога")
            }
        }
        println()
    }

    fun getCurrentState(): QuestState = currentState
}