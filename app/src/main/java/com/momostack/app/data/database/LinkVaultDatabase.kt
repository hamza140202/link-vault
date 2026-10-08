package com.momostack.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.momostack.app.data.dao.CategoryDao
import com.momostack.app.data.dao.LinkItemDao
import com.momostack.app.data.entity.CategoryEntity
import com.momostack.app.data.entity.LinkItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [LinkItemEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LinkVaultDatabase : RoomDatabase() {

    abstract fun linkItemDao(): LinkItemDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: LinkVaultDatabase? = null

        fun getDatabase(context: Context): LinkVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LinkVaultDatabase::class.java,
                    "link_vault.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }

            private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
                val defaults = listOf(
                    CategoryEntity("cat_instagram", "Instagram", "#E1306C", "camera", true),
                    CategoryEntity("cat_youtube", "YouTube", "#FF0000", "play", true),
                    CategoryEntity("cat_twitter", "Twitter", "#1DA1F2", "message-circle", true),
                    CategoryEntity("cat_threads", "Threads", "#000000", "at-sign", true),
                    CategoryEntity("cat_dev", "Development", "#4F46E5", "code", true),
                    CategoryEntity("cat_reading", "Reading", "#0D9488", "book", true),
                    CategoryEntity("cat_entertainment", "Entertainment", "#D97706", "play", true),
                    CategoryEntity("cat_shopping", "Shopping", "#E11D48", "shopping-bag", true),
                    CategoryEntity("cat_finance", "Finance", "#16A34A", "trending-up", true),
                    CategoryEntity("cat_work", "Work", "#2563EB", "briefcase", true),
                    CategoryEntity("cat_uncategorized", "Uncategorized", "#64748B", "folder", true)
                )
                categoryDao.insertDefaultCategories(defaults)
            }
        }
    }
}
