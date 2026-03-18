package Za4et

class DialogNode(
    val state: QuestState,
    val text: String
){
    private val choices = mutableMapOf<String, QuestState>()

    fun addChoice(choiceText: String, nextState: QuestState){
        choices[choiceText] = nextState
    }

    fun getNextState(choiceIndex: Int):  QuestState? {
        if (choiceIndex < 1 || choiceIndex > choices.size) return null
        return choices.values.elementAt(choiceIndex - 1)
    }

    fun print(){
        println("\n[$state]")
        println("NPC: \"$text\"")

        if (choices.isNotEmpty()) {
            println("\nВарианты:")
            var index = 1
            for ((choiceText, _) in choices) {
                println("  $index. $choiceText")
                index++
            }
        }
    }

    fun getChoicesCount(): Int = choices.size
}