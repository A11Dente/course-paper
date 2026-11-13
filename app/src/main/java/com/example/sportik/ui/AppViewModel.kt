package com.example.sportik.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sportik.data.AppDatabase
import com.example.sportik.data.Club
import com.example.sportik.data.Coach
import com.example.sportik.data.Fighter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val dao = AppDatabase.get(app).dao()

    private fun <T> Flow<List<T>>.hot(): StateFlow<List<T>> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clubs = dao.clubs().hot()
    val coaches = dao.coaches().hot()
    val coachRows = dao.coachRows().hot()
    val fighters = dao.fighters().hot()
    val tournaments = dao.tournaments().hot()
    val tournamentRows = dao.tournamentRows().hot()
    val allFights = dao.allFights().hot()
    val fighterRanking = dao.fighterRanking().hot()
    val clubRanking = dao.clubRanking().hot()
    val methodStats = dao.methodStats().hot()
    val auditLog = dao.auditLog().hot()

    /** Поединки одного турнира вместе с результатами. */
    fun tournamentFights(tournamentId: Int) = dao.tournamentFights(tournamentId)

    /** Поиск спортсменов с фильтрами по категории и клубу (SQL WHERE). */
    fun fighterRows(weight: String, clubId: Int) = dao.fighterRows(weight, clubId)

    /**
     * Выполняет операцию над БД и сообщает результат: null — успех,
     * иначе текст ошибки (например, сообщение сработавшего триггера).
     */
    fun run(onResult: (String?) -> Unit, block: suspend () -> Unit) {
        viewModelScope.launch {
            val error = try {
                block()
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                friendlyMessage(e)
            }
            onResult(error)
        }
    }

    private fun friendlyMessage(e: Exception): String {
        val raw = e.message ?: return "Не удалось выполнить операцию"
        return when {
            raw.contains("FOREIGN KEY", ignoreCase = true) ->
                "Операция невозможна: запись связана с другими данными"
            // Сообщение триггера приходит как «текст (code 1811 SQLITE_CONSTRAINT_TRIGGER)»
            else -> raw.substringBefore(" (code").trim().ifEmpty { "Не удалось выполнить операцию" }
        }
    }
}

/** ФИО одной строкой. */
fun fullName(surname: String, name: String, patronymic: String?): String =
    listOf(surname, name, patronymic.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")

fun Club.label() = "$name ($city)"
fun Coach.label() = fullName(surname, name, patronymic)
fun Fighter.label() = fullName(surname, name, patronymic)
