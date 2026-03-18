package lesson12


// система логирования
class LogSystem{
    fun register(){
        EventBus.subscribe { event ->
            println("[INFO] Получено событие: $event")
        }
            // fun lalala (GameEvent) {....} // обычная функция
        // fun lalala (GameEvent) -> {///} // лямбда
        // null - значение (перeменные) - т.е. это прям точное значение без изменений(практически)
        // Unit - структура, которая потом может измениться
    }
}

class AchievementSystem{
    private val killCounts = mutableMapOf<String, Int>()

    fun register(){
        EventBus.subscribe { event ->
            when(event){
                is GameEvent.CharacterDied -> {
                    if (event.killerName != null){
                        val currentCount = killCounts.getOrDefault(event.killerName, 0) + 1
                        killCounts[event.killerName] = currentCount

                        println("Счётчик убийств игрока ${event.killerName}: $currentCount")

                        if (currentCount == 1){
                            EventBus.post(GameEvent.AchivementUnlocked("first_blood", "Oleg"))
                            println("Игрок ${event.killerName} получил достижение 'Первая кровь'")
                        }
                        if (currentCount == 5){
                            EventBus.post(GameEvent.AchivementUnlocked("ti_cho_delaesh", "Oleg"))
                            println("Игрок ${event.killerName} получил достижение 'Ты чо делаешь'")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}