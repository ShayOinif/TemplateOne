package edu.shayo.templateone.utils

import android.content.Context
import androidx.core.content.edit

internal object SharedPreferenceUtil {

    const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"
    const val PREFERENCE_FILE_KEY = "com.example.android.while_in_use_location.PREFERENCE_FILE_KEY"

    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(
            PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
            .getBoolean(KEY_FOREGROUND_ENABLED, false)

    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) =
        context.getSharedPreferences(
            PREFERENCE_FILE_KEY,
            Context.MODE_PRIVATE).edit {
            putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
        }
}