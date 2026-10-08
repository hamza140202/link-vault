package com.momostack.app.classifier

object AutoClassifier {

    private val DOMAIN_RULES = mapOf(
        // Development
        "github.com" to "Development",
        "gitlab.com" to "Development",
        "stackoverflow.com" to "Development",
        "developer.android.com" to "Development",
        "kotlinlang.org" to "Development",
        "dev.to" to "Development",
        "hashnode.dev" to "Development",
        "codepen.io" to "Development",
        "npmjs.com" to "Development",
        "pypi.org" to "Development",

        // Reading / Research
        "medium.com" to "Reading",
        "substack.com" to "Reading",
        "wikipedia.org" to "Reading",
        "arxiv.org" to "Reading",
        "theverge.com" to "Reading",
        "techcrunch.com" to "Reading",
        "wired.com" to "Reading",
        "economist.com" to "Reading",
        "theatlantic.com" to "Reading",

        // Social & Video Platforms (Dedicated platform categorization)
        "youtube.com" to "YouTube",
        "youtu.be" to "YouTube",
        "instagram.com" to "Instagram",
        "threads.net" to "Threads",
        "twitter.com" to "Twitter",
        "x.com" to "Twitter",
        "reddit.com" to "Reddit",
        "tiktok.com" to "TikTok",

        // Entertainment
        "spotify.com" to "Entertainment",
        "netflix.com" to "Entertainment",
        "twitch.tv" to "Entertainment",
        "vimeo.com" to "Entertainment",
        "soundcloud.com" to "Entertainment",

        // Shopping
        "amazon.com" to "Shopping",
        "ebay.com" to "Shopping",
        "etsy.com" to "Shopping",
        "target.com" to "Shopping",
        "walmart.com" to "Shopping",
        "aliexpress.com" to "Shopping",

        // Finance
        "bloomberg.com" to "Finance",
        "wsj.com" to "Finance",
        "reuters.com" to "Finance",
        "cnbc.com" to "Finance",
        "investopedia.com" to "Finance",
        "coinmarketcap.com" to "Finance",

        // Work & Productivity
        "notion.so" to "Work",
        "figma.com" to "Work",
        "jira.atlassian.com" to "Work",
        "slack.com" to "Work",
        "docs.google.com" to "Work",
        "linear.app" to "Work",
        "asana.com" to "Work",
        "trello.com" to "Work"
    )

    private val KEYWORD_RULES = listOf(
        Pair(setOf("git", "github", "commit", "api", "sdk", "compiler", "framework", "tutorial", "coding", "software", "bug", "python", "kotlin", "javascript", "rust"), "Development"),
        Pair(setOf("trailer", "movie", "soundtrack", "album", "podcast", "stream", "gameplay", "music"), "Entertainment"),
        Pair(setOf("discount", "coupon", "cart", "checkout", "price", "order", "shipping", "store"), "Shopping"),
        Pair(setOf("stock", "dividend", "crypto", "bitcoin", "market", "nasdaq", "etf", "investing"), "Finance"),
        Pair(setOf("roadmap", "sprint", "meeting", "workflow", "project", "kanban", "workspace"), "Work")
    )

    fun classify(domain: String?, title: String?, description: String?): String {
        val cleanDomain = domain?.lowercase()?.trim()

        if (cleanDomain != null) {
            for ((keyDomain, category) in DOMAIN_RULES) {
                if (cleanDomain == keyDomain || cleanDomain.endsWith(".$keyDomain")) {
                    return category
                }
            }
        }

        val combinedContent = buildString {
            if (!title.isNullOrBlank()) append(title.lowercase()).append(" ")
            if (!description.isNullOrBlank()) append(description.lowercase())
        }

        if (combinedContent.isNotBlank()) {
            for ((keywords, category) in KEYWORD_RULES) {
                var matches = 0
                for (kw in keywords) {
                    if (combinedContent.contains(kw)) {
                        matches++
                        if (matches >= 2) {
                            return category
                        }
                    }
                }
            }
        }

        return "Uncategorized"
    }
}
