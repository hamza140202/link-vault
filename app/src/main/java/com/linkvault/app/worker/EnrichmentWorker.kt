package com.linkvault.app.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.linkvault.app.LinkVaultApp
import com.linkvault.app.classifier.AutoClassifier
import com.linkvault.app.data.model.ProcessingStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.util.concurrent.TimeUnit

class EnrichmentWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val dao = LinkVaultApp.instance.database.linkItemDao()
        val item = dao.getById(itemId) ?: return Result.failure()

        if (item.url.isBlank()) {
            return Result.success()
        }

        try {
            val request = Request.Builder()
                .url(item.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36 MomoStack/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                dao.update(item.copy(status = ProcessingStatus.FAILED_METADATA.name, updatedAt = System.currentTimeMillis()))
                return Result.success()
            }

            // Cap reading to 2MB to prevent memory exhaustion
            val responseBody = response.body
            val responseString = responseBody?.source()?.let { source ->
                source.request(2 * 1024 * 1024)
                source.buffer.clone().readUtf8()
            } ?: ""

            val doc = Jsoup.parse(responseString, item.url)

            val extractedTitle = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.title().takeIf { it.isNotBlank() }
                ?: item.title

            val extractedDescription = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() }
                ?: item.description

            val extractedPreviewImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }

            val extractedFavicon = doc.select("link[rel~=(?i)^(shortcut|icon|apple-touch-icon)]").attr("abs:href").takeIf { it.isNotBlank() }
                ?: fallbackFavicon(item.url)

            val derivedCategory = if (item.category == "Uncategorized") {
                AutoClassifier.classify(item.domain, extractedTitle, extractedDescription)
            } else {
                item.category
            }

            val updated = item.copy(
                title = extractedTitle,
                description = extractedDescription,
                previewImageUrl = extractedPreviewImage,
                faviconUrl = extractedFavicon,
                category = derivedCategory,
                status = ProcessingStatus.COMPLETED.name,
                updatedAt = System.currentTimeMillis()
            )

            dao.update(updated)

            showEnrichmentNotification(updated.title, updated.category)

            return Result.success()
        } catch (e: Exception) {
            dao.update(item.copy(status = ProcessingStatus.FAILED_METADATA.name, updatedAt = System.currentTimeMillis()))
            return Result.success()
        }
    }

    private fun fallbackFavicon(url: String): String? {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return null
            "https://$host/favicon.ico"
        } catch (_: Exception) {
            null
        }
    }

    private fun showEnrichmentNotification(title: String, category: String) {
        try {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notification = NotificationCompat.Builder(applicationContext, LinkVaultApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Enriched: $title")
                .setContentText("Categorized under $category")
                .setAutoCancel(true)
                .build()
            manager?.notify(title.hashCode(), notification)
        } catch (_: Exception) {
            // Notifications may be restricted by runtime permissions
        }
    }

    companion object {
        const val KEY_ITEM_ID = "enrichment_item_id"
    }
}
