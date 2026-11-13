package com.example.sportik

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sportik.ui.AppNav
import com.example.sportik.ui.AppViewModel
import com.example.sportik.ui.SportikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // тёмное оформление: светлые значки в строке состояния и панели навигации
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            SportikTheme {
                val vm: AppViewModel = viewModel()
                AppNav(vm)
            }
        }
    }
}
