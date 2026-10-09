package com.momostack.app.util

import android.content.ClipboardManager
import android.content.Context
import java.util.regex.Pattern

object RichTextFormatter {

    private val HTML_TAG_PATTERN = Pattern.compile("<(\\w+)[^>]*>(.*?)</\\1>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)

    fun containsHtml(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return lower.contains("<h1") || lower.contains("<h2") || lower.contains("<h3") ||
                lower.contains("<h4") || lower.contains("<b") || lower.contains("<strong") ||
                lower.contains("<i") || lower.contains("<em") || lower.contains("<ul") ||
                lower.contains("<li") || lower.contains("<p") || lower.contains("<br") ||
                lower.contains("<div>")
    }

    fun htmlToMarkdown(html: String): String {
        if (html.isBlank()) return ""

        var md = html
            // Replace line breaks and paragraph breaks
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)<p[^>]*>"), "")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)<div[^>]*>"), "")

        // Headers
        md = md.replace(Regex("(?i)<h1[^>]*>(.*?)</h1>", RegexOption.DOT_MATCHES_ALL)) { "# ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?i)<h2[^>]*>(.*?)</h2>", RegexOption.DOT_MATCHES_ALL)) { "## ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?i)<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL)) { "### ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?i)<h4[^>]*>(.*?)</h4>", RegexOption.DOT_MATCHES_ALL)) { "#### ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?i)<h5[^>]*>(.*?)</h5>", RegexOption.DOT_MATCHES_ALL)) { "##### ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?i)<h6[^>]*>(.*?)</h6>", RegexOption.DOT_MATCHES_ALL)) { "###### ${it.groupValues[1].trim()}\n\n" }

        // Bold and Italic
        md = md.replace(Regex("(?i)<(b|strong)[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)) { "**${it.groupValues[2].trim()}**" }
        md = md.replace(Regex("(?i)<(i|em)[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)) { "*${it.groupValues[2].trim()}*" }
        md = md.replace(Regex("(?i)<(s|del|strike)[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)) { "~~${it.groupValues[2].trim()}~~" }

        // Code
        md = md.replace(Regex("(?i)<pre[^>]*><code[^>]*>(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL)) { "```\n${it.groupValues[1].trim()}\n```\n\n" }
        md = md.replace(Regex("(?i)<code[^>]*>(.*?)</code>", RegexOption.DOT_MATCHES_ALL)) { "`${it.groupValues[1].trim()}`" }

        // Lists
        md = md.replace(Regex("(?i)<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)) { "- ${it.groupValues[1].trim()}\n" }
        md = md.replace(Regex("(?i)</?[ou]l[^>]*>"), "\n")

        // Blockquotes
        md = md.replace(Regex("(?i)<blockquote[^>]*>(.*?)</blockquote>", RegexOption.DOT_MATCHES_ALL)) {
            val content = it.groupValues[1].trim()
            content.lines().joinToString("\n") { line -> "> $line" } + "\n\n"
        }

        // Links
        md = md.replace(Regex("(?i)<a\\s+[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)) {
            val href = it.groupValues[1].trim()
            val text = it.groupValues[2].trim()
            if (text.isNotBlank() && text != href) "[$text]($href)" else href
        }

        // Strip remaining HTML tags
        md = md.replace(Regex("<[^>]+>"), "")

        // Decode HTML entities
        md = md.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        // Clean excess blank lines
        md = md.replace(Regex("\n{3,}"), "\n\n")

        return md.trim()
    }

    fun smartFormatClipboard(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null

        val item = clip.getItemAt(0)
        val html = item.htmlText
        if (!html.isNullOrBlank() && containsHtml(html)) {
            return htmlToMarkdown(html)
        }

        val plain = item.text?.toString() ?: item.coerceToText(context)?.toString() ?: ""
        if (containsHtml(plain)) {
            return htmlToMarkdown(plain)
        }

        return plain.ifBlank { null }
    }

    fun smartConvertPastedText(pastedText: String, context: Context): String {
        if (pastedText.isBlank()) return pastedText

        // 1. Direct HTML in pasted string
        if (containsHtml(pastedText)) {
            return htmlToMarkdown(pastedText)
        }

        // 2. Check if clipboard has richer HTML variant corresponding to this text
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val html = item.htmlText
                val plain = item.text?.toString() ?: item.coerceToText(context)?.toString() ?: ""

                if (!html.isNullOrBlank() && containsHtml(html)) {
                    // If the plain text from clipboard matches the pasted text, use converted HTML markdown
                    if (plain.trim() == pastedText.trim()) {
                        return htmlToMarkdown(html)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }

        return pastedText
    }
}
