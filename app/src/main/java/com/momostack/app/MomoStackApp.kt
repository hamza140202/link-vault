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

    lateinit var userPreferences: com.momostack.app.data.preferences.UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        userPreferences = com.momostack.app.data.preferences.UserPreferences(this)
        database = LinkVaultDatabase.getDatabase(this)
        repository = LinkVaultRepository(database.linkItemDao(), database.categoryDao())

        createNotificationChannel()

        if (userPreferences.isAutoCaptureScreenshotsEnabled() &&
            com.momostack.app.feature.screenshotvault.service.ScreenshotPermissionHelper.hasAllRequiredPermissions(this)) {
            com.momostack.app.feature.screenshotvault.service.ScreenshotWatcherService.start(this)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val captureChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MomoStack Capture & Enrichment",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications confirming captured links and completed enrichment."
            }

            val screenshotServiceChannel = NotificationChannel(
                SCREENSHOT_SERVICE_CHANNEL_ID,
                "Screenshot Auto-Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while automatic screenshot capture is active."
                setShowBadge(false)
            }

            val screenshotResultsChannel = NotificationChannel(
                SCREENSHOT_RESULTS_CHANNEL_ID,
                "Screenshot Text Extractions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when text is ready from captured screenshots."
                enableVibration(true)
            }

            val webArchiveChannel = NotificationChannel(
                WEB_ARCHIVE_CHANNEL_ID,
                "Webpage Offline Archival",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when web pages are saved offline as shareable PDF documents."
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(captureChannel)
            notificationManager?.createNotificationChannel(screenshotServiceChannel)
            notificationManager?.createNotificationChannel(screenshotResultsChannel)
            notificationManager?.createNotificationChannel(webArchiveChannel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "momostack_capture_channel"
        const val SCREENSHOT_SERVICE_CHANNEL_ID = "momostack_screenshot_service_channel"
        const val SCREENSHOT_RESULTS_CHANNEL_ID = "momostack_screenshot_results_channel"
        const val WEB_ARCHIVE_CHANNEL_ID = "momostack_web_archive_channel"

        lateinit var instance: MomoStackApp
            private set
    }
}
