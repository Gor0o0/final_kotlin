package lesson13

import lesson12.EventBus
import lesson12.GameEvent

fun main(){

    val system = TrainingStateSystem()
    system.register()

    val player1 = "Oleg"
    val player2 = "Innokentiy"

    println("\n=== Oleg начинает диалог ===")
    EventBus.post(GameEvent.DialogueStarted(player1, "Trainer"))
    EventBus.processQueue()

    println("\n=== Innokentiy начинает диалог ===")
    EventBus.post(GameEvent.DialogueStarted(player2, "Trainer"))
    EventBus.processQueue()

    println("\n=== Oleg выбирает 'accept' ===")
    EventBus.post(GameEvent.DialogueChoiceSelected(player1, "Trainer", "accept"))
    EventBus.processQueue()

    println("\n=== Innokentiy выбирает 'accept' ===")
    EventBus.post(GameEvent.DialogueChoiceSelected(player2, "Trainer", "accept"))
    EventBus.processQueue()

    println("\n===  Oleg убивает манекен ===")
    EventBus.post(GameEvent.CharacterDied("Dummy", player1, player1))
    EventBus.processQueue()

    println("\n===  Innokentiy убивает манекен ===")
    EventBus.post(GameEvent.CharacterDied("Dummy", player2, player2))
    EventBus.processQueue()

    println("\n=== Oleg завершает квест ===")
    EventBus.post(GameEvent.DialogueChoiceSelected(player1, "Trainer", "complete"))
    EventBus.processQueue()

    println("\n=== Innokentiy завершает квест ===")
    EventBus.post(GameEvent.DialogueChoiceSelected(player2, "Trainer", "complete"))
    EventBus.processQueue()

}