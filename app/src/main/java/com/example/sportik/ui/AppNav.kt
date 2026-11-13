package com.example.sportik.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sportik.R

private data class HomeItem(
    val title: String,
    val route: String,
    val color: Color,
    val vector: ImageVector? = null,
    @DrawableRes val res: Int = 0
)

private val homeItems = listOf(
    HomeItem("Клубы", "clubs", AppColors.Red, vector = Icons.Default.Home),
    HomeItem("Тренеры", "coaches", AppColors.Gold, vector = Icons.Default.Person),
    HomeItem("Спортсмены", "fighters", AppColors.Blue, res = R.drawable.ic_group),
    HomeItem("Турниры", "tournaments", AppColors.Green, vector = Icons.Default.DateRange),
    HomeItem("Поединки", "fights", AppColors.Red, res = R.drawable.ic_glove),
    HomeItem("Результаты", "results", AppColors.Gold, vector = Icons.Default.Star),
    HomeItem("Рейтинг и статистика", "stats", AppColors.Blue, res = R.drawable.ic_bars),
    HomeItem("Журнал изменений", "audit", AppColors.Sub, res = R.drawable.ic_history)
)

@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    Scaffold(topBar = { AppTopBar("Соревнования") }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(homeItems) { item -> HomeTile(item, onOpen) }
        }
    }
}

@Composable
private fun HomeTile(item: HomeItem, onOpen: (String) -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(item.route) }) {
        Column(
            Modifier
                .padding(16.dp)
                .height(110.dp)
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(item.color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (item.vector != null) {
                    Icon(item.vector, contentDescription = null, tint = item.color, modifier = Modifier.size(26.dp))
                } else {
                    Icon(
                        painterResource(item.res),
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(item.color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    val idArg = listOf(navArgument("id") { type = NavType.IntType })

    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(onOpen = { nav.navigate(it) }) }

        // справочники
        composable("clubs") { ClubListScreen(vm, back) { nav.navigate("club/$it") } }
        composable("club/{id}", arguments = idArg) { e ->
            ClubFormScreen(vm, e.arguments?.getInt("id") ?: 0, back)
        }
        composable("coaches") { CoachListScreen(vm, back) { nav.navigate("coach/$it") } }
        composable("coach/{id}", arguments = idArg) { e ->
            CoachFormScreen(vm, e.arguments?.getInt("id") ?: 0, back)
        }
        composable("fighters") { FighterListScreen(vm, back) { nav.navigate("fighter/$it") } }
        composable("fighter/{id}", arguments = idArg) { e ->
            FighterFormScreen(vm, e.arguments?.getInt("id") ?: 0, back)
        }

        // турниры: список -> турнир (поединки) -> форма поединка
        composable("tournaments") {
            TournamentListScreen(
                vm, back,
                onOpen = { nav.navigate("tournament/$it") },
                onAdd = { nav.navigate("tournamentForm/0") }
            )
        }
        composable("tournament/{id}", arguments = idArg) { e ->
            val id = e.arguments?.getInt("id") ?: 0
            TournamentDetailScreen(
                vm, id, back,
                onEditTournament = { nav.navigate("tournamentForm/$id") },
                onAddFight = { nav.navigate("fightForm/$id/0") },
                onEditFight = { nav.navigate("fightForm/$id/$it") }
            )
        }
        composable("tournamentForm/{id}", arguments = idArg) { e ->
            TournamentFormScreen(vm, e.arguments?.getInt("id") ?: 0, back)
        }
        composable(
            "fightForm/{tournamentId}/{fightId}",
            arguments = listOf(
                navArgument("tournamentId") { type = NavType.IntType },
                navArgument("fightId") { type = NavType.IntType }
            )
        ) { e ->
            FightFormScreen(
                vm,
                e.arguments?.getInt("tournamentId") ?: 0,
                e.arguments?.getInt("fightId") ?: 0,
                back
            )
        }

        // поединки: список турниров -> весовые категории турнира -> выбор двух участников
        composable("fights") { FightTournamentsScreen(vm, back) { nav.navigate("fights/$it") } }
        composable(
            "fights/{tournamentId}",
            arguments = listOf(navArgument("tournamentId") { type = NavType.IntType })
        ) { e ->
            TournamentFightsScreen(
                vm,
                e.arguments?.getInt("tournamentId") ?: 0,
                back
            ) { nav.navigate("fightPair/$it") }
        }
        composable(
            "fightPair/{fightId}",
            arguments = listOf(navArgument("fightId") { type = NavType.IntType })
        ) { e ->
            FightPairScreen(vm, e.arguments?.getInt("fightId") ?: 0, back)
        }

        // результаты: турниры -> поединки -> внесение и изменение результата
        composable("results") { ResultsScreen(vm, back) { nav.navigate("result/$it") } }
        composable(
            "result/{fightId}",
            arguments = listOf(navArgument("fightId") { type = NavType.IntType })
        ) { e ->
            FightResultScreen(vm, e.arguments?.getInt("fightId") ?: 0, back)
        }
        composable("stats") { StatsScreen(vm, back) }
        composable("audit") { AuditScreen(vm, back) }
    }
}
