package com.example.nshutiplanner.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val route: String) {
    object Login : Route("login")
    object Register : Route("register")
    object Dashboard : Route("dashboard")
    object Planner : Route("planner")
    object Tasks : Route("tasks")
    object VisionBoard : Route("vision_board")
    object Care : Route("care")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Route.Dashboard.route, "Home", Icons.Rounded.Home),
    BottomNavItem(Route.Planner.route, "Planner", Icons.Rounded.CalendarMonth),
    BottomNavItem(Route.Tasks.route, "Tasks", Icons.Rounded.TaskAlt),
    BottomNavItem(Route.VisionBoard.route, "Vision", Icons.Rounded.AutoAwesome),
    BottomNavItem(Route.Care.route, "Care", Icons.Rounded.Favorite)
)
