package com.example.sportik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sportik.data.Club
import com.example.sportik.data.Fighter
import com.example.sportik.data.WEIGHT_CATEGORIES

@Composable
fun FighterListScreen(vm: AppViewModel, onBack: () -> Unit, onEdit: (Int) -> Unit) {
    val clubs by vm.clubs.collectAsStateWithLifecycle()
    var weight by rememberSaveable { mutableStateOf("") }
    var clubId by rememberSaveable { mutableIntStateOf(0) }

    // Запрос с фильтрами (WHERE) выполняется заново при смене фильтра
    val flow = remember(weight, clubId) { vm.fighterRows(weight, clubId) }
    val rows by flow.collectAsStateWithLifecycle(initialValue = emptyList())

    EntityList(
        title = "Спортсмены",
        source = rows,
        onBack = onBack,
        onAdd = { onEdit(0) },
        rowTitle = { fullName(it.surname, it.name, it.patronymic) },
        rowSubtitle = {
            "${it.weightCategory} · ${it.clubName}\nТренер: ${it.coachName}\nДата рождения: ${it.birthDate}"
        },
        onEdit = { onEdit(it.id) },
        onDelete = { row, done ->
            vm.run(done) {
                vm.dao.fighter(row.id)?.let { vm.dao.deleteFighter(it) }
            }
        },
        filters = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    DropdownField(
                        label = "Категория",
                        options = listOf("") + WEIGHT_CATEGORIES,
                        selectedLabel = weight.ifEmpty { "Все" },
                        optionLabel = { it.ifEmpty { "Все" } },
                        onSelect = { weight = it }
                    )
                }
                Box(Modifier.weight(1f)) {
                    DropdownField(
                        label = "Клуб",
                        options = listOf<Club?>(null) + clubs,
                        selectedLabel = clubs.find { it.id == clubId }?.name ?: "Все",
                        optionLabel = { it?.name ?: "Все" },
                        onSelect = { clubId = it?.id ?: 0 }
                    )
                }
            }
        }
    )
}

@Composable
fun FighterFormScreen(vm: AppViewModel, id: Int, onBack: () -> Unit) {
    val clubs by vm.clubs.collectAsStateWithLifecycle()
    val coaches by vm.coaches.collectAsStateWithLifecycle()

    var surname by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var patronymic by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var clubId by rememberSaveable { mutableIntStateOf(0) }
    var coachId by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(id) {
        if (id > 0) {
            vm.dao.fighter(id)?.let {
                surname = it.surname
                name = it.name
                patronymic = it.patronymic.orEmpty()
                birthDate = it.birthDate
                weight = it.weightCategory
                clubId = it.clubId
                coachId = it.coachId
            }
        }
    }

    val clubCoaches = coaches.filter { it.clubId == clubId }

    FormScaffold(
        title = if (id == 0) "Новый спортсмен" else "Редактирование спортсмена",
        onBack = onBack,
        onSave = { report ->
            if (surname.isBlank() || name.isBlank()) {
                report("Заполните фамилию и имя")
            } else if (birthDate.isBlank()) {
                report("Укажите дату рождения")
            } else if (weight.isBlank()) {
                report("Выберите весовую категорию")
            } else if (clubId == 0 || coachId == 0) {
                report("Выберите клуб и тренера")
            } else {
                val fighter = Fighter(
                    id, surname.trim(), name.trim(),
                    patronymic.trim().ifEmpty { null },
                    birthDate, weight, clubId, coachId
                )
                vm.run(report) {
                    if (id == 0) {
                        vm.dao.insertFighter(fighter)
                    } else {
                        vm.dao.updateFighter(fighter)
                    }
                }
            }
        }
    ) {
        Field("Фамилия", surname, { surname = it })
        Field("Имя", name, { name = it })
        Field("Отчество", patronymic, { patronymic = it })
        DateField("Дата рождения", birthDate) { birthDate = it }
        DropdownField(
            label = "Весовая категория",
            options = WEIGHT_CATEGORIES,
            selectedLabel = weight.ifEmpty { "Выберите категорию" },
            optionLabel = { it },
            onSelect = { weight = it }
        )
        DropdownField(
            label = "Клуб",
            options = clubs,
            selectedLabel = clubs.find { it.id == clubId }?.label() ?: "Выберите клуб",
            optionLabel = { it.label() },
            onSelect = {
                clubId = it.id
                // тренер должен быть из выбранного клуба
                if (coaches.find { c -> c.id == coachId }?.clubId != it.id) coachId = 0
            }
        )
        DropdownField(
            label = "Тренер (из выбранного клуба)",
            options = clubCoaches,
            selectedLabel = clubCoaches.find { it.id == coachId }?.label()
                ?: if (clubId == 0) "Сначала выберите клуб" else "Выберите тренера",
            optionLabel = { it.label() },
            onSelect = { coachId = it.id }
        )
    }
}
