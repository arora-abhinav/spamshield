package com.example.spamshield

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.spamshield.ui.viewmodel.SpamShieldViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.spamshield.token.TokenManager
import com.example.spamshield.ui.navigation.SpamShieldNavGraph
import com.example.spamshield.ui.theme.DarkBackground
import com.example.spamshield.ui.theme.DarkSurface
import com.example.spamshield.ui.theme.SpamRed
import com.example.spamshield.ui.theme.SpamshieldTheme
import com.example.spamshield.ui.theme.TextSecondary

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SpamShieldViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* gracefully ignore result */ }

    private val smsPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if ((readGranted || receiveGranted) && TokenManager.getPreviousMsgConsent(this)) {
            viewModel.loadInboxMessages(this)
        }
    }

    private fun launchPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        smsPermissionsLauncher.launch(
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpamshieldTheme {
                var showDisclosure by remember {
                    mutableStateOf(!TokenManager.hasSeenDisclosure(this@MainActivity))
                }

                if (showDisclosure) {
                    SmsDisclosureDialog(
                        onAccept = {
                            TokenManager.setSeenDisclosure(this@MainActivity, true)
                            showDisclosure = false
                            launchPermissions()
                        },
                        onDecline = {
                            showDisclosure = false
                        }
                    )
                }

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
private fun SmsDisclosureDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* non-dismissible — user must make a choice */ },
        containerColor = DarkSurface,
        title = {
            Text("Before you continue", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                buildAnnotatedString {
                    append("SpamShield needs to read and receive your SMS messages ")
                    append("to classify them as spam or not spam.\n\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White)) {
                        append("What happens to your messages:\n")
                    }
                    append("• Each message is sent over HTTPS to our classification server, processed in memory, and immediately discarded — it is ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("never stored") }
                    append(" on our servers.\n")
                    append("• Classification results (spam/not spam, confidence score, timestamp) are stored anonymously under a random device ID.\n")
                    append("• Message text and sender information are stored ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("only on this device") }
                    append(".\n\n")
                    append("You can revoke SMS access at any time in Android Settings. ")
                    append("See our Privacy Policy for full details.")
                },
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("I understand, continue", color = SpamRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("No thanks", color = TextSecondary)
            }
        }
    )
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
