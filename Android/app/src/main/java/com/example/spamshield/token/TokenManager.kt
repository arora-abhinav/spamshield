package com.example.spamshield.token

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {

    // Not backed up — device-specific secrets and transient flags
    private const val SECURE_PREF_NAME = "spamshield_secure_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_IS_REGISTERED = "is_registered"
    private const val KEY_HAS_SYNCED_INBOX = "has_synced_inbox"
    private const val KEY_HAS_SEEN_DISCLOSURE = "has_seen_disclosure"

    // Backed up via Android Auto Backup — persists across reinstalls
    private const val BACKUP_PREF_NAME = "spamshield_backup_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_OPTED_IN = "opted_in"
    private const val KEY_PREVIOUS_MSG_CONSENT = "previous_msg_consent"

    @Volatile private var securePrefs: SharedPreferences? = null

    private fun getSecurePrefs(context: Context): SharedPreferences {
        return securePrefs ?: synchronized(this) {
            securePrefs ?: buildSecurePrefs(context.applicationContext).also { securePrefs = it }
        }
    }

    private fun buildSecurePrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getBackupPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(BACKUP_PREF_NAME, Context.MODE_PRIVATE)

    // Tokens (secure, not backed up)

    fun saveTokens(context: Context, accessToken: String, refreshToken: String, deviceId: String?) {
        getSecurePrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
        if (deviceId != null) {
            getBackupPrefs(context).edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
    }

    fun saveNewTokens(context: Context, accessToken: String, refreshToken: String) {
        getSecurePrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(context: Context): String? =
        getSecurePrefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(context: Context): String? =
        getSecurePrefs(context).getString(KEY_REFRESH_TOKEN, null)

    // Registration state (secure, not backed up)

    fun isRegistered(context: Context): Boolean =
        getSecurePrefs(context).getBoolean(KEY_IS_REGISTERED, false)

    fun setRegistered(context: Context, value: Boolean) {
        getSecurePrefs(context).edit().putBoolean(KEY_IS_REGISTERED, value).apply()
    }

    fun hasSyncedInbox(context: Context): Boolean =
        getSecurePrefs(context).getBoolean(KEY_HAS_SYNCED_INBOX, false)

    fun setHasSyncedInbox(context: Context, value: Boolean) {
        getSecurePrefs(context).edit().putBoolean(KEY_HAS_SYNCED_INBOX, value).apply()
    }

    // Disclosure flag (secure, not backed up — resets on reinstall so disclosure shows again)

    fun hasSeenDisclosure(context: Context): Boolean =
        getSecurePrefs(context).getBoolean(KEY_HAS_SEEN_DISCLOSURE, false)

    fun setSeenDisclosure(context: Context, value: Boolean) {
        getSecurePrefs(context).edit().putBoolean(KEY_HAS_SEEN_DISCLOSURE, value).apply()
    }

    // Device ID (backed up — persists across reinstalls)

    fun getDeviceId(context: Context): String? =
        getBackupPrefs(context).getString(KEY_DEVICE_ID, null)

    fun getBackedUpDeviceId(context: Context): String? =
        getBackupPrefs(context).getString(KEY_DEVICE_ID, null)

    // User settings (backed up — preserve preferences across reinstalls)

    fun isOptedIn(context: Context): Boolean =
        getBackupPrefs(context).getBoolean(KEY_OPTED_IN, false)

    fun setOptedIn(context: Context, value: Boolean) {
        getBackupPrefs(context).edit().putBoolean(KEY_OPTED_IN, value).apply()
    }

    fun getPreviousMsgConsent(context: Context): Boolean =
        getBackupPrefs(context).getBoolean(KEY_PREVIOUS_MSG_CONSENT, false)

    fun setPreviousMsgConsent(context: Context, value: Boolean) {
        getBackupPrefs(context).edit().putBoolean(KEY_PREVIOUS_MSG_CONSENT, value).apply()
    }

    fun clearAll(context: Context) {
        getSecurePrefs(context).edit().clear().apply()
        securePrefs = null
        getBackupPrefs(context).edit().clear().apply()
    }
}
