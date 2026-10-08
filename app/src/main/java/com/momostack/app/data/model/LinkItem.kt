package com.momostack.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProcessingStatus {
    PENDING,
    ENRICHING,
    COMPLETED,
    FAILED_METADATA
}

@Serializable
data class LinkItem(
    val id: String,
    val url: String,
    val normalizedUrl: String? = null,
    val title: String,
    val domain: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val category: String = "Uncategorized",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val status: ProcessingStatus = ProcessingStatus.PENDING,
    val faviconUrl: String? = null,
    val previewImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
