package com.example.spamshield

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.spamshield.ui.navigation.SpamShieldNavGraph
import com.example.spamshield.ui.theme.DarkBackground
import com.example.spamshield.ui.theme.DarkSurface
import com.example.spamshield.ui.theme.SpamRed
import com.example.spamshield.ui.theme.SpamshieldTheme
import com.example.spamshield.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* gracefully ignore result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            SpamshieldTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = { BottomNavBar(navController = navController) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SpamShieldNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    data class NavItem(val route: String, val icon: ImageVector, val label: String)

    val items = listOf(
        NavItem("home", Icons.Filled.Home, "Home"),
        NavItem("history", Icons.Filled.History, "History"),
        NavItem("statistics", Icons.Filled.BarChart, "Stats"),
        NavItem("settings", Icons.Filled.Settings, "Settings")
    )

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SpamRed,
                    selectedTextColor = SpamRed,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SpamRed.copy(alpha = 0.12f)
                )
            )
        }
    }
}
