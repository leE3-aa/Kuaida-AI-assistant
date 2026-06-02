package com.kuaida.app.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kuaida.core.model.InterviewSession
import com.kuaida.feature.history.HistoryRoute
import com.kuaida.feature.knowledge.KnowledgeRoute
import com.kuaida.feature.settings.SettingsRoute
import com.kuaida.feature.templates.TemplateRoute
import com.kuaida.feature.workbench.WorkbenchRoute

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun QuickAnswerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var templatePrefill by remember { mutableStateOf<String?>(null) }
    var historyPrefill by remember { mutableStateOf<InterviewSession?>(null) }

    val bottomItems = listOf(
        BottomDestination(WorkbenchRoute.route, "工作台", { Icon(Icons.Outlined.Build, null) }),
        BottomDestination(KnowledgeRoute.route, "知识库", { Icon(Icons.Outlined.FolderCopy, null) }),
        BottomDestination(HistoryRoute.route, "历史", { Icon(Icons.Outlined.History, null) }),
        BottomDestination(SettingsRoute.route, "设置", { Icon(Icons.Outlined.Settings, null) }),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentDestination?.route != TemplateRoute.route) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                    ) {
                        bottomItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.18f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                                label = "navIconScale",
                            )
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Box(modifier = Modifier.scale(iconScale)) {
                                        item.icon()
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = WorkbenchRoute.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(WorkbenchRoute.route) {
                WorkbenchRoute(
                    contentPadding = PaddingValues(),
                    templatePrefill = templatePrefill,
                    onTemplatePrefillConsumed = { templatePrefill = null },
                    historyPrefill = historyPrefill,
                    onHistoryPrefillConsumed = { historyPrefill = null },
                    onOpenTemplates = { navController.navigate(TemplateRoute.route) },
                )
            }
            composable(KnowledgeRoute.route) {
                KnowledgeRoute(contentPadding = PaddingValues())
            }
            composable(HistoryRoute.route) {
                HistoryRoute(
                    contentPadding = PaddingValues(),
                    onAskAgain = { session ->
                        templatePrefill = null
                        historyPrefill = session
                        navController.navigate(WorkbenchRoute.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(SettingsRoute.route) {
                SettingsRoute(contentPadding = PaddingValues())
            }
            composable(TemplateRoute.route) {
                TemplateRoute(
                    contentPadding = PaddingValues(),
                    onBack = { navController.popBackStack() },
                    onUseTemplate = { prompt ->
                        historyPrefill = null
                        templatePrefill = prompt
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
