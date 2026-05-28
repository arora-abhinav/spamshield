package com.example.spamshield.token

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {
    private const val PREF_NAME = "spamshield_secure_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_IS_REGISTERED = "is_registered"
    private const val KEY_OPTED_IN = "opted_in"

    @Volatile private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: buildPrefs(context.applicationContext).also { prefs = it }
        }
    }

    private fun buildPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String, deviceId: String?) {
        getPrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply { if (deviceId != null) putString(KEY_DEVICE_ID, deviceId) }
            .apply()
    }

    fun saveNewTokens(context: Context, accessToken: String, refreshToken: String) {
        getPrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(context: Context): String? =
        getPrefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(context: Context): String? =
        getPrefs(context).getString(KEY_REFRESH_TOKEN, null)

    fun getDeviceId(context: Context): String? =
        getPrefs(context).getString(KEY_DEVICE_ID, null)

    fun isRegistered(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IS_REGISTERED, false)

    fun setRegistered(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_REGISTERED, value).apply()
    }

    fun isOptedIn(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_OPTED_IN, false)

    fun setOptedIn(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_OPTED_IN, value).apply()
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
        prefs = null
    }
}
