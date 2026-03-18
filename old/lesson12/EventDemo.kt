package lesson12

fun main(){
    val firstEventListenerId = EventBus.subscribeOnce { event ->
        println("[FIRST EVENT] Первое событие в игре: ${event::class.simpleName}")
    }
    //регистрация систем
    val logSystem = LogSystem()
    logSystem.register()

    val achivementSystem = AchievementSystem()
    achivementSystem.register()

    val questSystem = QuestSystem()
    questSystem.register()

    val npcSystem = NpcSystem()
    npcSystem.register()

    println("=== СЦЕНА 1: Игрок начинает диалог с неписем ====")

    EventBus.post(GameEvent.DialogueStarted("Oleg", "Старый"))

    EventBus.processQueue(50)

    println("=== СЦЕНА 2: Процесс боя ====")
    val combat = CombatSystemDemo()
    combat.simulateFight()

    EventBus.processQueue(50)

    println("=== СЦЕНА 3: Игрок возвращается с докладом и головой Кирилла===")
    EventBus.post (
        GameEvent.DialogueChoiceSelected(
        "Старый", "Oleg", "report_done")
    )
    EventBus.processQueue(50)

    //другой игрок
    println("=== СЦЕНА 4: Другой игрок начинает тот же квест ===")
    EventBus.post(GameEvent.DialogueStarted("Laboda", "Старый"))
    EventBus.processQueue(10)

    EventBus.processQueue(50)
}