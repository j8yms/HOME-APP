package com.example.householdapp.core.health

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.Permission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HealthConnectHelper {
    private const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

    private fun isSdkSufficient(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean =
        try {
            @Suppress("Deprecation")
            packageManager.getApplicationInfo(packageName, 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    fun isAvailable(context: Context): Boolean {
        if (!isSdkSufficient()) return false
        return isPackageInstalled(context.packageManager, PROVIDER_PACKAGE_NAME)
    }

    fun client(context: Context): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    val readPermissions = setOf(
        Permission.createReadPermission(StepsRecord::class),
        Permission.createReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun hasPermissions(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val granted = client(context).permissionController.getGrantedPermissions(readPermissions)
            granted.containsAll(readPermissions)
        } catch (e: Exception) {
            false
        }
    }
}
