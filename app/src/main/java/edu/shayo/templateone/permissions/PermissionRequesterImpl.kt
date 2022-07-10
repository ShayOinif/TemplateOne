package edu.shayo.templateone.permissions

import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import edu.shayo.templateone.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject

class PermissionRequesterImpl @Inject constructor() : PermissionRequester {

    private var requiredPermissions: MutableMap<Permission, PermissionState>? = null
    private var callback: WeakReference<(Map<Permission, Boolean>) -> Unit>? = null
    private var currentComponentActivity: WeakReference<ComponentActivity>? = null
    private var permissionRequestLauncher: ActivityResultLauncher<Array<String>>? = null

    override fun from(componentActivity: ComponentActivity): PermissionRequester {
        currentComponentActivity = WeakReference(componentActivity)

        permissionRequestLauncher = currentComponentActivity?.get()
            ?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->

                grantResults.forEach { (permissionName, isGranted) ->

                    if (isGranted) {
                        Permission.systemToPermission(permissionName)?.let {
                            requiredPermissions?.put(it, PermissionState.GRANTED)
                        }
                    }
                }

                sendResultAndCleanUp()
            }

        return this
    }

    override fun withPermissions(vararg permissions: Permission) =

        requiredPermissions?.let {
            null
        } ?: run {
            requiredPermissions = mutableMapOf()

            permissions.forEach {
                requiredPermissions?.put(it, PermissionState.NOT_GRANTED)
            }

            return@run this
        }

    override suspend fun checkPermissions(callback: (Map<Permission, Boolean>) -> Unit) {
        this.callback = WeakReference(callback)

        handlePermissionRequest()
    }

    private suspend fun handlePermissionRequest() {
        currentComponentActivity?.get()?.let { componentActivity ->

            requiredPermissions?.let { permissions ->
                permissions.forEach { (permission, _) ->
                    if (componentActivity.hasPermission(permission))

                        permissions[permission] = PermissionState.GRANTED
                }

                permissions.filter { it.value == PermissionState.NOT_GRANTED }
                    .forEach { (permission, _) ->
                        if (componentActivity.shouldShowRequestPermissionRationale(permission.name)) {
                            componentActivity.showRationale(permission)
                            Log.d("Shay", "Done waiting")
                        }
                        else {
                            permissions[permission] = PermissionState.SHOULD_ASK

                            Log.d("Shay", "Done waiting")
                        }
                    }

                permissionRequestLauncher?.launch(
                    permissions.filter { it.value == PermissionState.SHOULD_ASK }
                        .map { permissionEntry ->
                            permissionEntry.key.name
                        }.toTypedArray()
                )
            }
        }
    }

    private fun sendResultAndCleanUp() {

        val result = requiredPermissions?.mapValues { permissionEntry ->
            permissionEntry.value == PermissionState.GRANTED
        } ?: mapOf()

        callback?.get()?.invoke(result)

        cleanUp()
    }

    private fun cleanUp() {
        callback = null
        requiredPermissions = null
    }

    private suspend fun ComponentActivity.showRationale(permission: Permission) {

        val answer = MutableSharedFlow<PermissionState>(1)
        val context = Job() + Dispatchers.IO

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_permission_title))
            .setMessage(getString(permission.permissionRationaleResId))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.dialog_permission_button_positive)) { _, _ ->
                    answer.tryEmit(PermissionState.SHOULD_ASK)
            }
            .setNegativeButton(getString(R.string.dialog_permission_button_negative)) { _, _ ->
                answer.tryEmit(PermissionState.NOT_GRANTED)
            }
            .show()

        withContext(context) {
            answer.take(1).collectLatest {
                requiredPermissions?.put(permission, it)
            }
        }
    }
}

private fun ComponentActivity.hasPermission(permission: Permission): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission.name
    ) == PackageManager.PERMISSION_GRANTED
}


private enum class PermissionState {
    GRANTED,
    NOT_GRANTED,
    SHOULD_ASK
}