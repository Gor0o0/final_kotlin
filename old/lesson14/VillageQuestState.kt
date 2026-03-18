package lesson14

// 1 путь.
// - Поговорить со Старым
// - Согласиться на помощь
// Убить Кирилла-Шамана
// Вернуться и доложить об выполнении
// КОНЦОВКА - ГЕРОЙ ДЕРЕВНИ

//2 Путь
// - Говорит
// - Соглашается
// - Не убивает Кирилла
// - Договаривается с ним
// КОНЦОВКА - МИРНЫЙ ДОГОВОР

// 3 путь
// Говорит
// Отказывается
// Помогает Кириллу
// КОНЦОВКА - ДЕРЕВНЯ В ОГНЕ - АЧИВКА КАКОЙ ЦЕНОЙ
enum class  VillageQuestState{
    NOT_STARTED,
    TALKED_TO_ELDER,

    ACCEPTED_HELP,
    REFUSED_HELP,

    KILLED_KIRILL_SHAMAN,
    MADE_PEACE,
    HELPED_KIRILL,

    KILLED_ALL,

    SECRET_ENDING,
    HERO_ENDING,
    PEACE_ENDING,
    BAD_ENDING
}