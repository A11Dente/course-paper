package com.example.sportik.data

/** Результаты запросов с JOIN: названия связанных записей вместо числовых ключей. */

data class CoachRow(
    val id: Int,
    val surname: String,
    val name: String,
    val patronymic: String?,
    val phone: String?,
    val clubId: Int,
    val clubName: String
)

data class FighterRow(
    val id: Int,
    val surname: String,
    val name: String,
    val patronymic: String?,
    val birthDate: String,
    val weightCategory: String,
    val clubId: Int,
    val coachId: Int,
    val clubName: String,
    val coachName: String
)

/** Турнир со сводкой: число поединков и число поединков с результатом. */
data class TournamentRow(
    val id: Int,
    val title: String,
    val date: String,
    val location: String?,
    val organizer: String?,
    val fightCount: Int,
    val resultCount: Int
)

/** Поединок с участниками и результатом (результата и участников может ещё не быть). */
data class FightInfoRow(
    val id: Int,
    val tournamentId: Int,
    val date: String,
    val weightCategory: String,
    val fighter1Id: Int?,
    val fighter1Name: String?,
    val fighter2Id: Int?,
    val fighter2Name: String?,
    val resultId: Int?,
    val winnerId: Int?,
    val winnerName: String?,
    val victoryMethod: String?,
    val round: Int?
) {
    /** Участники одной строкой: «Иванов Иван — Козлов Михаил». */
    fun participantsText(): String = when {
        fighter1Name != null && fighter2Name != null -> "$fighter1Name — $fighter2Name"
        fighter1Name != null -> "$fighter1Name — (второй участник не выбран)"
        fighter2Name != null -> "(первый участник не выбран) — $fighter2Name"
        else -> "Участники не выбраны"
    }

    val hasBothFighters: Boolean get() = fighter1Id != null && fighter2Id != null
}

/** Общая часть запросов о поединках: участники и результат присоединяются через LEFT JOIN. */
const val FIGHT_INFO_SQL = """SELECT f.id, f.tournamentId, f.date, f.weightCategory,
       f.fighter1Id AS fighter1Id, (a.surname || ' ' || a.name) AS fighter1Name,
       f.fighter2Id AS fighter2Id, (b.surname || ' ' || b.name) AS fighter2Name,
       r.id AS resultId, r.fighterId AS winnerId,
       (w.surname || ' ' || w.name) AS winnerName,
       r.victoryMethod AS victoryMethod, r.round AS round
FROM Fight f
LEFT JOIN Fighter a ON a.id = f.fighter1Id
LEFT JOIN Fighter b ON b.id = f.fighter2Id
LEFT JOIN Result r ON r.fightId = f.id
LEFT JOIN Fighter w ON w.id = r.fighterId"""

/** Строка статистики: подпись, пояснение, числовое значение. */
data class StatRow(
    val label: String,
    val extra: String,
    val value: Int
)

/** Справочные значения, используемые в формах. */
val WEIGHT_CATEGORIES = listOf(
    "до 66 кг", "до 70 кг", "до 77 кг", "до 80 кг",
    "до 84 кг", "до 90 кг", "до 93 кг", "свыше 93 кг"
)

val VICTORY_METHODS = listOf(
    "KO", "TKO", "Сабмишн",
    "Единогласное решение", "Раздельное решение", "Дисквалификация"
)

val ROUNDS = listOf(1, 2, 3, 4, 5)
