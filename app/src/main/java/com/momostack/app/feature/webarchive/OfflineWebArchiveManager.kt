package com.momostack.app.feature.webarchive

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.momostack.app.MomoStackApp
import com.momostack.app.R
import com.momostack.app.data.model.LinkItem
import com.momostack.app.data.model.OfflineArchiveStatus
import com.momostack.app.data.repository.LinkVaultRepository
import com.momostack.app.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object OfflineWebArchiveManager {

    suspend fun archiveWebPage(
        context: Context,
        repository: LinkVaultRepository,
        item: LinkItem
    ): Result<File> = withContext(Dispatchers.IO) {
        val notificationId = (item.id.hashCode() and 0x7FFFFFFF) % 10000 + 4000
        val archivesDir = File(context.filesDir, "web_archives").apply { mkdirs() }
        val destFile = File(archivesDir, "webdoc_${item.id}.pdf")

        // 1. Mark as EXTRACTING
        repository.updateOfflineArchive(
            id = item.id,
            status = OfflineArchiveStatus.EXTRACTING,
            pdfPath = null,
            size = 0L
        )

        showProgressNotification(context, notificationId, item.title.ifBlank { item.url })

        // 2. Perform Web-to-PDF conversion
        val result = WebPdfExtractor.convertUrlToPdf(context, item.url, destFile)

        if (result.isSuccess) {
            val file = result.getOrThrow()
            val fileSize = file.length()

            // 3. Update DB with READY status and path
            repository.updateOfflineArchive(
                id = item.id,
                status = OfflineArchiveStatus.READY,
                pdfPath = file.absolutePath,
                size = fileSize
            )

            showCompletedNotification(context, notificationId, item.id, item.title.ifBlank { item.url }, file)
            Result.success(file)
        } else {
            // Mark as FAILED
            repository.updateOfflineArchive(
                id = item.id,
                status = OfflineArchiveStatus.FAILED,
                pdfPath = null,
                size = 0L
            )

            showFailedNotification(context, notificationId, item.title.ifBlank { item.url }, result.exceptionOrNull()?.message)
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error archiving webpage"))
        }
    }

    fun getSharePdfIntent(context: Context, item: LinkItem): Intent? {
        val path = item.offlinePdfPath ?: return null
        val file = File(path)
        if (!file.exists()) return null

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val cleanTitle = item.title.take(60).replace(Regex("[^a-zA-Z0-9.-]"), "_")

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, item.title)
            putExtra(Intent.EXTRA_TEXT, "Here is the offline document for: ${item.title}\n${item.url}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun showProgressNotification(context: Context, notificationId: Int, title: String) {
        val notification = NotificationCompat.Builder(context, MomoStackApp.WEB_ARCHIVE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📄 Saving Webpage Offline...")
            .setContentText(title.take(70))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun showCompletedNotification(
        context: Context,
        notificationId: Int,
        itemId: String,
        title: String,
        file: File
    ) {
        val sizeMb = file.length() / (1024.0 * 1024.0)
        val sizeText = if (sizeMb >= 1.0) String.format("%.1f MB", sizeMb) else "${file.length() / 1024} KB"

        // Action 1: Read Offline inside MomoStack
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "offline_reader/$itemId")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Share PDF
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share Offline PDF")
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            chooserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MomoStackApp.WEB_ARCHIVE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✨ Webpage Saved Offline")
            .setContentText("$title ($sizeText)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title ($sizeText)\nReady to read offline or share as a PDF document."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Read Document", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Share PDF", sharePendingIntent)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, builder.build())
    }

    private fun showFailedNotification(
        context: Context,
        notificationId: Int,
        title: String,
        errorMessage: String?
    ) {
        val builder = NotificationCompat.Builder(context, MomoStackApp.WEB_ARCHIVE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Offline Archival Failed")
            .setContentText(title.take(70))
            .setStyle(NotificationCompat.BigTextStyle().bigText("Could not save offline document for: $title\n${errorMessage ?: "Network or render failure"}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, builder.build())
    }
}
