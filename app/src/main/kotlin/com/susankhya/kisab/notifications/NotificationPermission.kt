package com.susankhya.kisab.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationPermission {
    val PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

    fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isGranted(context: Context): Boolean {
        if (!requiresRuntimePermission()) return true
        return ContextCompat.checkSelfPermission(context, PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
    }
}

enum class NotificationPermissionUiState {
    /** OS allows notifications (API &lt; 33 always, or granted on 33+). */
    ON,
    /** API 33+ and not granted. */
    OFF
}

object NotificationPermissionPresentation {
    fun uiState(granted: Boolean, requiresRuntime: Boolean = NotificationPermission.requiresRuntimePermission()): NotificationPermissionUiState {
        if (!requiresRuntime) return NotificationPermissionUiState.ON
        return if (granted) NotificationPermissionUiState.ON else NotificationPermissionUiState.OFF
    }
}
