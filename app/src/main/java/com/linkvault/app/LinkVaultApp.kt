package com.linkvault.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.linkvault.app.data.database.LinkVaultDatabase
import com.linkvault.app.data.repository.LinkVaultRepository

class LinkVaultApp : Application() {

    lateinit var database: LinkVaultDatabase
        private set

    lateinit var repository: LinkVaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = LinkVaultDatabase.getDatabase(this)
        repository = LinkVaultRepository(database.linkItemDao(), database.categoryDao())

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Link Capture & Enrichment",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications confirming captured links and completed enrichment."
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "linkvault_capture_channel"

        lateinit var instance: LinkVaultApp
            private set
    }
}
