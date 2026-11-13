package com.example.sportik.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Палитра «Тёмный графит»: графитовый фон, красный акцент, цветные значки разделов. */
object AppColors {
    val Bg = Color(0xFF0E1014)
    val Bar = Color(0xFF161A21)
    val Card = Color(0xFF1A1F28)
    val Border = Color(0xFF2A303C)
    val Field = Color(0xFF4A5168)
    val Text = Color(0xFFF2F4F8)
    val Sub = Color(0xFF9AA4B2)
    val Red = Color(0xFFE0463E)
    val Gold = Color(0xFFE8A33D)
    val Blue = Color(0xFF4DA3FF)
    val Green = Color(0xFF7BD389)
    val ChipBg = Color(0xFF2B2F3A)
}

private val Scheme = darkColorScheme(
    primary = AppColors.Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A1D1A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = AppColors.Gold,
    background = AppColors.Bg,
    onBackground = AppColors.Text,
    surface = AppColors.Bg,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.Card,
    onSurfaceVariant = AppColors.Sub,
    surfaceContainerLowest = AppColors.Bg,
    surfaceContainerLow = AppColors.Card,
    surfaceContainer = AppColors.Card,
    surfaceContainerHigh = AppColors.Card,
    surfaceContainerHighest = AppColors.Card,
    outline = AppColors.Field,
    outlineVariant = AppColors.Border
)

@Composable
fun SportikTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = AppColors.Bar,
    titleContentColor = AppColors.Text,
    navigationIconContentColor = AppColors.Text,
    actionIconContentColor = AppColors.Text
)
