package com.example.sportik.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sportik.data.FightInfoRow
import com.example.sportik.data.ROUNDS
import com.example.sportik.data.Tournament
import com.example.sportik.data.VICTORY_METHODS
import com.example.sportik.data.WEIGHT_CATEGORIES

/**
 * Раздел «Результаты»: список турниров; турнир раскрывается и показывает свои поединки
 * (весовая категория, участники, победитель). Нажатие на поединок открывает форму
 * внесения или изменения результата.
 */
@Composable
fun ResultsScreen(vm: AppViewModel, onBack: () -> Unit, onOpenFight: (Int) -> Unit) {
    val tournaments by vm.tournaments.collectAsStateWithLifecycle()
    val fights by vm.allFights.collectAsStateWithLifecycle()
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }
    val byTournament = fights.groupBy { it.tournamentId }

    Scaffold(topBar = { AppTopBar("Результаты", onBack) }) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(tournaments) { t ->
                val rows = byTournament[t.id].orEmpty().sortedWith(
                    compareBy<FightInfoRow>(
                        { WEIGHT_CATEGORIES.indexOf(it.weightCategory) },
                        { it.id }
                    )
                )
                val done = rows.count { it.resultId != null }
                val open = expanded[t.id] == true
                AppCard(Modifier.fillMaxWidth(), onClick = { expanded[t.id] = !open }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${t.date} · результатов: $done из ${rows.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.Sub
                                )
                            }
                            Icon(
                                if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (open) "Свернуть" else "Раскрыть",
                                tint = AppColors.Red
                            )
                        }
                        if (open) {
                            HorizontalDivider(
                                color = AppColors.Border,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            if (rows.isEmpty()) {
                                Text(
                                    "В турнире пока нет весовых категорий",
                                    color = AppColors.Sub,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                            rows.forEach { f -> FightResultRow(f, onOpenFight) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FightResultRow(f: FightInfoRow, onOpenFight: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpenFight(f.id) }
            .padding(top = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(f.weightCategory)
            Text(
                if (f.resultId != null) "Изменить результат" else "Внести результат",
                color = AppColors.Red,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            f.participantsText(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (f.winnerName != null) {
            Text(
                "Победил: ${f.winnerName}",
                color = AppColors.Green,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${f.victoryMethod} · раунд ${f.round}",
                color = AppColors.Sub,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text("Результат не внесён", color = AppColors.Sub, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Внесение и изменение результата поединка: победитель выбирается из двух участников. */
@Composable
fun FightResultScreen(vm: AppViewModel, fightId: Int, onBack: () -> Unit) {
    val fighters by vm.fighters.collectAsStateWithLifecycle()
    var info by remember { mutableStateOf<FightInfoRow?>(null) }
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var fighterId by rememberSaveable { mutableIntStateOf(0) }
    var method by rememberSaveable { mutableStateOf("") }
    var round by rememberSaveable { mutableIntStateOf(1) }

    LaunchedEffect(fightId) {
        val i = vm.dao.fightInfo(fightId)
        info = i
        if (i != null) {
            tournament = vm.dao.tournament(i.tournamentId)
            if (i.winnerId != null) {
                fighterId = i.winnerId
                method = i.victoryMethod.orEmpty()
                round = i.round ?: 1
            }
        }
    }

    val current = info
    val participants = fighters.filter {
        current != null && (it.id == current.fighter1Id || it.id == current.fighter2Id)
    }
    val ready = current != null && current.hasBothFighters

    FormScaffold(
        title = if (current?.resultId != null) "Изменение результата" else "Внесение результата",
        onBack = onBack,
        onSave = { report ->
            if (!ready) {
                report("Сначала выберите двух участников в разделе «Поединки»")
            } else if (fighterId == 0) {
                report("Выберите победителя")
            } else if (method.isBlank()) {
                report("Выберите способ победы")
            } else {
                // процедура: добавление или изменение результата одной транзакцией
                vm.run(report) { vm.dao.saveResult(fightId, fighterId, method, round) }
            }
        }
    ) { report ->
        Text(
            current?.let {
                "Турнир: ${tournament?.title.orEmpty()}\nДата: ${it.date}\n" +
                    "Весовая категория: ${it.weightCategory}\nУчастники: ${it.participantsText()}"
            } ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Sub
        )
        if (current != null && !ready) {
            Text(
                "Для внесения результата сначала выберите двух участников в разделе «Поединки».",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Gold
            )
        }
        DropdownField(
            label = "Победитель",
            options = participants,
            selectedLabel = participants.find { it.id == fighterId }?.label() ?: "Выберите победителя",
            optionLabel = { it.label() },
            onSelect = { fighterId = it.id }
        )
        DropdownField(
            label = "Способ победы",
            options = VICTORY_METHODS,
            selectedLabel = method.ifEmpty { "Выберите способ" },
            optionLabel = { it },
            onSelect = { method = it }
        )
        DropdownField(
            label = "Раунд",
            options = ROUNDS,
            selectedLabel = round.toString(),
            optionLabel = { it.toString() },
            onSelect = { round = it }
        )
        if (current?.resultId != null) {
            OutlinedButton(
                onClick = { vm.run(report) { vm.dao.deleteResultByFight(fightId) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Удалить результат") }
        }
    }
}
