package com.momostack.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.momostack.app.data.database.LinkVaultDatabase
import com.momostack.app.data.repository.LinkVaultRepository

typealias LinkVaultApp = MomoStackApp

class MomoStackApp : Application() {

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
                "MomoStack Capture & Enrichment",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications confirming captured links and completed enrichment."
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "momostack_capture_channel"

        lateinit var instance: MomoStackApp
            private set
    }
}
