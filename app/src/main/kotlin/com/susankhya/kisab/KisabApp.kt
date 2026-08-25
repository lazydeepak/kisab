package com.susankhya.kisab

import android.app.Application
import com.susankhya.kisab.notifications.NotificationChannels

class KisabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }
}
