package com.linkvault.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "categories")
@Serializable
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isSystem: Boolean = false
)
