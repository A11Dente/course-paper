package com.example.sportik.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sportik.data.FightInfoRow
import com.example.sportik.data.Tournament
import kotlinx.coroutines.launch

/** Список турниров: название, организатор, дата, число поединков. */
@Composable
fun TournamentListScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (Int) -> Unit, onAdd: () -> Unit) {
    val rows by vm.tournamentRows.collectAsStateWithLifecycle()
    EntityList(
        title = "Турниры",
        source = rows,
        onBack = onBack,
        onAdd = onAdd,
        rowTitle = { it.title },
        rowSubtitle = { t ->
            val organizer = if (t.organizer.isNullOrBlank()) "" else "Организатор: ${t.organizer}\n"
            val place = listOf(t.date, t.location.orEmpty()).filter { s -> s.isNotBlank() }.joinToString(" · ")
            "$organizer$place\nВесовых категорий: ${t.fightCount}"
        },
        onEdit = { onOpen(it.id) },
        onDelete = { row, done ->
            vm.run(done) { vm.dao.tournament(row.id)?.let { vm.dao.deleteTournament(it) } }
        },
        deleteExtra = { Text("Весовые категории этого турнира, их участники и результаты тоже будут удалены.") }
    )
}

/**
 * Экран турнира: сведения о турнире и его весовые категории (участники и победитель — для просмотра).
 * Здесь добавляются, изменяются и удаляются весовые категории; участников выбирают в разделе «Поединки»,
 * а результаты вносят в разделе «Результаты».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    vm: AppViewModel,
    id: Int,
    onBack: () -> Unit,
    onEditTournament: () -> Unit,
    onAddFight: () -> Unit,
    onEditFight: (Int) -> Unit
) {
    val tournaments by vm.tournaments.collectAsStateWithLifecycle()
    val tournament = tournaments.find { it.id == id }
    val flow = remember(id) { vm.tournamentFights(id) }
    val fights by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    var toDelete by remember { mutableStateOf<FightInfoRow?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = tournament?.title ?: "Турнир",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEditTournament) {
                        Icon(Icons.Default.Edit, contentDescription = "Изменить турнир")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFight,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Весовая категория")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            if (tournament != null) {
                item { TournamentInfoCard(tournament) }
            }
            item {
                Text(
                    "Весовые категории (${fights.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (fights.isEmpty()) {
                item {
                    Text(
                        "Весовых категорий пока нет. Нажмите «Весовая категория», чтобы добавить.",
                        color = AppColors.Sub
                    )
                }
            }
            items(fights) { f ->
                AppCard(Modifier.fillMaxWidth(), onClick = { onEditFight(f.id) }) {
                    Row(
                        Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            CategoryChip(f.weightCategory)
                            Text(
                                f.participantsText(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            if (f.winnerName != null) {
                                Text(
                                    "Победил: ${f.winnerName} (${f.victoryMethod}, раунд ${f.round})",
                                    color = AppColors.Green,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        IconButton(onClick = { toDelete = f }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить весовую категорию")
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { f ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить весовую категорию?") },
            text = { Text("Категория ${f.weightCategory}, её участники и результат будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    vm.run({ error -> if (error != null) scope.launch { snackbar.showSnackbar(error) } }) {
                        vm.dao.fight(f.id)?.let { vm.dao.deleteFight(it) }
                    }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun TournamentInfoCard(t: Tournament) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                listOf(t.date, t.location.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.titleMedium
            )
            if (!t.organizer.isNullOrBlank()) {
                Text(
                    "Организатор: ${t.organizer}",
                    color = AppColors.Sub,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TournamentFormScreen(vm: AppViewModel, id: Int, onBack: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var organizer by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id > 0) {
            vm.dao.tournament(id)?.let {
                title = it.title
                date = it.date
                location = it.location.orEmpty()
                organizer = it.organizer.orEmpty()
            }
        }
    }

    FormScaffold(
        title = if (id == 0) "Новый турнир" else "Редактирование турнира",
        onBack = onBack,
        onSave = { report ->
            if (title.isBlank()) {
                report("Введите название турнира")
            } else if (date.isBlank()) {
                report("Укажите дату турнира")
            } else {
                val t = Tournament(
                    id, title.trim(), date,
                    location.trim().ifEmpty { null },
                    organizer.trim().ifEmpty { null }
                )
                vm.run(report) {
                    if (id == 0) {
                        vm.dao.insertTournament(t)
                    } else {
                        vm.dao.updateTournament(t)
                    }
                }
            }
        }
    ) {
        Field("Название", title, { title = it })
        DateField("Дата проведения", date) { date = it }
        Field("Место проведения", location, { location = it })
        Field("Организатор", organizer, { organizer = it })
    }
}
