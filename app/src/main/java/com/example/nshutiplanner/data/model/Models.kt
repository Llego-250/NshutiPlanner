package com.example.nshutiplanner.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val partnerId: String = "",
    val coupleId: String = "",
    val photoUrl: String = "",
    val motto: String = "",
    val themeColor: String = "lavender",
    val currentMood: Int = 3
)

data class Plan(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",          // "yyyy-MM-dd"
    val isWeekly: Boolean = false,
    val assignedTo: String = "",    // uid
    val createdBy: String = "",
    val isCompleted: Boolean = false,
    val coupleId: String = "",
    val streakCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)

enum class TaskCategory { HEALTH, CAREER, RELATIONSHIP, FUN }

data class Task(
    val id: String = "",
    val title: String = "",
    val category: String = TaskCategory.RELATIONSHIP.name,
    val isShared: Boolean = false,
    val assignedTo: String = "",
    val createdBy: String = "",
    val isCompleted: Boolean = false,
    val progress: Float = 0f,       // 0.0 – 1.0
    val coupleId: String = "",
    val milestone: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class VisionItem(
    val id: String = "",
    val type: String = "quote",     // "quote" | "image" | "goal"
    val content: String = "",       // text or image URL
    val addedBy: String = "",
    val coupleId: String = "",
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val createdAt: Timestamp = Timestamp.now()
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val emoji: String = "",
    val isNote: Boolean = false,
    val coupleId: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class MoodEntry(
    val id: String = "",
    val userId: String = "",
    val mood: Int = 3,              // 1–5 scale
    val note: String = "",
    val coupleId: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class Badge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "",          // emoji or icon name
    val earnedBy: String = "",
    val coupleId: String = "",
    val earnedAt: Timestamp = Timestamp.now()
)

// Pre-defined badge definitions
object BadgeDefinitions {
    val all = listOf(
        Badge(id = "weekly_hero", title = "Weekly Planner Hero", description = "Completed all plans for a week", icon = "🏆"),
        Badge(id = "streak_7", title = "7-Day Streak", description = "7 days of consistent planning", icon = "🔥"),
        Badge(id = "task_master", title = "Task Master", description = "Completed 10 tasks", icon = "⭐"),
        Badge(id = "vision_builder", title = "Vision Builder", description = "Added 5 items to vision board", icon = "🌟"),
        Badge(id = "care_giver", title = "Care Giver", description = "Sent 10 supportive messages", icon = "💜"),
        Badge(id = "health_champ", title = "Health Champion", description = "Completed 5 health tasks", icon = "💪")
    )
}
