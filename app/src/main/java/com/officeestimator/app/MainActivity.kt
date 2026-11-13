package com.officeestimator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.officeestimator.app.data.Store
import com.officeestimator.app.ui.AppState
import com.officeestimator.app.ui.CostScreen
import com.officeestimator.app.ui.EstimatorTheme
import com.officeestimator.app.ui.FacadeScreen
import com.officeestimator.app.ui.FoundationScreen
import com.officeestimator.app.ui.HomeScreen
import com.officeestimator.app.ui.InsulationScreen
import com.officeestimator.app.ui.LandScreen
import com.officeestimator.app.ui.MasonryScreen
import com.officeestimator.app.ui.MaterialsScreen
import com.officeestimator.app.ui.PricesScreen
import com.officeestimator.app.ui.ProjectDataScreen
import com.officeestimator.app.ui.DashboardScreen
import com.officeestimator.app.ui.NoProjectScreen
import com.officeestimator.app.ui.QuantitiesScreen
import com.officeestimator.app.ui.RatesScreen
import com.officeestimator.app.ui.ReportScreen
import com.officeestimator.app.ui.ScheduleScreen
import com.officeestimator.app.ui.Screen
import com.officeestimator.app.ui.SettingsScreen
import com.officeestimator.app.ui.StructureScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = AppState(Store(applicationContext))
        setContent { App(app) }
    }
}

@Composable
fun App(app: AppState) {
    EstimatorTheme(app.dark) {
        // التطبيق عربي بالكامل: الاتجاه من اليمين إلى اليسار مهما كانت لغة الجهاز
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BackHandler(enabled = app.screen != Screen.HOME) { app.back() }
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = { TopBar(app) },
                bottomBar = { BottomBar(app) }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    val needsProject = app.screen != Screen.HOME && app.screen != Screen.SETTINGS && app.screen != Screen.RATES
                    if (needsProject && !app.hasProject) NoProjectScreen(app) else when (app.screen) {
                        Screen.HOME -> HomeScreen(app)
                        Screen.DASHBOARD -> DashboardScreen(app)
                        Screen.QUANTITIES -> QuantitiesScreen(app)
                        Screen.REPORT -> ReportScreen(app)
                        Screen.PRICES -> PricesScreen(app)
                        Screen.SETTINGS -> SettingsScreen(app)
                        Screen.PROJECT_DATA -> ProjectDataScreen(app)
                        Screen.COST -> CostScreen(app)
                        Screen.LAND -> LandScreen(app)
                        Screen.STRUCTURE -> StructureScreen(app)
                        Screen.FOUNDATION -> FoundationScreen(app)
                        Screen.MASONRY -> MasonryScreen(app)
                        Screen.INSULATION -> InsulationScreen(app)
                        Screen.SCHEDULE -> ScheduleScreen(app)
                        Screen.FACADE -> FacadeScreen(app)
                        Screen.MATERIALS -> MaterialsScreen(app)
                        Screen.RATES -> RatesScreen(app)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(app: AppState) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!app.screen.isTab || app.wizard) {
                IconButton(onClick = { app.back() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                val title = if (app.screen == Screen.HOME)
                    app.office.officeName.ifBlank { "تقدير البناء" } else app.screen.title
                Text(
                    title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = if (app.screen == Screen.HOME) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (app.screen == Screen.HOME || !app.hasProject) app.office.engineer else app.project.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = { app.toggleDark() }) {
                Text(if (app.dark) "وضع نهاري" else "وضع ليلي")
            }
        }
    }
}

@Composable
private fun BottomBar(app: AppState) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val tabs = listOf(
            Screen.HOME to Icons.Filled.Home,
            Screen.DASHBOARD to Icons.Filled.Menu,
            Screen.REPORT to Icons.Filled.Info,
            Screen.PRICES to Icons.Filled.ShoppingCart,
            Screen.SETTINGS to Icons.Filled.Settings
        )
        tabs.forEach { (screen, icon) ->
            NavigationBarItem(
                selected = if (app.wizard) screen == Screen.HOME
                else app.screen == screen || (screen == Screen.DASHBOARD && !app.screen.isTab),
                onClick = { app.goTab(screen) },
                icon = { Icon(icon, contentDescription = screen.title) },
                label = { Text(screen.title) }
            )
        }
    }
}
