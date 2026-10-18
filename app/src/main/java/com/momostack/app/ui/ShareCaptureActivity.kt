package com.momostack.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.momostack.app.LinkVaultApp
import com.momostack.app.classifier.AutoClassifier
import com.momostack.app.data.model.LinkItem
import com.momostack.app.data.model.ProcessingStatus
import com.momostack.app.util.UrlExtractor
import com.momostack.app.worker.EnrichmentScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ShareCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        handleIncomingShareIntent(intent)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) {
            finish()
            return
        }

        // 1. First extract any shared text or URL
        var sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: ""

        var htmlContent: String? = null
        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            val item = clipData.getItemAt(0)
            if (sharedText.isBlank()) {
                sharedText = item.text?.toString() ?: ""
            }
            htmlContent = item.htmlText
        }

        // Check if shared text contains a URL (e.g. Chrome or browser link share)
        val hasUrl = sharedText.isNotBlank() && UrlExtractor.extract(sharedText).url != null

        // 2. Check for image share only when not a URL share
        val imageUri: android.net.Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        val isImageShare = !hasUrl && (
            intent.type?.startsWith("image/") == true ||
            (imageUri != null && contentResolver.getType(imageUri)?.startsWith("image/") == true)
        )

        if (isImageShare && imageUri != null) {
            handleImageShare(imageUri)
            return
        }

        if (sharedText.isBlank()) {
            Toast.makeText(this, "No content found to save", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Smart format if HTML is present
        val formattedBody = if (!htmlContent.isNullOrBlank() && com.momostack.app.util.RichTextFormatter.containsHtml(htmlContent)) {
            com.momostack.app.util.RichTextFormatter.htmlToMarkdown(htmlContent)
        } else if (com.momostack.app.util.RichTextFormatter.containsHtml(sharedText)) {
            com.momostack.app.util.RichTextFormatter.htmlToMarkdown(sharedText)
        } else {
            sharedText
        }

        val extracted = UrlExtractor.extract(sharedText)
        val itemId = UUID.randomUUID().toString()

        val itemToSave = if (extracted.url != null) {
            val initialCategory = AutoClassifier.classify(extracted.domain, null, null)
            LinkItem(
                id = itemId,
                url = extracted.url,
                normalizedUrl = extracted.normalizedUrl,
                title = extracted.domain ?: extracted.url,
                domain = extracted.domain,
                notes = extracted.accompanyingText ?: if (formattedBody != sharedText) formattedBody else null,
                category = initialCategory,
                status = ProcessingStatus.PENDING
            )
        } else {
            // Smart note recognition from clipboard/text share
            val cleanTitle = formattedBody.lines().firstOrNull { it.isNotBlank() }
                ?.replace(Regex("^#+\\s*"), "")
                ?.take(50)
                ?: "Clipboard Note"

            LinkItem(
                id = itemId,
                url = "",
                title = cleanTitle,
                notes = formattedBody,
                category = "Work",
                status = ProcessingStatus.COMPLETED
            )
        }

        lifecycleScope.launch {
            val savedItem = withContext(Dispatchers.IO) {
                LinkVaultApp.instance.repository.saveOrMergeItem(itemToSave)
            }

            if (savedItem.url.isNotBlank()) {
                EnrichmentScheduler.scheduleEnrichment(applicationContext, savedItem.id)
            }

            Toast.makeText(
                applicationContext,
                if (savedItem.url.isNotBlank()) "🥟 Saved to MomoStack" else "📝 Note saved to MomoStack",
                Toast.LENGTH_SHORT
            ).show()

            finishAndRemoveTask()
        }
    }

    private fun handleImageShare(imageUri: android.net.Uri) {
        lifecycleScope.launch {
            Toast.makeText(applicationContext, "📸 Saving to Screenshot Vault...", Toast.LENGTH_SHORT).show()
            withContext(Dispatchers.IO) {
                try {
                    val db = com.momostack.app.feature.screenshotvault.data.ScreenshotVaultDatabase.getDatabase(applicationContext)
                    val repo = com.momostack.app.feature.screenshotvault.data.ScreenshotVaultRepository(applicationContext, db.screenshotDao())
                    val saved = repo.importScreenshotFromUri(imageUri)

                    val notificationId = (saved.id % 10000).toInt() + 1000
                    showScreenshotCompletionNotification(notificationId, saved.extractedText)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Toast.makeText(applicationContext, "✨ Screenshot & text saved to Vault!", Toast.LENGTH_SHORT).show()
            finishAndRemoveTask()
        }
    }

    private fun showScreenshotCompletionNotification(notificationId: Int, text: String) {
        val copyIntent = Intent(this, com.momostack.app.feature.screenshotvault.service.CopyTextReceiver::class.java).apply {
            action = com.momostack.app.feature.screenshotvault.service.CopyTextReceiver.ACTION_COPY_TEXT
            putExtra(com.momostack.app.feature.screenshotvault.service.CopyTextReceiver.EXTRA_TEXT, text)
            putExtra(com.momostack.app.feature.screenshotvault.service.CopyTextReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val copyPendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            notificationId,
            copyIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "settings")
        }
        val openPendingIntent = android.app.PendingIntent.getActivity(
            this,
            notificationId + 1,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(this, com.momostack.app.MomoStackApp.SCREENSHOT_RESULTS_CHANNEL_ID)
            .setSmallIcon(com.momostack.app.R.mipmap.ic_launcher)
            .setContentTitle("✨ Transcription Completed")
            .setContentText(if (text.isBlank()) "Screenshot saved to your Vault." else text.take(80))
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(text.ifBlank { "Screenshot saved to your Vault." }))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        if (text.isNotBlank()) {
            builder.addAction(
                android.R.drawable.ic_menu_send,
                "Copy Text",
                copyPendingIntent
            )
        }

        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager?.notify(notificationId, builder.build())
    }
}
