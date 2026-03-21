package com.example.nshutiplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.nshutiplanner.ui.navigation.*
import com.example.nshutiplanner.ui.screens.auth.*
import com.example.nshutiplanner.ui.screens.care.NshutiCareScreen
import com.example.nshutiplanner.ui.screens.dashboard.DashboardScreen
import com.example.nshutiplanner.ui.screens.planner.PlannerScreen
import com.example.nshutiplanner.ui.screens.profile.ProfileScreen
import com.example.nshutiplanner.ui.screens.tasks.TasksScreen
import com.example.nshutiplanner.ui.screens.visionboard.VisionBoardScreen
import com.example.nshutiplanner.ui.theme.LavenderDark
import com.example.nshutiplanner.ui.theme.NshutiTheme
import com.example.nshutiplanner.ui.theme.SurfaceDark
import com.example.nshutiplanner.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            NshutiTheme(darkTheme = darkTheme) {
                NshutiApp(darkTheme = darkTheme, onToggleTheme = { darkTheme = !darkTheme })
            }
        }
    }
}

@Composable
fun NshutiApp(darkTheme: Boolean = false, onToggleTheme: () -> Unit = {}) {
    val appVm: AppViewModel = viewModel()
    val navController = rememberNavController()
    val currentUser by appVm.currentUser.collectAsState()

    val startDestination = if (appVm.isLoggedIn) Route.Dashboard.route else Route.Login.route

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Box {
                        PillNavigationBar(currentRoute, bottomNavItems) { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        if (currentRoute == Route.Dashboard.route) {
                            FloatingActionButton(
                                onClick = { navController.navigate(Route.Care.route) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-24).dp, y = (-72).dp)
                                    .size(52.dp),
                                containerColor = LavenderDark,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Favorite, "Care", modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Login.route) {
                LoginScreen(
                    repo = appVm.repo,
                    onLogin = {
                        appVm.loadUser()
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Login.route) { inclusive = true }
                        }
                    },
                    onGoRegister = { navController.navigate(Route.Register.route) }
                )
            }

            composable(Route.Register.route) {
                RegisterScreen(
                    repo = appVm.repo,
                    onRegister = {
                        appVm.loadUser()
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Register.route) { inclusive = true }
                        }
                    },
                    onGoLogin = { navController.popBackStack() }
                )
            }

            composable(Route.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                DashboardScreen(vm = vm, user = currentUser, repo = appVm.repo, onCareClick = {
                    navController.navigate(Route.Care.route)
                })
            }

            composable(Route.Planner.route) {
                val vm: PlannerViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                PlannerScreen(vm = vm, currentUid = appVm.repo.currentUid, onVisionClick = {
                    navController.navigate(Route.VisionBoard.route)
                })
            }

            composable(Route.Tasks.route) {
                val vm: TasksViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                TasksScreen(vm = vm, currentUid = appVm.repo.currentUid)
            }

            composable(Route.VisionBoard.route) {
                val vm: VisionViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                VisionBoardScreen(vm = vm, currentUid = appVm.repo.currentUid, onBack = { navController.popBackStack() })
            }

            composable(Route.Profile.route) {
                ProfileScreen(
                    user = currentUser,
                    repo = appVm.repo,
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    onLogout = {
                        appVm.logout()
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onCareClick = { navController.navigate(Route.Care.route) }
                )
            }

            composable(Route.Care.route) {
                val vm: CareViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                NshutiCareScreen(vm = vm, currentUid = appVm.repo.currentUid, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun PillNavigationBar(
    currentRoute: String?,
    items: List<BottomNavItem>,
    onNavigate: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface == SurfaceDark
    val glassBase = if (isDark) Color(0xFF1A1625) else Color(0xFFFFFFFF)
    val glassBorder = if (isDark) Color(0x40B0A8CC) else Color(0x60FFFFFF)
    val glassHighlight = if (isDark) Color(0x15FFFFFF) else Color(0x80FFFFFF)
    val activeColor = LavenderDark
    val inactiveColor = if (isDark) Color(0xFFB0A8CC) else Color(0xFF6B6480)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Blur glow behind
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(24.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(glassBase.copy(alpha = 0.5f))
        )
        // Glass pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            glassHighlight,
                            glassBase.copy(alpha = if (isDark) 0.88f else 0.72f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(glassBorder, Color.Transparent)),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                if (selected) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                Brush.linearGradient(listOf(activeColor, activeColor.copy(alpha = 0.8f)))
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                            .clickable { onNavigate(item.route) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(item.icon, item.label, tint = Color.White, modifier = Modifier.size(22.dp))
                        Text(item.label, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { onNavigate(item.route) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, item.label, tint = inactiveColor, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
