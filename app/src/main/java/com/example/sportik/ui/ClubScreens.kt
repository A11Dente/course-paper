package com.example.sportik.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sportik.data.Club

@Composable
fun ClubListScreen(vm: AppViewModel, onBack: () -> Unit, onEdit: (Int) -> Unit) {
    val clubs by vm.clubs.collectAsStateWithLifecycle()
    EntityList(
        title = "Клубы",
        source = clubs,
        onBack = onBack,
        onAdd = { onEdit(0) },
        rowTitle = { it.name },
        rowSubtitle = { listOf(it.city, it.contactInfo.orEmpty()).filter { s -> s.isNotBlank() }.joinToString("\n") },
        onEdit = { onEdit(it.id) },
        onDelete = { club, done -> vm.run(done) { vm.dao.deleteClub(club) } }
    )
}

@Composable
fun ClubFormScreen(vm: AppViewModel, id: Int, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id > 0) {
            vm.dao.club(id)?.let {
                name = it.name
                city = it.city
                contact = it.contactInfo.orEmpty()
            }
        }
    }

    FormScaffold(
        title = if (id == 0) "Новый клуб" else "Редактирование клуба",
        onBack = onBack,
        onSave = { report ->
            if (name.isBlank() || city.isBlank()) {
                report("Заполните название и город")
            } else {
                val club = Club(id, name.trim(), city.trim(), contact.trim().ifEmpty { null })
                vm.run(report) {
                    if (id == 0) {
                        vm.dao.insertClub(club)
                    } else {
                        vm.dao.updateClub(club)
                    }
                }
            }
        }
    ) {
        Field("Название", name, { name = it })
        Field("Город", city, { city = it })
        Field("Контакты (адрес, телефон, email)", contact, { contact = it }, singleLine = false)
    }
}
