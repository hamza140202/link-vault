package com.linkvault.app.util

import java.net.URI
import java.util.regex.Pattern

object UrlExtractor {

    private val URL_PATTERN = Pattern.compile(
        "(https?://[a-zA-Z0-9\\-_]+(\\.[a-zA-Z0-9\\-_]+)+(:[0-9]+)?(/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?)",
        Pattern.CASE_INSENSITIVE
    )

    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "si", "ref", "igshid", "mc_cid", "mc_eid"
    )

    data class ExtractedResult(
        val url: String?,
        val normalizedUrl: String?,
        val domain: String?,
        val accompanyingText: String?
    )

    fun extract(rawText: String?): ExtractedResult {
        if (rawText.isNullOrBlank()) {
            return ExtractedResult(null, null, null, null)
        }

        val matcher = URL_PATTERN.matcher(rawText)
        if (matcher.find()) {
            val foundUrl = matcher.group(1)?.trim() ?: ""
            val remainingText = rawText.replace(foundUrl, "").trim()
            val normalized = normalizeUrl(foundUrl)
            val domain = extractDomain(normalized)

            return ExtractedResult(
                url = foundUrl,
                normalizedUrl = normalized,
                domain = domain,
                accompanyingText = if (remainingText.isNotBlank()) remainingText else null
            )
        }

        return ExtractedResult(
            url = null,
            normalizedUrl = null,
            domain = null,
            accompanyingText = rawText.trim()
        )
    }

    fun normalizeUrl(rawUrl: String): String {
        return try {
            val uri = URI(rawUrl.trim())
            val scheme = uri.scheme?.lowercase() ?: "https"
            val host = uri.host?.lowercase() ?: return rawUrl
            val port = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            val path = uri.rawPath ?: ""

            val cleanedQuery = if (!uri.rawQuery.isNullOrBlank()) {
                val queryParams = uri.rawQuery.split("&").filter { param ->
                    val key = param.substringBefore("=").lowercase()
                    !TRACKING_PARAMS.contains(key)
                }
                if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
            } else ""

            "$scheme://$host$port$path$cleanedQuery"
        } catch (_: Exception) {
            rawUrl.trim()
        }
    }

    fun extractDomain(url: String): String? {
        return try {
            val uri = URI(url.trim())
            val host = uri.host?.lowercase() ?: return null
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (_: Exception) {
            null
        }
    }
}
