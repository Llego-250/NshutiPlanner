package com.example.nshutiplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nshutiplanner.data.model.*
import com.example.nshutiplanner.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    val repo = FirebaseRepository()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    val isLoggedIn get() = FirebaseAuth.getInstance().currentUser != null

    init {
        if (isLoggedIn) loadUser()
    }

    fun loadUser() = viewModelScope.launch {
        _currentUser.value = repo.getUser(repo.currentUid)
    }

    fun logout() {
        repo.logout()
        _currentUser.value = null
    }

    val coupleId get() = _currentUser.value?.coupleId ?: repo.currentUid
}

// ── Planner ──────────────────────────────────────────────────────────────────

class PlannerViewModel(private val repo: FirebaseRepository) : ViewModel() {
    private val _coupleId = MutableStateFlow("")
    val plans: StateFlow<List<Plan>> = _coupleId
        .filter { it.isNotEmpty() }
        .flatMapLatest { repo.getPlans(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(coupleId: String) { _coupleId.value = coupleId }

    fun savePlan(plan: Plan) = viewModelScope.launch {
        repo.savePlan(plan, _coupleId.value)
        checkPlannerBadge()
    }

    fun togglePlan(planId: String, completed: Boolean) = viewModelScope.launch {
        repo.togglePlan(planId, completed)
    }

    fun deletePlan(planId: String) = viewModelScope.launch { repo.deletePlan(planId) }

    private suspend fun checkPlannerBadge() {
        val completed = plans.value.count { it.isCompleted }
        if (completed >= 7) repo.awardBadge(BadgeDefinitions.all.first { it.id == "weekly_hero" }, _coupleId.value)
    }
}

// ── Tasks ─────────────────────────────────────────────────────────────────────

class TasksViewModel(private val repo: FirebaseRepository) : ViewModel() {
    private val _coupleId = MutableStateFlow("")
    val tasks: StateFlow<List<Task>> = _coupleId
        .filter { it.isNotEmpty() }
        .flatMapLatest { repo.getTasks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(coupleId: String) { _coupleId.value = coupleId }

    fun saveTask(task: Task) = viewModelScope.launch {
        repo.saveTask(task, _coupleId.value)
    }

    fun updateProgress(taskId: String, progress: Float) = viewModelScope.launch {
        repo.updateTaskProgress(taskId, progress, progress >= 1f)
        checkTaskBadge()
    }

    fun deleteTask(taskId: String) = viewModelScope.launch { repo.deleteTask(taskId) }

    private suspend fun checkTaskBadge() {
        val completed = tasks.value.count { it.isCompleted }
        if (completed >= 10) repo.awardBadge(BadgeDefinitions.all.first { it.id == "task_master" }, _coupleId.value)
        val healthDone = tasks.value.count { it.isCompleted && it.category == TaskCategory.HEALTH.name }
        if (healthDone >= 5) repo.awardBadge(BadgeDefinitions.all.first { it.id == "health_champ" }, _coupleId.value)
    }
}

// ── Vision Board ──────────────────────────────────────────────────────────────

class VisionViewModel(private val repo: FirebaseRepository) : ViewModel() {
    private val _coupleId = MutableStateFlow("")
    val items: StateFlow<List<VisionItem>> = _coupleId
        .filter { it.isNotEmpty() }
        .flatMapLatest { repo.getVisionItems(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(coupleId: String) { _coupleId.value = coupleId }

    fun saveItem(item: VisionItem) = viewModelScope.launch {
        repo.saveVisionItem(item, _coupleId.value)
        if (items.value.size >= 5) repo.awardBadge(BadgeDefinitions.all.first { it.id == "vision_builder" }, _coupleId.value)
    }

    fun deleteItem(itemId: String) = viewModelScope.launch { repo.deleteVisionItem(itemId) }

    fun uploadImage(uri: android.net.Uri, onUrl: (String) -> Unit) = viewModelScope.launch {
        val url = repo.uploadVisionImage(uri, _coupleId.value)
        onUrl(url)
    }
}

// ── NshutiCare ────────────────────────────────────────────────────────────────

class CareViewModel(private val repo: FirebaseRepository) : ViewModel() {
    private val _coupleId = MutableStateFlow("")
    val messages: StateFlow<List<Message>> = _coupleId
        .filter { it.isNotEmpty() }
        .flatMapLatest { repo.getMessages(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moods: StateFlow<List<MoodEntry>> = _coupleId
        .filter { it.isNotEmpty() }
        .flatMapLatest { repo.getMoods(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(coupleId: String) { _coupleId.value = coupleId }

    fun sendMessage(text: String, emoji: String = "", isNote: Boolean = false) = viewModelScope.launch {
        repo.sendMessage(Message(senderId = repo.currentUid, text = text, emoji = emoji, isNote = isNote), _coupleId.value)
        checkCareBadge()
    }

    fun saveMood(mood: Int, note: String) = viewModelScope.launch {
        repo.saveMood(MoodEntry(userId = repo.currentUid, mood = mood, note = note), _coupleId.value)
    }

    private suspend fun checkCareBadge() {
        val myMessages = messages.value.count { it.senderId == repo.currentUid }
        if (myMessages >= 10) repo.awardBadge(BadgeDefinitions.all.first { it.id == "care_giver" }, _coupleId.value)
    }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

class DashboardViewModel(private val repo: FirebaseRepository) : ViewModel() {
    private val _coupleId = MutableStateFlow("")

    val plans: StateFlow<List<Plan>> = _coupleId.filter { it.isNotEmpty() }
        .flatMapLatest { repo.getPlans(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = _coupleId.filter { it.isNotEmpty() }
        .flatMapLatest { repo.getTasks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<Badge>> = _coupleId.filter { it.isNotEmpty() }
        .flatMapLatest { repo.getBadges(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(coupleId: String) { _coupleId.value = coupleId }

    val planProgress get() = plans.value.let { list ->
        if (list.isEmpty()) 0f else list.count { it.isCompleted }.toFloat() / list.size
    }

    val taskProgress get() = tasks.value.let { list ->
        if (list.isEmpty()) 0f else list.count { it.isCompleted }.toFloat() / list.size
    }
}
