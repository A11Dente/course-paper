package com.example.sportik.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Клуб */
@Entity(tableName = "Club")
data class Club(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val city: String,
    val contactInfo: String? = null
)

/** Тренер (принадлежит клубу) */
@Entity(
    tableName = "Coach",
    foreignKeys = [
        ForeignKey(
            entity = Club::class,
            parentColumns = ["id"],
            childColumns = ["clubId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("clubId")]
)
data class Coach(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surname: String,
    val name: String,
    val patronymic: String? = null,
    val phone: String? = null,
    val clubId: Int
)

/** Спортсмен (клуб + тренер + весовая категория) */
@Entity(
    tableName = "Fighter",
    foreignKeys = [
        ForeignKey(
            entity = Club::class,
            parentColumns = ["id"],
            childColumns = ["clubId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Coach::class,
            parentColumns = ["id"],
            childColumns = ["coachId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("clubId"), Index("coachId")]
)
data class Fighter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surname: String,
    val name: String,
    val patronymic: String? = null,
    val birthDate: String,
    val weightCategory: String,
    val clubId: Int,
    val coachId: Int
)

/** Турнир */
@Entity(tableName = "Tournament")
data class Tournament(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String,
    val location: String? = null,
    val organizer: String? = null
)

/**
 * Поединок на турнире: весовая категория турнира и два участника (участники выбираются
 * после создания категории). При удалении турнира удаляется каскадом.
 */
@Entity(
    tableName = "Fight",
    foreignKeys = [
        ForeignKey(
            entity = Tournament::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Fighter::class,
            parentColumns = ["id"],
            childColumns = ["fighter1Id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Fighter::class,
            parentColumns = ["id"],
            childColumns = ["fighter2Id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("tournamentId"), Index("fighter1Id"), Index("fighter2Id")]
)
data class Fight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tournamentId: Int,
    val date: String,
    val weightCategory: String,
    val fighter1Id: Int? = null,
    val fighter2Id: Int? = null
)

/** Результат поединка: победитель, способ победы, раунд */
@Entity(
    tableName = "Result",
    foreignKeys = [
        ForeignKey(
            entity = Fight::class,
            parentColumns = ["id"],
            childColumns = ["fightId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Fighter::class,
            parentColumns = ["id"],
            childColumns = ["fighterId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("fightId"), Index("fighterId")]
)
data class FightResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fightId: Int,
    val fighterId: Int,
    val victoryMethod: String,
    val round: Int
)

/** Журнал изменений: заполняется только триггерами БД */
@Entity(tableName = "AuditLog")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tableName: String,
    val operation: String,
    val recordId: Int,
    val details: String,
    val createdAt: String
)
