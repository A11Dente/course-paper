package com.example.sportik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sportik.data.StatRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val fighters by vm.fighterRanking.collectAsStateWithLifecycle()
    val clubs by vm.clubRanking.collectAsStateWithLifecycle()
    val methods by vm.methodStats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar("Рейтинг и статистика", onBack)
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { StatSection("Рейтинг спортсменов (число побед)", fighters) }
            item { StatSection("Рейтинг клубов (число побед)", clubs) }
            item { StatSection("Способы побед", methods) }
        }
    }
}

@Composable
private fun StatSection(title: String, rows: List<StatRow>) {
    val max = (rows.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (rows.isEmpty()) Text("Нет данных")
            rows.forEachIndexed { index, row ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${index + 1}. ${row.label}" + if (row.extra.isBlank()) "" else " (${row.extra})",
                            modifier = Modifier.weight(1f)
                        )
                        Text(row.value.toString(), style = MaterialTheme.typography.titleSmall)
                    }
                    LinearProgressIndicator(
                        progress = { row.value.toFloat() / max },
                        color = AppColors.Red,
                        trackColor = AppColors.Border,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(vm: AppViewModel, onBack: () -> Unit) {
    val log by vm.auditLog.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar("Журнал изменений", onBack)
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Записи создаются автоматически триггерами БД при изменении спортсменов и результатов.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (log.isEmpty()) {
                Text("Журнал пока пуст. Измените данные спортсмена или результата — запись появится здесь.")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(log) { entry ->
                    val operation = when (entry.operation) {
                        "INSERT" -> "Добавление"
                        "UPDATE" -> "Изменение"
                        "DELETE" -> "Удаление"
                        else -> entry.operation
                    }
                    val table = when (entry.tableName) {
                        "Fighter" -> "спортсмена"
                        "Result" -> "результата"
                        else -> entry.tableName
                    }
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("$operation $table №${entry.recordId}", style = MaterialTheme.typography.titleSmall)
                            Text(entry.details)
                            Text(entry.createdAt, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
