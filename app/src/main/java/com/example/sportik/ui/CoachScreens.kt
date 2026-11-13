package com.example.sportik.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sportik.data.Coach
import com.example.sportik.data.CoachRow

@Composable
fun CoachListScreen(vm: AppViewModel, onBack: () -> Unit, onEdit: (Int) -> Unit) {
    val rows by vm.coachRows.collectAsStateWithLifecycle()
    // Тренер, которому передаются спортсмены удаляемого тренера (0 — не передавать)
    var reassignTo by rememberSaveable { mutableIntStateOf(0) }

    EntityList(
        title = "Тренеры",
        source = rows,
        onBack = onBack,
        onAdd = { onEdit(0) },
        rowTitle = { fullName(it.surname, it.name, it.patronymic) },
        rowSubtitle = { "Клуб: ${it.clubName}" + if (it.phone.isNullOrBlank()) "" else "\n${it.phone}" },
        onEdit = { onEdit(it.id) },
        onDelete = { row, done ->
            val target = reassignTo
            reassignTo = 0
            vm.run(done) {
                if (target > 0) {
                    vm.dao.deleteCoachReassign(row.id, target)
                } else {
                    vm.dao.deleteCoachById(row.id)
                }
            }
        },
        deleteExtra = { row: CoachRow ->
            LaunchedEffect(row.id) { reassignTo = 0 }
            val colleagues = rows.filter { it.clubId == row.clubId && it.id != row.id }
            Text(
                "Если у тренера есть спортсмены, передайте их другому тренеру клуба:",
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
            DropdownField(
                label = "Передать спортсменов",
                options = listOf<CoachRow?>(null) + colleagues,
                selectedLabel = colleagues.find { it.id == reassignTo }
                    ?.let { fullName(it.surname, it.name, it.patronymic) } ?: "Не передавать",
                optionLabel = { it?.let { c -> fullName(c.surname, c.name, c.patronymic) } ?: "Не передавать" },
                onSelect = { reassignTo = it?.id ?: 0 }
            )
        }
    )
}

@Composable
fun CoachFormScreen(vm: AppViewModel, id: Int, onBack: () -> Unit) {
    val clubs by vm.clubs.collectAsStateWithLifecycle()
    var surname by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var patronymic by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var clubId by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(id) {
        if (id > 0) {
            vm.dao.coach(id)?.let {
                surname = it.surname
                name = it.name
                patronymic = it.patronymic.orEmpty()
                phone = it.phone.orEmpty()
                clubId = it.clubId
            }
        }
    }

    FormScaffold(
        title = if (id == 0) "Новый тренер" else "Редактирование тренера",
        onBack = onBack,
        onSave = { report ->
            if (surname.isBlank() || name.isBlank()) {
                report("Заполните фамилию и имя")
            } else if (clubId == 0) {
                report("Выберите клуб")
            } else {
                val coach = Coach(
                    id, surname.trim(), name.trim(),
                    patronymic.trim().ifEmpty { null },
                    phone.trim().ifEmpty { null },
                    clubId
                )
                vm.run(report) {
                    if (id == 0) {
                        vm.dao.insertCoach(coach)
                    } else {
                        vm.dao.updateCoach(coach)
                    }
                }
            }
        }
    ) {
        Field("Фамилия", surname, { surname = it })
        Field("Имя", name, { name = it })
        Field("Отчество", patronymic, { patronymic = it })
        Field("Телефон", phone, { phone = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
        DropdownField(
            label = "Клуб",
            options = clubs,
            selectedLabel = clubs.find { it.id == clubId }?.label() ?: "Выберите клуб",
            optionLabel = { it.label() },
            onSelect = { clubId = it.id }
        )
    }
}
