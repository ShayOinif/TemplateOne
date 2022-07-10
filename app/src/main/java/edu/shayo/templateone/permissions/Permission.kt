package edu.shayo.templateone.permissions

import android.Manifest
import androidx.annotation.StringRes
import edu.shayo.templateone.R

sealed class Permission private constructor(
    val name: String,
    @StringRes
    val permissionDisplayResId: Int,
    @StringRes
    val permissionRationaleResId: Int,
) {

    object FineLocation : Permission(
        Manifest.permission.ACCESS_FINE_LOCATION,
        R.string.fine_location_permission,
        R.string.fine_location_rationale,
    )

    object BackgroundLocation : Permission(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        R.string.background_location_permission,
        R.string.background_location_rationale,
    )

    object ForegroundService : Permission(
        Manifest.permission.FOREGROUND_SERVICE,
        R.string.foreground_service_permission,
        R.string.background_location_rationale,
    )

    companion object {
        fun systemToPermission(systemName: String) =
            when (systemName) {
                Manifest.permission.FOREGROUND_SERVICE -> ForegroundService
                Manifest.permission.ACCESS_BACKGROUND_LOCATION -> BackgroundLocation
                Manifest.permission.ACCESS_FINE_LOCATION -> FineLocation
                else -> null
            }
    }
}