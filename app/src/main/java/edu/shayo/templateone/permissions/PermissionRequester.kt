package edu.shayo.templateone.permissions

import androidx.fragment.app.FragmentActivity

interface PermissionRequester {
    fun from(fragment: FragmentActivity)

    fun rationale(description: String): PermissionRequester

    fun request(vararg permission: Permission): PermissionRequester

    fun checkPermission(callback: (Boolean) -> Unit)

    fun checkDetailedPermission(callback: (Map<Permission,Boolean>) -> Unit)
}