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

        var sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: ""

        var htmlContent: String? = null
        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            val item = clipData.getItemAt(0)
            if (sharedText.isBlank()) {
                sharedText = item.text?.toString() ?: item.coerceToText(this).toString()
            }
            htmlContent = item.htmlText
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
}
