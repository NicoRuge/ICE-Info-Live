package com.nruge.iceinfo.util

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "iceinfo_settings"
    private const val KEY_TARGET_STOP_EVA = "target_stop_eva"
    private const val KEY_IS_MOCK_MODE = "is_mock_mode"
    private const val KEY_DEMO_SPEED = "demo_speed"

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
}
