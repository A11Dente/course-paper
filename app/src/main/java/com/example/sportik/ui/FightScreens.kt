package com.example.sportik.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sportik.data.Fight
import com.example.sportik.data.FightInfoRow
import com.example.sportik.data.Tournament
import com.example.sportik.data.WEIGHT_CATEGORIES

/** Раздел «Поединки», шаг 1: список турниров. */
@Composable
fun FightTournamentsScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val rows by vm.tournamentRows.collectAsStateWithLifecycle()
    EntityList(
        title = "Поединки",
        source = rows,
        onBack = onBack,
        rowTitle = { it.title },
        rowSubtitle = { t ->
            val organizer = if (t.organizer.isNullOrBlank()) "" else "Организатор: ${t.organizer}\n"
            "$organizer${t.date}\nВесовых категорий: ${t.fightCount}"
        },
        onEdit = { onOpen(it.id) }
    )
}

/** Раздел «Поединки», шаг 2: весовые категории выбранного турнира и их участники. */
@Composable
fun TournamentFightsScreen(
    vm: AppViewModel,
    tournamentId: Int,
    onBack: () -> Unit,
    onOpenFight: (Int) -> Unit
) {
    val tournaments by vm.tournaments.collectAsStateWithLifecycle()
    val flow = remember(tournamentId) { vm.tournamentFights(tournamentId) }
    val fights by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    EntityList(
        title = tournaments.find { it.id == tournamentId }?.title ?: "Поединки турнира",
        source = fights,
        onBack = onBack,
        rowTitle = { it.weightCategory },
        rowSubtitle = { f -> f.participantsText() },
        onEdit = { onOpenFight(it.id) }
    )
}

/** Добавление и изменение весовой категории внутри турнира: турнир и дата подставляются автоматически. */
@Composable
fun FightFormScreen(vm: AppViewModel, tournamentId: Int, fightId: Int, onBack: () -> Unit) {
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var weight by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(tournamentId, fightId) {
        tournament = vm.dao.tournament(tournamentId)
        if (fightId > 0) {
            vm.dao.fight(fightId)?.let { weight = it.weightCategory }
        }
    }

    FormScaffold(
        title = if (fightId == 0) "Новая весовая категория" else "Редактирование категории",
        onBack = onBack,
        onSave = { report ->
            val t = tournament
            if (t == null) {
                report("Турнир не найден")
            } else if (weight.isBlank()) {
                report("Выберите весовую категорию")
            } else {
                vm.run(report) {
                    if (fightId == 0) {
                        // дата поединка всегда равна дате турнира (контролируется триггером)
                        vm.dao.insertFight(Fight(0, t.id, t.date, weight))
                    } else {
                        vm.dao.fight(fightId)?.let { vm.dao.updateFight(it.copy(weightCategory = weight)) }
                    }
                }
            }
        }
    ) {
        Text(
            tournament?.let { "Турнир: ${it.title}\nДата: ${it.date}" } ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Sub
        )
        DropdownField(
            label = "Весовая категория",
            options = WEIGHT_CATEGORIES,
            selectedLabel = weight.ifEmpty { "Выберите категорию" },
            optionLabel = { it },
            onSelect = { weight = it }
        )
        Text(
            "Участников выберите позже в разделе «Поединки», а результат внесите в разделе «Результаты».",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Sub
        )
    }
}

/** Раздел «Поединки», шаг 3: выбор двух участников поединка. */
@Composable
fun FightPairScreen(vm: AppViewModel, fightId: Int, onBack: () -> Unit) {
    val fighters by vm.fighters.collectAsStateWithLifecycle()
    var info by remember { mutableStateOf<FightInfoRow?>(null) }
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var first by rememberSaveable { mutableIntStateOf(0) }
    var second by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(fightId) {
        val i = vm.dao.fightInfo(fightId)
        info = i
        if (i != null) {
            tournament = vm.dao.tournament(i.tournamentId)
            first = i.fighter1Id ?: 0
            second = i.fighter2Id ?: 0
        }
    }

    val category = info?.weightCategory
    // участники выбираются среди спортсменов весовой категории поединка
    val inCategory = fighters.filter { category != null && it.weightCategory == category }

    FormScaffold(
        title = "Участники поединка",
        onBack = onBack,
        onSave = { report ->
            if (first == 0 || second == 0) {
                report("Выберите двух бойцов")
            } else if (first == second) {
                report("Нужно выбрать двух разных спортсменов")
            } else {
                vm.run(report) {
                    vm.dao.fight(fightId)?.let {
                        vm.dao.updateFight(it.copy(fighter1Id = first, fighter2Id = second))
                    }
                }
            }
        }
    ) { report ->
        Text(
            info?.let {
                "Турнир: ${tournament?.title.orEmpty()}\nДата: ${it.date}\nВесовая категория: ${it.weightCategory}"
            } ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Sub
        )
        if (info != null && inCategory.size < 2) {
            Text(
                "В этой весовой категории меньше двух спортсменов. Добавьте спортсменов в разделе «Спортсмены».",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Gold
            )
        }
        DropdownField(
            label = "Боец 1",
            options = inCategory.filter { it.id != second },
            selectedLabel = inCategory.find { it.id == first }?.label() ?: "Выберите бойца",
            optionLabel = { it.label() },
            onSelect = { first = it.id }
        )
        DropdownField(
            label = "Боец 2",
            options = inCategory.filter { it.id != first },
            selectedLabel = inCategory.find { it.id == second }?.label() ?: "Выберите бойца",
            optionLabel = { it.label() },
            onSelect = { second = it.id }
        )
        if (info?.fighter1Id != null || info?.fighter2Id != null) {
            OutlinedButton(
                onClick = {
                    vm.run(report) {
                        vm.dao.fight(fightId)?.let {
                            vm.dao.updateFight(it.copy(fighter1Id = null, fighter2Id = null))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Очистить участников") }
        }
    }
}
