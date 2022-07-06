package edu.shayo.templateone.permissions

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import edu.shayo.templateone.R
import java.lang.ref.WeakReference
import javax.inject.Inject

class PermissionRequesterImpl @Inject constructor() : PermissionRequester {

    private val requiredPermissions = mutableListOf<Permission>()
    private var rationale: String? = null
    private var callback: (Boolean) -> Unit = {}
    private var detailedCallback: (Map<Permission, Boolean>) -> Unit = {}
    private  var currentFragment: WeakReference<FragmentActivity>? = null

    private val permissionCheck =
        currentFragment?.get()
            ?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
                sendResultAndCleanUp(grantResults)
            }

    override fun from(fragment: FragmentActivity) {
        currentFragment = WeakReference(fragment)
    }

    override fun rationale(description: String): PermissionRequester {
        rationale = description
        return this
    }

    override fun request(vararg permission: Permission): PermissionRequester {
        requiredPermissions.addAll(permission)
        return this
    }

    override fun checkPermission(callback: (Boolean) -> Unit) {
        this.callback = callback
        handlePermissionRequest()
    }

    override fun checkDetailedPermission(callback: (Map<Permission, Boolean>) -> Unit) {
        this.detailedCallback = callback
        handlePermissionRequest()
    }

    private fun handlePermissionRequest() {
        currentFragment?.get()?.let { fragment ->
            when {
                areAllPermissionsGranted(fragment) -> sendPositiveResult()
                shouldShowPermissionRationale(fragment) -> displayRationale(fragment)
                else -> requestPermissions()
            }
        }
    }

    private fun displayRationale(fragment: FragmentActivity) {
        AlertDialog.Builder(fragment)
            .setTitle(fragment.getString(R.string.dialog_permission_title))
            .setMessage(rationale ?: fragment.getString(R.string.dialog_permission_default_message))
            .setCancelable(false)
            .setPositiveButton(fragment.getString(R.string.dialog_permission_button_positive)) { _, _ ->
                requestPermissions()
            }
            .show()
    }

    private fun sendPositiveResult() {
        sendResultAndCleanUp(getPermissionList().associateWith { true })
    }

    private fun sendResultAndCleanUp(grantResults: Map<String, Boolean>) {
        callback(grantResults.all { it.value })
        detailedCallback(grantResults.mapKeys { Permission.from(it.key) })
        cleanUp()
    }

    private fun cleanUp() {
        requiredPermissions.clear()
        rationale = null
        callback = {}
        detailedCallback = {}
    }

    private fun requestPermissions() {
        permissionCheck?.launch(getPermissionList())
    }

    private fun areAllPermissionsGranted(fragment: FragmentActivity) =
        requiredPermissions.all { it.isGranted(fragment) }

    private fun shouldShowPermissionRationale(fragment: FragmentActivity) =
        requiredPermissions.any { it.requiresRationale(fragment) }

    private fun getPermissionList() =
        requiredPermissions.flatMap { it.permissions.toList() }.toTypedArray()

    private fun Permission.isGranted(fragment: FragmentActivity) =
        permissions.all { hasPermission(fragment, it) }

    private fun Permission.requiresRationale(fragment: FragmentActivity) =
        permissions.any { fragment.shouldShowRequestPermissionRationale(it) }

    private fun hasPermission(fragment: FragmentActivity, permission: String) =
        ContextCompat.checkSelfPermission(
            fragment,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}

sealed class Permission(vararg val permissions: String) {
    // Grouped permissions
    object Location : Permission(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)


    companion object {
        fun from(permission: String) = when (permission) {
            ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION -> Location
            else -> throw IllegalArgumentException("Unknown permission: $permission")
        }
    }
}