package com.linkvault.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.linkvault.app.LinkVaultApp
import com.linkvault.app.classifier.AutoClassifier
import com.linkvault.app.data.model.LinkItem
import com.linkvault.app.data.model.ProcessingStatus
import com.linkvault.app.util.UrlExtractor
import com.linkvault.app.worker.EnrichmentScheduler
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

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: ""

        if (sharedText.isBlank()) {
            Toast.makeText(this, "No content found to save", Toast.LENGTH_SHORT).show()
            finish()
            return
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
                notes = extracted.accompanyingText,
                category = initialCategory,
                status = ProcessingStatus.PENDING
            )
        } else {
            LinkItem(
                id = itemId,
                url = "",
                title = sharedText.lines().firstOrNull()?.take(50) ?: "Shared Note",
                notes = sharedText,
                category = "Work",
                status = ProcessingStatus.COMPLETED
            )
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                LinkVaultApp.instance.repository.saveItem(itemToSave)
            }

            if (extracted.url != null) {
                EnrichmentScheduler.scheduleEnrichment(applicationContext, itemId)
            }

            Toast.makeText(
                applicationContext,
                if (extracted.url != null) "🥟 Saved to MomoStack" else "🥟 Note captured in MomoStack",
                Toast.LENGTH_SHORT
            ).show()

            finishAndRemoveTask()
        }
    }
}
