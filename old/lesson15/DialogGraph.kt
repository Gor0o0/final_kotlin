package lesson15

class DialogGraph{
    private val nodes = mutableMapOf<DialogState, DialogNode>()

    init{
        val greeting = DialogNode(
            DialogState.GREETING,
            "Привет, доходяга"
        )
        val offer = DialogNode(
            DialogState.OFFER_QUEST,
            "Наш угрожает Кирилл шаман я русский."
        )
        val accepted = DialogNode(
            DialogState.ACCEPTED,
            "Отлично, примеси мне его голову."
        )
        val refused = DialogNode(
            DialogState.REFUSED,
            "Жаль, тогда проваривай отсюда и не возвращайся, гнойный!"
        )
        val completed = DialogNode(
            DialogState.QUEST_COMPLETED,
            "Отлично! Держи награду"
        )
        greeting.addChoice("work", DialogState.OFFER_QUEST)
        greeting.addChoice("bye", DialogState.END)

        offer.addChoice("accept", DialogState.ACCEPTED)
        offer.addChoice("refuse", DialogState.REFUSED)

        accepted.addChoice("bye", DialogState.END)
        refused.addChoice("bye", DialogState.END)
        completed.addChoice("bye", DialogState.END)

        listOf(greeting, offer, accepted, refused, completed).forEach {
            nodes[it.state] = it
        }
    }
    fun getNode(state: DialogState): DialogNode{
        return nodes[state]!!
    }
}