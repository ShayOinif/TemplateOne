package edu.shayo.templateone.permissions

import androidx.activity.ComponentActivity

interface PermissionRequester {
    fun from(componentActivity: ComponentActivity): PermissionRequester

    fun withPermissions(vararg permissions: Permission): PermissionRequester?

    suspend fun checkPermissions(callback: (Map<Permission,Boolean>) -> Unit)
}