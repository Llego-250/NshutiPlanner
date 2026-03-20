package com.example.nshutiplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nshutiplanner.data.repository.FirebaseRepository

@Suppress("UNCHECKED_CAST")
class VmFactory(private val repo: FirebaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(PlannerViewModel::class.java) -> PlannerViewModel(repo) as T
        modelClass.isAssignableFrom(TasksViewModel::class.java) -> TasksViewModel(repo) as T
        modelClass.isAssignableFrom(VisionViewModel::class.java) -> VisionViewModel(repo) as T
        modelClass.isAssignableFrom(CareViewModel::class.java) -> CareViewModel(repo) as T
        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repo) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
