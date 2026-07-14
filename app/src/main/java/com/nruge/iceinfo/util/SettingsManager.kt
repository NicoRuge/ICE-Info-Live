package com.nruge.iceinfo.util

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import android.provider.Settings
import java.util.Locale

object SettingsManager {
    private const val PREFS_NAME = "iceinfo_settings"
    private const val KEY_TARGET_STOP_EVA = "target_stop_eva"
    private const val KEY_IS_MOCK_MODE = "is_mock_mode"
    private const val KEY_DEMO_SPEED = "demo_speed"
    private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version"
    private const val KEY_REDUCED_MOTION = "reduced_motion"
    private const val KEY_APP_THEME = "app_theme"
    private const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
    private const val KEY_CRASH_CONSENT_VERSION = "crash_consent_version_code"
    private const val KEY_LAST_JOURNEY_KEY = "last_journey_key"
    private const val KEY_COACH_NUMBER = "coach_number"
    private const val KEY_SEAT_NUMBER = "seat_number"
    private const val KEY_PRIDE_FLAG_HIDDEN = "pride_flag_hidden"

    fun isCrashReportingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CRASH_REPORTING, false)
    }

    fun setCrashReportingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CRASH_REPORTING, enabled).apply()
    }

    /**
     * versionCode for which the user last answered the crash-reporting consent
     * dialog. -1 means never asked. Used to re-prompt once after each update.
     */
    fun getCrashConsentVersion(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_CRASH_CONSENT_VERSION, -1)
    }

    fun setCrashConsentVersion(context: Context, versionCode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CRASH_CONSENT_VERSION, versionCode).apply()
    }

    fun setAppTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_APP_THEME, theme).apply()
    }

    fun getAppTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_THEME, "SYSTEM") ?: "SYSTEM"
    }

    fun isSystemReducedMotion(context: Context): Boolean {
        val resolver = context.contentResolver
        val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return transition == 0f || animator == 0f
    }

    fun setReducedMotion(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
    }

    fun isReducedMotion(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_REDUCED_MOTION)) {
            prefs.getBoolean(KEY_REDUCED_MOTION, false)
        } else {
            isSystemReducedMotion(context)
        }
    }

    fun getLanguage(context: Context): String {
        val lm = context.getSystemService(LocaleManager::class.java)
        val current = lm.applicationLocales
        val tag = if (!current.isEmpty) current[0].language else Locale.getDefault().language
        return if (tag == "de") "de" else "en"
    }

    fun setLanguage(context: Context, language: String) {
        val lm = context.getSystemService(LocaleManager::class.java)
        lm.applicationLocales = LocaleList.forLanguageTags(language)
    }

    fun setTargetStopEva(context: Context, eva: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TARGET_STOP_EVA, eva).apply()
    }

    fun getTargetStopEva(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TARGET_STOP_EVA, null)
    }

    fun setMockMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_MOCK_MODE, enabled).apply()
    }

    fun isMockMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_MOCK_MODE, false)
    }

    fun setDemoSpeed(context: Context, speed: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DEMO_SPEED, speed).apply()
    }

    fun getDemoSpeed(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DEMO_SPEED, 114)
    }

    fun setOnboardingShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SHOWN, true).apply()
    }

    fun getLastSeenVersion(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_SEEN_VERSION, 0)

    fun setLastSeenVersion(context: Context, versionCode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_LAST_SEEN_VERSION, versionCode).apply()
    }

    fun getLastJourneyKey(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_JOURNEY_KEY, "") ?: ""

    fun setLastJourneyKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_JOURNEY_KEY, key).apply()
    }

    fun setCoachNumber(context: Context, coach: Int?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            if (coach != null) putInt(KEY_COACH_NUMBER, coach) else remove(KEY_COACH_NUMBER)
        }.apply()
    }

    fun getCoachNumber(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_COACH_NUMBER)) prefs.getInt(KEY_COACH_NUMBER, 0) else null
    }

    fun setSeatNumber(context: Context, seat: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SEAT_NUMBER, seat).apply()
    }

    fun getSeatNumber(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SEAT_NUMBER, "") ?: ""
    }

    fun isPrideFlagHidden(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRIDE_FLAG_HIDDEN, false)

    fun setPrideFlagHidden(context: Context, hidden: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PRIDE_FLAG_HIDDEN, hidden).apply()
    }

    fun isOnboardingShown(context: Context): Boolean {
        // Also migrate the old "iceinfo_prefs" key so existing users aren't re-shown onboarding
        val oldPrefs = context.getSharedPreferences("iceinfo_prefs", Context.MODE_PRIVATE)
        if (oldPrefs.contains("onboarding_shown")) {
            val wasShown = oldPrefs.getBoolean("onboarding_shown", false)
            if (wasShown) setOnboardingShown(context)
            oldPrefs.edit().remove("onboarding_shown").apply()
            return wasShown
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SHOWN, false)
    }
}
