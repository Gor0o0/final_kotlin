package lesson12


object EventBus{
    //сохранение типа слушателя
    typealias Listener = (GameEvent) -> Unit
    //typealias - "переменная, но для типов данных
    // создание нового более короткого псевдонима для типа данных
    // т.е вместо (Game Event) -> Unit будем писать его псевдоним Listener

    // список подписчиков(слушателей) хранить в Мар
    // чтобы можно было опираться на id подписчика в базе
    private val listeners = mutableMapOf<Int, Listener>()
    private var nextId: Int = 1
    //ID следующего подписчика

    // очередь событий (пошаговой обработки событий)
    private val eventQueue = ArrayDeque<GameEvent>()
    // ArrayDeque - двусторонняя очередь
    // здесь будут храниться события, которые мы захотим обработать позже
    fun subscribe(listener: Listener): Int{
        val id = nextId
        nextId += 1

        listeners[id] = listener

        println("Подписчик добавлен. Id=$id. Всего подписчиков: ${listeners.size}")
        return id
    }

    fun unsubscribe(id: Int){
        val removed = listeners.remove(id)

        if (removed != null){
            println("Подписчик удалён. id = $id")
        } else {
            println("Не удалось отписаться. Не найден id = $id")
        }
    }
    fun subscribeOnce(listener: Listener): Int{
        // одноразовая подписка на события
        // Слушатель сам отпишется после первого полученного события
        var id: Int = -1
        //временная переменная для id
        id = subscribeOnce {
            event -> listener(event)

            unsubscribe(id)
            //Отреагировали и сразу отписались
        }

        return id
    }

    fun publish(event: GameEvent){
        //сразу публикует и выполняет событие, мгновенно вызывая подписчиков
        println("Событие опубликовано: $event")
        for (listener in listeners.values){
            listener(event)
        }
    }

    fun post(event: GameEvent){
        // post - отложить события в очередь выполнения (выполнится  не сразу)
        eventQueue.addLast(event)
        //addLast - добавление в конец очереди
        println("Событие $event добавлено в очередь (в очереди ${eventQueue.size})")

    }
    fun processQueue(maxEvent: Int = 10){
        var processed = 0

        while (processed < maxEvent && eventQueue.isNotEmpty()){
            val event = eventQueue.removeFirst()
            // достаёт и удаляет первый элемент из очереди

            publish(event)

            processed++
        }
    }
}