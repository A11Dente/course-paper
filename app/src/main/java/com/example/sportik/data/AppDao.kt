package com.example.sportik.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppDao {

    // ---------------------------------------------------------------- Клубы
    @Query("SELECT * FROM Club ORDER BY name")
    abstract fun clubs(): Flow<List<Club>>

    @Query("SELECT * FROM Club WHERE id = :id")
    abstract suspend fun club(id: Int): Club?

    @Insert
    abstract suspend fun insertClub(club: Club): Long

    @Update
    abstract suspend fun updateClub(club: Club)

    @Delete
    abstract suspend fun deleteClub(club: Club)

    // -------------------------------------------------------------- Тренеры
    @Query("SELECT * FROM Coach ORDER BY surname, name")
    abstract fun coaches(): Flow<List<Coach>>

    @Query(
        """SELECT k.id, k.surname, k.name, k.patronymic, k.phone, k.clubId, c.name AS clubName
           FROM Coach k JOIN Club c ON c.id = k.clubId
           ORDER BY k.surname, k.name"""
    )
    abstract fun coachRows(): Flow<List<CoachRow>>

    @Query("SELECT * FROM Coach WHERE id = :id")
    abstract suspend fun coach(id: Int): Coach?

    @Insert
    abstract suspend fun insertCoach(coach: Coach): Long

    @Update
    abstract suspend fun updateCoach(coach: Coach)

    @Delete
    abstract suspend fun deleteCoach(coach: Coach)

    @Query("UPDATE Fighter SET coachId = :newCoachId WHERE coachId = :oldCoachId")
    abstract suspend fun reassignFighters(oldCoachId: Int, newCoachId: Int)

    @Query("DELETE FROM Coach WHERE id = :coachId")
    abstract suspend fun deleteCoachById(coachId: Int)

    // ------------------------------------------------------------ Спортсмены
    /**
     * Список спортсменов с фильтрами (поиск в БД средствами SQL).
     * weight = "" и clubId = 0 означают «без фильтра».
     */
    @Query(
        """SELECT f.id, f.surname, f.name, f.patronymic, f.birthDate, f.weightCategory,
                  f.clubId, f.coachId, c.name AS clubName,
                  (k.surname || ' ' || k.name) AS coachName
           FROM Fighter f
           JOIN Club c ON c.id = f.clubId
           JOIN Coach k ON k.id = f.coachId
           WHERE (:weight = '' OR f.weightCategory = :weight)
             AND (:clubId = 0 OR f.clubId = :clubId)
           ORDER BY f.surname, f.name"""
    )
    abstract fun fighterRows(weight: String, clubId: Int): Flow<List<FighterRow>>

    @Query("SELECT * FROM Fighter ORDER BY surname, name")
    abstract fun fighters(): Flow<List<Fighter>>

    @Query("SELECT * FROM Fighter WHERE id = :id")
    abstract suspend fun fighter(id: Int): Fighter?

    @Insert
    abstract suspend fun insertFighter(fighter: Fighter): Long

    @Update
    abstract suspend fun updateFighter(fighter: Fighter)

    @Delete
    abstract suspend fun deleteFighter(fighter: Fighter)

    // --------------------------------------------------------------- Турниры
    @Query("SELECT * FROM Tournament ORDER BY date DESC, title")
    abstract fun tournaments(): Flow<List<Tournament>>

    @Query("SELECT * FROM Tournament WHERE id = :id")
    abstract suspend fun tournament(id: Int): Tournament?

    @Insert
    abstract suspend fun insertTournament(tournament: Tournament): Long

    @Update
    abstract suspend fun updateTournament(tournament: Tournament)

    @Delete
    abstract suspend fun deleteTournament(tournament: Tournament)

    // ------------------------------------------------------------ Поединки
    /** Турниры со сводкой по поединкам (для списков турниров). */
    @Query(
        """SELECT t.id, t.title, t.date, t.location, t.organizer,
                  (SELECT COUNT(*) FROM Fight f WHERE f.tournamentId = t.id) AS fightCount,
                  (SELECT COUNT(*) FROM Fight f JOIN Result r ON r.fightId = f.id
                    WHERE f.tournamentId = t.id) AS resultCount
           FROM Tournament t
           ORDER BY t.date DESC, t.title"""
    )
    abstract fun tournamentRows(): Flow<List<TournamentRow>>

    /** Поединки одного турнира с участниками и результатами. */
    @Query(FIGHT_INFO_SQL + " WHERE f.tournamentId = :tournamentId ORDER BY f.id")
    abstract fun tournamentFights(tournamentId: Int): Flow<List<FightInfoRow>>

    /** Все поединки всех турниров (для раздела «Результаты»). */
    @Query(FIGHT_INFO_SQL + " ORDER BY f.tournamentId, f.id")
    abstract fun allFights(): Flow<List<FightInfoRow>>

    /** Один поединок с участниками и результатом (для форм). */
    @Query(FIGHT_INFO_SQL + " WHERE f.id = :fightId")
    abstract suspend fun fightInfo(fightId: Int): FightInfoRow?

    @Query("SELECT * FROM Fight WHERE id = :id")
    abstract suspend fun fight(id: Int): Fight?

    @Insert
    abstract suspend fun insertFight(fight: Fight): Long

    @Update
    abstract suspend fun updateFight(fight: Fight)

    @Delete
    abstract suspend fun deleteFight(fight: Fight)

    // ------------------------------------------------------------ Результаты
    @Query("SELECT * FROM Result WHERE id = :id")
    abstract suspend fun result(id: Int): FightResult?

    @Query("SELECT * FROM Result WHERE fightId = :fightId LIMIT 1")
    abstract suspend fun resultByFight(fightId: Int): FightResult?

    @Query("DELETE FROM Result WHERE fightId = :fightId")
    abstract suspend fun deleteResultByFight(fightId: Int)

    @Insert
    abstract suspend fun insertResult(result: FightResult): Long

    @Update
    abstract suspend fun updateResult(result: FightResult)

    @Delete
    abstract suspend fun deleteResult(result: FightResult)

    // ------------------------------------------------------------ Статистика
    @Query(
        """SELECT (f.surname || ' ' || f.name) AS label, c.name AS extra, COUNT(r.id) AS value
           FROM Fighter f
           JOIN Club c ON c.id = f.clubId
           LEFT JOIN Result r ON r.fighterId = f.id
           GROUP BY f.id
           ORDER BY value DESC, f.surname"""
    )
    abstract fun fighterRanking(): Flow<List<StatRow>>

    @Query(
        """SELECT c.name AS label, c.city AS extra, COUNT(r.id) AS value
           FROM Club c
           LEFT JOIN Fighter f ON f.clubId = c.id
           LEFT JOIN Result r ON r.fighterId = f.id
           GROUP BY c.id
           ORDER BY value DESC, c.name"""
    )
    abstract fun clubRanking(): Flow<List<StatRow>>

    @Query(
        """SELECT victoryMethod AS label, '' AS extra, COUNT(*) AS value
           FROM Result
           GROUP BY victoryMethod
           ORDER BY value DESC"""
    )
    abstract fun methodStats(): Flow<List<StatRow>>

    // ------------------------------------------------------ Журнал (триггеры)
    @Query("SELECT * FROM AuditLog ORDER BY id DESC LIMIT 200")
    abstract fun auditLog(): Flow<List<AuditLog>>

    // ------------------------------- «Хранимые процедуры» (транзакции Room)

    /**
     * Процедура «Сохранить результат поединка»: если результат уже есть, изменяет его,
     * иначе добавляет новый. Чтение и запись выполняются в одной транзакции: если триггер
     * отклонит результат (например, победитель другой весовой категории), изменения откатываются.
     */
    @Transaction
    open suspend fun saveResult(fightId: Int, fighterId: Int, method: String, round: Int) {
        val existing = resultByFight(fightId)
        if (existing == null) {
            insertResult(
                FightResult(
                    fightId = fightId,
                    fighterId = fighterId,
                    victoryMethod = method,
                    round = round
                )
            )
        } else {
            updateResult(existing.copy(fighterId = fighterId, victoryMethod = method, round = round))
        }
    }

    /**
     * Процедура «Удалить тренера с передачей спортсменов»: переводит всех
     * спортсменов тренера к другому тренеру и удаляет тренера. Если новый тренер
     * из другого клуба, триггер отклонит перевод и вся операция откатится.
     */
    @Transaction
    open suspend fun deleteCoachReassign(coachId: Int, newCoachId: Int) {
        reassignFighters(coachId, newCoachId)
        deleteCoachById(coachId)
    }
}
