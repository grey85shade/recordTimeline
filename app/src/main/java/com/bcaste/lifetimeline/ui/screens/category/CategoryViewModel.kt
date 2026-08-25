package com.bcaste.lifetimeline.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcaste.lifetimeline.data.ProfileManager
import com.bcaste.lifetimeline.data.TimelineRepository
import com.bcaste.lifetimeline.data.local.entity.Category
import com.bcaste.lifetimeline.data.local.entity.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: TimelineRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    val categories: StateFlow<List<Category>> = profileManager.activeProfileId
        .flatMapLatest { profileId ->
            if (profileId == Profile.MAIN_ID) repository.getAllCategories()
            else repository.getCategoriesByProfile(profileId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String, color: String = "#000000", icon: String = "label") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    id = UUID.randomUUID().toString(),
                    profileId = profileManager.activeProfileId.value,
                    name = name,
                    color = color,
                    icon = icon
                )
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
