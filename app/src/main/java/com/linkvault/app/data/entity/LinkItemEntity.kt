package com.linkvault.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.linkvault.app.data.model.LinkItem
import com.linkvault.app.data.model.ProcessingStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "link_items",
    indices = [
        Index(value = ["isArchived", "createdAt"]),
        Index(value = ["category", "isArchived", "createdAt"]),
        Index(value = ["isFavorite"]),
        Index(value = ["url"]),
        Index(value = ["domain"])
    ]
)
data class LinkItemEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val normalizedUrl: String?,
    val title: String,
    val domain: String?,
    val description: String?,
    val notes: String?,
    val category: String,
    val tagsJson: String,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val status: String,
    val faviconUrl: String?,
    val previewImageUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): LinkItem {
        val parsedTags = try {
            Json.decodeFromString<List<String>>(tagsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val parsedStatus = try {
            ProcessingStatus.valueOf(status)
        } catch (_: Exception) {
            ProcessingStatus.PENDING
        }

        return LinkItem(
            id = id,
            url = url,
            normalizedUrl = normalizedUrl,
            title = title,
            domain = domain,
            description = description,
            notes = notes,
            category = category,
            tags = parsedTags,
            isFavorite = isFavorite,
            isArchived = isArchived,
            status = parsedStatus,
            faviconUrl = faviconUrl,
            previewImageUrl = previewImageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(item: LinkItem): LinkItemEntity {
            return LinkItemEntity(
                id = item.id,
                url = item.url,
                normalizedUrl = item.normalizedUrl,
                title = item.title,
                domain = item.domain,
                description = item.description,
                notes = item.notes,
                category = item.category,
                tagsJson = Json.encodeToString(item.tags),
                isFavorite = item.isFavorite,
                isArchived = item.isArchived,
                status = item.status.name,
                faviconUrl = item.faviconUrl,
                previewImageUrl = item.previewImageUrl,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
    }
}
