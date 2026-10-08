package com.momostack.app.util

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class PlatformMetadata(
    val title: String?,
    val description: String?,
    val previewImageUrl: String?,
    val faviconUrl: String?,
    val category: String,
    val domain: String?
)

object PlatformMetadataExtractor {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val YOUTUBE_PATTERN = Pattern.compile(
        "(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=|shorts\\/))([a-zA-Z0-9_-]{11})",
        Pattern.CASE_INSENSITIVE
    )

    private val TWITTER_PATTERN = Pattern.compile(
        "(?:twitter\\.com|x\\.com)\\/([a-zA-Z0-9_]+)\\/status\\/([0-9]+)",
        Pattern.CASE_INSENSITIVE
    )

    private val INSTAGRAM_PATTERN = Pattern.compile(
        "instagram\\.com\\/(?:p|reel|reels|tv)\\/([a-zA-Z0-9_-]+)",
        Pattern.CASE_INSENSITIVE
    )

    private val THREADS_PATTERN = Pattern.compile(
        "threads\\.net\\/(?:@([a-zA-Z0-9_.]+)\\/post\\/([a-zA-Z0-9_-]+)|t\\/([a-zA-Z0-9_-]+))",
        Pattern.CASE_INSENSITIVE
    )

    private val GITHUB_PATTERN = Pattern.compile(
        "github\\.com\\/([a-zA-Z0-9_.-]+)\\/([a-zA-Z0-9_.-]+)",
        Pattern.CASE_INSENSITIVE
    )

    private val HUGGINGFACE_PATTERN = Pattern.compile(
        "huggingface\\.co\\/(?:models\\/|spaces\\/|datasets\\/)?([a-zA-Z0-9_.-]+)\\/([a-zA-Z0-9_.-]+)",
        Pattern.CASE_INSENSITIVE
    )

    fun isSocialPlatform(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be") ||
               lower.contains("twitter.com") || lower.contains("x.com") ||
               lower.contains("instagram.com") || lower.contains("threads.net") ||
               lower.contains("github.com") || lower.contains("huggingface.co")
    }

    fun extract(url: String): PlatformMetadata? {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> extractYouTube(url)
            lower.contains("twitter.com") || lower.contains("x.com") -> extractTwitter(url)
            lower.contains("instagram.com") -> extractInstagram(url)
            lower.contains("threads.net") -> extractThreads(url)
            lower.contains("github.com") -> extractGitHub(url)
            lower.contains("huggingface.co") -> extractHuggingFace(url)
            else -> null
        }
    }

    private fun extractYouTube(url: String): PlatformMetadata {
        val matcher = YOUTUBE_PATTERN.matcher(url)
        val videoId = if (matcher.find()) matcher.group(1) else null
        val defaultThumbnail = if (videoId != null) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        } else null

        var title: String? = null
        var author: String? = null
        var oembedThumbnail: String? = null

        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=" + java.net.URLEncoder.encode(url, "UTF-8") + "&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    title = json.optString("title").takeIf { it.isNotBlank() }
                    author = json.optString("author_name").takeIf { it.isNotBlank() }
                    oembedThumbnail = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
                }
            }
        } catch (_: Exception) {}

        val finalTitle = title ?: (if (author != null) "YouTube Video by $author" else "YouTube Video")
        val finalDesc = if (author != null) "Watch on YouTube • by $author" else "Watch video on YouTube"
        val finalImage = oembedThumbnail ?: defaultThumbnail

        return PlatformMetadata(
            title = finalTitle,
            description = finalDesc,
            previewImageUrl = finalImage,
            faviconUrl = "https://www.youtube.com/s/desktop/favicon.ico",
            category = "YouTube",
            domain = "youtube.com"
        )
    }

    private fun extractTwitter(url: String): PlatformMetadata {
        val matcher = TWITTER_PATTERN.matcher(url)
        var username: String? = null
        var tweetId: String? = null
        if (matcher.find()) {
            username = matcher.group(1)
            tweetId = matcher.group(2)
        }

        var title: String? = null
        var desc: String? = null
        var previewImage: String? = null

        // 1. Try FxTwitter API for full rich preview (includes photo/video attachments!)
        if (username != null && tweetId != null) {
            try {
                val fxUrl = "https://api.fxtwitter.com/$username/status/$tweetId"
                val request = Request.Builder()
                    .url(fxUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val tweetObj = json.optJSONObject("tweet")
                        if (tweetObj != null) {
                            val text = tweetObj.optString("text")
                            val authorObj = tweetObj.optJSONObject("author")
                            val authorName = authorObj?.optString("name") ?: username
                            val authorAvatar = authorObj?.optString("avatar_url")
                            
                            val mediaObj = tweetObj.optJSONObject("media")
                            val photosArr = mediaObj?.optJSONArray("photos")
                            val firstPhoto = photosArr?.optJSONObject(0)?.optString("url")

                            title = "@$username on X: \"${text.take(80)}\""
                            desc = text
                            previewImage = firstPhoto ?: authorAvatar
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Fallback to Twitter Publish oEmbed if fxtwitter was unavailable
        if (title == null) {
            try {
                val oembedUrl = "https://publish.twitter.com/oembed?url=" + java.net.URLEncoder.encode(url, "UTF-8") + "&omit_script=true"
                val request = Request.Builder().url(oembedUrl).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val authorName = json.optString("author_name")
                        val html = json.optString("html")
                        val doc = Jsoup.parse(html)
                        val tweetText = doc.select("p").text()
                        title = if (authorName.isNotBlank()) "Post by $authorName (@$username)" else "Post on X"
                        desc = tweetText
                    }
                }
            } catch (_: Exception) {}
        }

        return PlatformMetadata(
            title = title ?: (if (username != null) "Post by @$username on X" else "Post on X (Twitter)"),
            description = desc ?: "View post on X",
            previewImageUrl = previewImage,
            faviconUrl = "https://abs.twimg.com/favicons/twitter.3.ico",
            category = "Twitter",
            domain = "x.com"
        )
    }

    private fun extractInstagram(url: String): PlatformMetadata {
        val matcher = INSTAGRAM_PATTERN.matcher(url)
        val shortcode = if (matcher.find()) matcher.group(1) else null

        var title: String? = null
        var desc: String? = null
        var previewImage: String? = null

        // Request with facebookexternalhit bot user-agent: Instagram serves full OpenGraph tags to social scrapers!
        try {
            val cleanUrl = if (shortcode != null) "https://www.instagram.com/p/$shortcode/" else url
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val doc = Jsoup.parse(body)
                title = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                desc = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                previewImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {}

        // Fallback: direct media redirect URL if no og:image parsed
        if (previewImage == null && shortcode != null) {
            previewImage = "https://www.instagram.com/p/$shortcode/media/?size=l"
        }

        val finalTitle = title?.takeIf { it.isNotBlank() }
            ?: (if (shortcode != null) "Instagram Post ($shortcode)" else "Instagram Post")

        return PlatformMetadata(
            title = finalTitle,
            description = desc ?: "View photo/reel on Instagram",
            previewImageUrl = previewImage,
            faviconUrl = "https://static.cdninstagram.com/rsrc.php/v3/yI/r/VsNE-OHk_8a.png",
            category = "Instagram",
            domain = "instagram.com"
        )
    }

    private fun extractThreads(url: String): PlatformMetadata {
        val matcher = THREADS_PATTERN.matcher(url)
        val username = if (matcher.find()) matcher.group(1) else null

        var title: String? = null
        var desc: String? = null
        var previewImage: String? = null

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val doc = Jsoup.parse(body)
                title = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                desc = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                previewImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {}

        return PlatformMetadata(
            title = title ?: (if (username != null) "Threads post by @$username" else "Threads post"),
            description = desc ?: "View conversation on Threads",
            previewImageUrl = previewImage,
            faviconUrl = "https://static.cdninstagram.com/rsrc.php/yD/r/5Ddg9uA_S5Q.ico",
            category = "Threads",
            domain = "threads.net"
        )
    }

    private fun extractGitHub(url: String): PlatformMetadata {
        val matcher = GITHUB_PATTERN.matcher(url)
        var owner: String? = null
        var repo: String? = null
        if (matcher.find()) {
            val candOwner = matcher.group(1)
            val candRepo = matcher.group(2).removeSuffix(".git")
            val nonRepoOwners = setOf("features", "topics", "collections", "trending", "explore", "pricing", "marketplace", "login", "signup", "settings", "notifications")
            if (!nonRepoOwners.contains(candOwner.lowercase())) {
                owner = candOwner
                repo = candRepo
            }
        }

        var title: String? = null
        var description: String? = null
        var previewImage: String? = null

        if (owner != null && repo != null) {
            title = "$owner/$repo"
            // High-resolution official GitHub OpenGraph social card
            previewImage = "https://opengraph.githubassets.com/1/$owner/$repo"

            try {
                val apiUrl = "https://api.github.com/repos/$owner/$repo"
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "MomoStack/1.0")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val repoDesc = json.optString("description").takeIf { it.isNotBlank() }
                        val stars = json.optInt("stargazers_count", 0)
                        val language = json.optString("language").takeIf { it.isNotBlank() }
                        val extra = buildString {
                            if (language != null) append("[$language] ")
                            if (stars > 0) append("⭐ $stars • ")
                            if (repoDesc != null) append(repoDesc)
                        }
                        description = if (extra.isNotBlank()) extra else repoDesc
                    }
                }
            } catch (_: Exception) {}
        }

        if (description.isNullOrBlank() || previewImage.isNullOrBlank()) {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("facebookexternalhit/1.1")
                    .timeout(10000)
                    .get()
                if (title == null) {
                    title = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                        ?: doc.title()
                }
                if (description == null) {
                    description = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                        ?: doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
                }
                if (previewImage == null) {
                    previewImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                }
            } catch (_: Exception) {}
        }

        return PlatformMetadata(
            title = title ?: (if (repo != null) repo else "GitHub Repository"),
            description = description ?: "View code, issues, and discussions on GitHub.",
            previewImageUrl = previewImage ?: (if (owner != null && repo != null) "https://opengraph.githubassets.com/1/$owner/$repo" else null),
            faviconUrl = "https://github.githubassets.com/favicons/favicon.png",
            category = "GitHub",
            domain = "github.com"
        )
    }

    private fun extractHuggingFace(url: String): PlatformMetadata {
        val matcher = HUGGINGFACE_PATTERN.matcher(url)
        var owner: String? = null
        var modelOrSpace: String? = null
        if (matcher.find()) {
            owner = matcher.group(1)
            modelOrSpace = matcher.group(2)
        }

        var title: String? = if (owner != null && modelOrSpace != null) "$owner/$modelOrSpace" else null
        var description: String? = null
        var previewImage: String? = null

        if (owner != null && modelOrSpace != null && !url.contains("/spaces/") && !url.contains("/datasets/")) {
            try {
                val apiUrl = "https://huggingface.co/api/models/$owner/$modelOrSpace"
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "MomoStack/1.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val pipeline = json.optString("pipeline_tag").takeIf { it.isNotBlank() }
                        val likes = json.optInt("likes", 0)
                        val downloads = json.optInt("downloads", 0)
                        val extra = buildString {
                            if (pipeline != null) append("[$pipeline] ")
                            if (likes > 0) append("❤️ $likes • ")
                            if (downloads > 0) append("⬇️ $downloads")
                        }
                        if (extra.isNotBlank()) description = extra
                    }
                }
            } catch (_: Exception) {}
        }

        try {
            val doc = Jsoup.connect(url)
                .userAgent("facebookexternalhit/1.1")
                .timeout(10000)
                .get()
            if (title == null) {
                title = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                    ?: doc.title()
            }
            val ogDesc = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
            if (description == null && ogDesc != null) {
                description = ogDesc
            } else if (description != null && ogDesc != null) {
                description = "$description • $ogDesc"
            }
            previewImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
        } catch (_: Exception) {}

        return PlatformMetadata(
            title = title ?: (if (modelOrSpace != null) modelOrSpace else "Hugging Face Model"),
            description = description ?: "Explore machine learning models, datasets, and apps on Hugging Face.",
            previewImageUrl = previewImage,
            faviconUrl = "https://huggingface.co/front/assets/huggingface_logo-noborder.svg",
            category = "Hugging Face",
            domain = "huggingface.co"
        )
    }
}
