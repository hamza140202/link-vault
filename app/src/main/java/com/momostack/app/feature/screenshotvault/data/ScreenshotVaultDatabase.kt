package com.momostack.app.feature.screenshotvault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Isolated Room Database for the Screenshot Vault feature.
 * Keeping this completely independent from LinkVaultDatabase ensures:
 * 1. Zero risk to existing user links and notes.
 * 2. No migration conflicts or schema upgrades required for link_vault.db.
 * 3. Clean sandboxing: data can be queried, backed up, or purged independently.
 */
@Database(
    entities = [ScreenshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScreenshotVaultDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenshotVaultDatabase? = null

        fun getDatabase(context: Context): ScreenshotVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScreenshotVaultDatabase::class.java,
                    "screenshot_vault.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
