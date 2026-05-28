package com.example.spamshield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.spamshield.ui.screens.HistoryScreen
import com.example.spamshield.ui.screens.HomeScreen
import com.example.spamshield.ui.screens.SettingsScreen
import com.example.spamshield.ui.screens.StatisticsScreen
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel

@Composable
fun SpamShieldNavGraph(navController: NavHostController) {
    val viewModel: SpamShieldViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel = viewModel)
        }
        composable("history") {
            HistoryScreen(viewModel = viewModel)
        }
        composable("statistics") {
            StatisticsScreen(viewModel = viewModel)
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
