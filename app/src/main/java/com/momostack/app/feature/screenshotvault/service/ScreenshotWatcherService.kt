package com.momostack.app.feature.screenshotvault.service

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
import com.momostack.app.R
import com.momostack.app.feature.screenshotvault.data.ScreenshotEntity
import com.momostack.app.feature.screenshotvault.data.ScreenshotVaultDatabase
import com.momostack.app.ui.MainActivity
import kotlinx.coroutines.*
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
    private var externalObserver: ContentObserver? = null
    private var internalObserver: ContentObserver? = null

    // In-memory cache of recently processed media IDs to prevent duplicate processing
    private val processedIds = Collections.synchronizedSet(HashSet<Long>())

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        registerScreenshotObservers()
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
        externalObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        internalObserver?.let {
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
            .setSmallIcon(R.mipmap.ic_launcher)
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

    private fun registerScreenshotObservers() {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                serviceScope.launch {
                    processScreenshots(uri)
                }
            }
        }

        externalObserver = observer
        internalObserver = observer

        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun processScreenshots(triggerUri: Uri?) {
        // Small delay to allow Android SystemUI to finish writing the image file to storage
        delay(450)

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
        } else {
            projection.add(MediaStore.Images.Media.DATA)
        }

        // Strict: NEVER append LIMIT 1 to sortOrder as Android 10+ MediaProvider throws IllegalArgumentException
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                null,
                null,
                sortOrder
            )?.use { cursor ->
                var inspected = 0
                while (cursor.moveToNext() && inspected < 8) {
                    inspected++

                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val bucketIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                    } else {
                        cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    }
                    val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                    val id = cursor.getLong(idIndex)
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else ""
                    val bucket = if (bucketIndex != -1) cursor.getString(bucketIndex) ?: "" else ""
                    val path = if (pathIndex != -1) cursor.getString(pathIndex) ?: "" else ""
                    val dateAdded = if (dateIndex != -1) cursor.getLong(dateIndex) else 0L

                    // Deduplicate
                    if (processedIds.contains(id)) continue

                    // Check if it matches any Android OEM screenshot pattern
                    val isScreenshot = bucket.contains("Screenshot", ignoreCase = true) ||
                            path.contains("Screenshot", ignoreCase = true) ||
                            name.contains("Screenshot", ignoreCase = true) ||
                            name.startsWith("Screen_", ignoreCase = true) ||
                            name.contains("ScreenCapture", ignoreCase = true) ||
                            path.contains("ScreenCapture", ignoreCase = true)

                    if (!isScreenshot) continue

                    // Check timestamp: within the last 3 minutes
                    val currentTime = System.currentTimeMillis()
                    val dateAddedMs = if (dateAdded > 1000000000000L) dateAdded else dateAdded * 1000L
                    val ageMs = Math.abs(currentTime - dateAddedMs)
                    if (ageMs > 180_000L) continue

                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    // Copy to internal private filesDir/screenshots/
                    val screenshotsDir = File(filesDir, "screenshots").apply { mkdirs() }
                    val destFile = File(screenshotsDir, "screenshot_${id}_${System.currentTimeMillis()}.png")

                    var copySuccess = false
                    try {
                        contentResolver.openInputStream(contentUri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                val copied = input.copyTo(output)
                                if (copied > 0) copySuccess = true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (!copySuccess || !destFile.exists() || destFile.length() == 0L) {
                        // File not ready yet, do not mark as processed so next onChange event can retry
                        continue
                    }

                    // Successfully saved to local storage! Now mark as processed
                    processedIds.add(id)

                    val notificationId = (id % 10000).toInt() + 1000

                    // 1. Post "Screenshot Captured" notification
                    showReadingNotification(notificationId)

                    // Run ML Kit Text Recognition on background thread
                    val extractedText = ScreenshotOcrProcessor.extractTextFromFile(destFile)

                    // Insert into isolated ScreenshotVaultDatabase
                    val database = ScreenshotVaultDatabase.getDatabase(applicationContext)
                    val entity = ScreenshotEntity(
                        localImagePath = destFile.absolutePath,
                        originalUri = contentUri.toString(),
                        extractedText = extractedText
                    )
                    database.screenshotDao().insertScreenshot(entity)

                    // 2. Post "Transcription Completed" notification
                    showResultNotification(notificationId, extractedText)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showReadingNotification(notificationId: Int) {
        val notification = NotificationCompat.Builder(this, MomoStackApp.SCREENSHOT_RESULTS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📸 Screenshot Captured")
            .setContentText("Transcribing text in background...")
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun showResultNotification(notificationId: Int, text: String) {
        val displayText = if (text.isBlank()) {
            "Screenshot saved to Vault (no text detected)"
        } else {
            val preview = if (text.length > 80) text.take(80) + "..." else text
            preview
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✨ Transcription Completed")
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.ifBlank { "Screenshot saved to your Vault." }))
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
