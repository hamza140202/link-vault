package com.momostack.app.feature.screenshotvault.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.momostack.app.MomoStackApp
import com.momostack.app.feature.screenshotvault.data.ScreenshotEntity
import com.momostack.app.feature.screenshotvault.data.ScreenshotVaultDatabase
import com.momostack.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

/**
 * Background Foreground Service with MediaStore ContentObserver.
 * Detects screenshots taken anywhere on the device, copies them into isolated storage,
 * and runs on-device ML Kit OCR to extract text with zero latency.
 */
class ScreenshotWatcherService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var contentObserver: ContentObserver? = null

    // In-memory cache of recently processed media IDs to prevent duplicate processing
    private val processedIds = Collections.synchronizedSet(HashSet<Long>())

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        registerScreenshotObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "settings")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MomoStackApp.SCREENSHOT_SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Screenshot Vault Active")
            .setContentText("Monitoring for new screenshots to extract text")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_SERVICE_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_SERVICE_ID, notification)
        }
    }

    private fun registerScreenshotObserver() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                serviceScope.launch {
                    processLatestScreenshot()
                }
            }
        }

        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun processLatestScreenshot() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 1"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return

                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathIndex = cursor.getColumnIndex(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
                )
                val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: ""
                val path = if (pathIndex != -1) cursor.getString(pathIndex) ?: "" else ""
                val dateAdded = cursor.getLong(dateIndex)

                // Verify this image was added in the last 60 seconds to avoid reprocessing historical images
                val currentTimeSeconds = System.currentTimeMillis() / 1000
                if (currentTimeSeconds - dateAdded > 60) return

                // Check folder/name pattern to ensure it's a real screenshot
                val isScreenshot = path.contains("Screenshots", ignoreCase = true) ||
                        name.contains("Screenshot", ignoreCase = true)

                if (!isScreenshot) return

                // Deduplicate
                if (!processedIds.add(id)) return

                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // Copy to internal private filesDir/screenshots/
                val screenshotsDir = File(filesDir, "screenshots").apply { mkdirs() }
                val destFile = File(screenshotsDir, "screenshot_${id}_${System.currentTimeMillis()}.png")

                contentResolver.openInputStream(contentUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return

                val notificationId = (id % 10000).toInt() + 1000

                // Post extraction in progress notification
                showReadingNotification(notificationId)

                // Run ML Kit Text Recognition
                val extractedText = ScreenshotOcrProcessor.extractTextFromFile(destFile)

                // Insert into isolated ScreenshotVaultDatabase
                val database = ScreenshotVaultDatabase.getDatabase(applicationContext)
                val entity = ScreenshotEntity(
                    localImagePath = destFile.absolutePath,
                    originalUri = contentUri.toString(),
                    extractedText = extractedText
                )
                database.screenshotDao().insertScreenshot(entity)

                // Post result notification with quick Copy button
                showResultNotification(notificationId, extractedText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showReadingNotification(notificationId: Int) {
        val notification = NotificationCompat.Builder(this, MomoStackApp.SCREENSHOT_RESULTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("MomoStack · Screenshot Captured")
            .setContentText("Reading text from screenshot...")
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun showResultNotification(notificationId: Int, text: String) {
        val displayText = if (text.isBlank()) {
            "Screenshot saved (no text detected)"
        } else {
            val preview = if (text.length > 80) text.take(80) + "..." else text
            "Text Ready: \"$preview\""
        }

        // Action: Copy Text
        val copyIntent = Intent(this, CopyTextReceiver::class.java).apply {
            action = CopyTextReceiver.ACTION_COPY_TEXT
            putExtra(CopyTextReceiver.EXTRA_TEXT, text)
            putExtra(CopyTextReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Open MomoStack Vault
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "settings")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            notificationId + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, MomoStackApp.SCREENSHOT_RESULTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("✨ Screenshot Text Ready")
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.ifBlank { "No readable text found in screenshot." }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        if (text.isNotBlank()) {
            builder.addAction(
                android.R.drawable.ic_menu_send,
                "Copy Text",
                copyPendingIntent
            )
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, builder.build())
    }

    companion object {
        const val NOTIFICATION_SERVICE_ID = 4001
        const val ACTION_STOP_SERVICE = "com.momostack.app.ACTION_STOP_SCREENSHOT_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, ScreenshotWatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenshotWatcherService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }
}
