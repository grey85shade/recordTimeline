package com.bcaste.lifetimeline.data

import android.content.Context
import com.bcaste.lifetimeline.data.local.entity.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    private val _activeProfileId = MutableStateFlow(prefs.getString("active_profile_id", Profile.MAIN_ID) ?: Profile.MAIN_ID)
    val activeProfileId = _activeProfileId.asStateFlow()

    fun setActiveProfile(id: String) {
        _activeProfileId.value = id
        prefs.edit().putString("active_profile_id", id).apply()
    }
}
