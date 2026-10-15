package com.momostack.app.backup

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.momostack.app.MomoStackApp
import com.momostack.app.R
import com.momostack.app.data.dao.CategoryDao
import com.momostack.app.data.dao.LinkItemDao
import com.momostack.app.data.entity.LinkItemEntity
import com.momostack.app.feature.screenshotvault.data.ScreenshotEntity
import com.momostack.app.feature.screenshotvault.data.ScreenshotVaultDatabase
import com.momostack.app.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    const val CURRENT_BACKUP_VERSION = 2
    const val BACKUP_FILE_NAME = "momostackbackup.json"
    private const val NOTIFICATION_ID_BACKUP = 8801
    private const val NOTIFICATION_ID_RESTORE = 8802

    suspend fun createBackupPayload(
        context: Context,
        linkItemDao: LinkItemDao,
        categoryDao: CategoryDao
    ): BackupPayload = withContext(Dispatchers.IO) {
        val items = linkItemDao.getAllList().map { it.toDomain() }
        val categories = categoryDao.getAllCategoriesList()

        val screenshotsList = try {
            val sDao = ScreenshotVaultDatabase.getInstance(context).screenshotDao()
            sDao.getAllScreenshotsList().map { s ->
                ScreenshotBackupItem(
                    originalUri = s.originalUri,
                    extractedText = s.extractedText,
                    userNotes = s.userNotes,
                    createdAt = s.createdAt,
                    isFavorite = s.isFavorite
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            appVersion = "1.0.15",
            exportedAt = System.currentTimeMillis(),
            items = items,
            categories = categories,
            screenshots = screenshotsList
        )
    }

    suspend fun createBackupJson(
        context: Context,
        linkItemDao: LinkItemDao,
        categoryDao: CategoryDao
    ): String = withContext(Dispatchers.IO) {
        val payload = createBackupPayload(context, linkItemDao, categoryDao)
        json.encodeToString(payload)
    }

    suspend fun createAndSaveBackupFile(
        context: Context,
        linkItemDao: LinkItemDao,
        categoryDao: CategoryDao
    ): Result<BackupCreationResult> = withContext(Dispatchers.IO) {
        try {
            val payload = createBackupPayload(context, linkItemDao, categoryDao)
            val jsonString = json.encodeToString(payload)
            val totalItems = payload.items.size

            // 1. Save to internal app filesDir/backups/
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val internalFile = File(backupDir, BACKUP_FILE_NAME)
            internalFile.writeText(jsonString)

            // Also keep a timestamped copy for version safety
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val timestampedFile = File(backupDir, "momostackbackup_$timeStamp.json")
            timestampedFile.writeText(jsonString)

            // 2. Save directly to public Downloads folder for zero-effort user access
            var publicUri: Uri? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILE_NAME)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MomoStack")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val insertedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (insertedUri != null) {
                        resolver.openOutputStream(insertedUri)?.use { out ->
                            out.write(jsonString.toByteArray(Charsets.UTF_8))
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(insertedUri, values, null, null)
                        publicUri = insertedUri
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val pubDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MomoStack").apply { mkdirs() }
                    val pubFile = File(pubDir, BACKUP_FILE_NAME)
                    pubFile.writeText(jsonString)
                    publicUri = Uri.fromFile(pubFile)
                }
            } catch (_: Exception) {
                // MediaStore public export failure should not break internal backup
            }

            // 3. Post system notification with 1-tap share action
            showBackupSuccessNotification(context, internalFile, totalItems)

            Result.success(
                BackupCreationResult(
                    file = internalFile,
                    publicUri = publicUri,
                    itemCount = totalItems,
                    jsonString = jsonString
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getShareBackupIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MomoStack Vault Backup")
            putExtra(Intent.EXTRA_TEXT, "Here is my MomoStack vault backup file. Open MomoStack on any phone to retrieve it instantly.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun showBackupSuccessNotification(context: Context, backupFile: File, totalItems: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shareIntent = Intent.createChooser(getShareBackupIntent(context, backupFile), "Share MomoStack Backup")
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            1,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MomoStackApp.BACKUP_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💾 MomoStack Backup Ready")
            .setContentText("Saved $totalItems items to $BACKUP_FILE_NAME")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Your vault backup was created successfully ($totalItems links & notes).\nSaved to Downloads/MomoStack/$BACKUP_FILE_NAME.\nReady to transfer or retrieve on any phone."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_momo_logo, "Share File", sharePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BACKUP, notification)
    }

    suspend fun restoreFromInputStream(
        context: Context,
        inputStream: InputStream,
        linkItemDao: LinkItemDao,
        categoryDao: CategoryDao,
        clearExisting: Boolean = false
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            restoreFromJson(context, jsonString, linkItemDao, categoryDao, clearExisting)
        } catch (e: Exception) {
            RestoreResult.Error("Failed to read backup file: ${e.localizedMessage ?: "Unknown file error"}")
        }
    }

    suspend fun restoreFromJson(
        context: Context,
        jsonString: String,
        linkItemDao: LinkItemDao,
        categoryDao: CategoryDao,
        clearExisting: Boolean = false
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val payload = json.decodeFromString<BackupPayload>(jsonString)

            if (payload.backupVersion > CURRENT_BACKUP_VERSION) {
                return@withContext RestoreResult.Error(
                    "Unsupported backup version (${payload.backupVersion}). Please update MomoStack to restore this file."
                )
            }

            if (clearExisting) {
                linkItemDao.clearAll()
            }

            val entities = payload.items.map { LinkItemEntity.fromDomain(it) }
            linkItemDao.insertAll(entities)

            for (cat in payload.categories) {
                categoryDao.insertCategory(cat)
            }

            var restoredScreenshots = 0
            if (payload.screenshots.isNotEmpty()) {
                try {
                    val sDao = ScreenshotVaultDatabase.getInstance(context).screenshotDao()
                    val sEntities = payload.screenshots.map { s ->
                        ScreenshotEntity(
                            localImagePath = "",
                            originalUri = s.originalUri,
                            extractedText = s.extractedText,
                            userNotes = s.userNotes,
                            createdAt = s.createdAt,
                            characterCount = s.extractedText.length,
                            wordCount = if (s.extractedText.isBlank()) 0 else s.extractedText.trim().split("\\s+".toRegex()).size,
                            isFavorite = s.isFavorite
                        )
                    }
                    sDao.insertAll(sEntities)
                    restoredScreenshots = sEntities.size
                } catch (_: Exception) {}
            }

            showRestoreSuccessNotification(context, entities.size, payload.categories.size)

            RestoreResult.Success(
                restoredItemsCount = entities.size,
                restoredCategoriesCount = payload.categories.size,
                restoredScreenshotsCount = restoredScreenshots
            )
        } catch (e: Exception) {
            RestoreResult.Error("Failed to parse backup archive: ${e.localizedMessage ?: "Invalid JSON format"}")
        }
    }

    private fun showRestoreSuccessNotification(context: Context, itemsCount: Int, categoriesCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MomoStackApp.BACKUP_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎉 Vault Restored Successfully")
            .setContentText("Retrieved $itemsCount links & notes, $categoriesCount categories.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "All items have been restored into your MomoStack vault.\nOpen the app to see your links, notes, and tags ready to use!"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_RESTORE, notification)
    }
}
