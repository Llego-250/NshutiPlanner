package com.example.nshutiplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.nshutiplanner.ui.screens.tasks.TasksScreen
import com.example.nshutiplanner.ui.screens.visionboard.VisionBoardScreen
import com.example.nshutiplanner.ui.theme.LavenderDark
import com.example.nshutiplanner.ui.theme.NshutiTheme
import com.example.nshutiplanner.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NshutiTheme {
                NshutiApp()
            }
        }
    }
}

@Composable
fun NshutiApp() {
    val appVm: AppViewModel = viewModel()
    val navController = rememberNavController()
    val currentUser by appVm.currentUser.collectAsState()

    val startDestination = if (appVm.isLoggedIn) Route.Dashboard.route else Route.Login.route

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PillNavigationBar(currentRoute, bottomNavItems) { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
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
                DashboardScreen(vm = vm, user = currentUser, repo = appVm.repo)
            }

            composable(Route.Planner.route) {
                val vm: PlannerViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                PlannerScreen(vm = vm, currentUid = appVm.repo.currentUid)
            }

            composable(Route.Tasks.route) {
                val vm: TasksViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                TasksScreen(vm = vm, currentUid = appVm.repo.currentUid)
            }

            composable(Route.VisionBoard.route) {
                val vm: VisionViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                VisionBoardScreen(vm = vm, currentUid = appVm.repo.currentUid)
            }

            composable(Route.Care.route) {
                val vm: CareViewModel = viewModel(factory = VmFactory(appVm.repo))
                LaunchedEffect(appVm.coupleId) { vm.init(appVm.coupleId) }
                NshutiCareScreen(vm = vm, currentUid = appVm.repo.currentUid)
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
    val bgColor = Color(0xFF1A1625)
    val activeColor = LavenderDark
    val inactiveColor = Color(0xFFB0A8CC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(bgColor)
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
                            .background(activeColor)
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
