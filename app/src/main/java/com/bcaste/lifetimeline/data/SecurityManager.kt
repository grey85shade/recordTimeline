package com.bcaste.lifetimeline.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PASSWORD = "app_password"
    }

    fun setPassword(password: String?) {
        sharedPreferences.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun getPassword(): String? {
        return sharedPreferences.getString(KEY_PASSWORD, null)
    }

    fun isPasswordSet(): Boolean {
        return !getPassword().isNullOrBlank()
    }

    fun checkPassword(input: String): Boolean {
        return getPassword() == input
    }
}
