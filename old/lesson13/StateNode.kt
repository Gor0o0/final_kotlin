package lesson13

import lesson12.GameEvent
import java.awt.font.GlyphMetrics

// узлы графа ( State Node)
// будем явно описывать каждый переход, а не прятать их за if/else

class StateNode(
    val state: TrainingState
    // Какое состояние представляет этот узел

){
    // все возможные переходы ИЗ этого состояния
    private val transitions = mutableMapOf<Class<out GameEvent>, TrainingState>()
    // Класс GameEvent ИЛИ ЛЮБОЙ класс, который от него наследуется
    // GameEvent - это иерархия событий( а события от неё наследуются)
    // out - модификатор ковариантности, используется в обобщённых generic типах данных (классах)
    // Используется для указания, что параметризованный тип может быть использован в иерархии
    // Если пишем класс, который должен вернуть строго тип данных  game event, то используем out
    // Словарь: ключ - тип события ( например DialogueStarted)
    // Значение - в какое состояние перейдём при данном событии

    fun addTransition(
        eventType: Class<out GameEvent>,
        nextState: TrainingState
    ){
        transitions[eventType] = nextState
    }
    fun getNextState(event: GameEvent): TrainingState? {
        //Достаём класс события и ищем, куда он может перейти в случае этого события
        return transitions[event::class.java]
        // Событие -> Состояние
        // event - Kotlin
        // event::class - язык программирования
        // event::class - скажи мне, КАКОГО ТИПА данный объект
        // :: - оператор ссылки
        // Он НЕ ИСПОЛЬЗУЕТ ОБЪЕКТ, а только ссылается  на информацию о нём
        // ссылка на класс объекта event
        // .java - зачем?
        // Kotlin и Java существуют вместе одновременно
        // Котлин работает поверх JVM (Java Virtual Machine)
        // И создаваемый нами Map существует и работает как Java-класс
        // event::class - Котлин класс
        // event::class.java - Java класс
        // ЭТО НЕ ДВА РАЗНЫХ КЛАССА - это 2 разных формы записи одного и того же типа
        // Java тип здесь нам нужен, потому что Map<Class<>, ...> - использует именно класс Java
        // return transitions[event::class.java] -  по человечески
        // "Взять тип события, которое пришло, найти в таблице переходов(map), то в какое состояние мы должны перейти и верни его к нам"
        // Пример: Если приходит событие DialogueStarted то код сделает:
        // transition[DialogueStarted] -> TALKING
        // Если события в таблице не найдётся -> вернёт null

    }
}