package com.momostack.app.util

import android.content.ClipboardManager
import android.content.Context
import android.text.Html
import android.text.Spanned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.momostack.app.ui.theme.IndigoPrimary
import java.util.regex.Pattern

object RichTextFormatter {

    fun containsHtml(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return lower.contains("<h1") || lower.contains("<h2") || lower.contains("<h3") ||
                lower.contains("<h4") || lower.contains("<h5") || lower.contains("<h6") ||
                lower.contains("<b") || lower.contains("<strong") ||
                lower.contains("<i") || lower.contains("<em") ||
                lower.contains("<u") || lower.contains("<s") || lower.contains("<del") || lower.contains("<strike") ||
                lower.contains("<ul") || lower.contains("<ol") || lower.contains("<li") ||
                lower.contains("<p") || lower.contains("<br") || lower.contains("<div") ||
                lower.contains("<span") || lower.contains("<blockquote") ||
                lower.contains("<pre") || lower.contains("<code") || lower.contains("<a ") ||
                lower.contains("style=") || lower.contains("&nbsp;") || lower.contains("&amp;")
    }

    fun htmlToMarkdown(html: String): String {
        if (html.isBlank()) return ""

        var md = html
            // Strip head, script, style, comments
            .replace(Regex("(?is)<head[^>]*>.*?</head>"), "")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), "")
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
            .replace(Regex("(?is)<!--.*?-->"), "")

        // Line breaks and paragraph breaks
        md = md.replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)<p[^>]*>"), "")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)<div[^>]*>"), "")
            .replace(Regex("(?i)<hr\\s*/?>"), "\n---\n\n")

        // Chrome & Web styled spans: font-weight (bold, 700, 800, 900)
        md = md.replace(Regex("(?is)<span[^>]*style=[\"'][^\"']*font-weight:\\s*(?:bold|[7-9]00)[^\"']*[\"'][^>]*>(.*?)</span>")) {
            "**${it.groupValues[1].trim()}**"
        }

        // Chrome & Web styled spans: font-style: italic
        md = md.replace(Regex("(?is)<span[^>]*style=[\"'][^\"']*font-style:\\s*italic[^\"']*[\"'][^>]*>(.*?)</span>")) {
            "*${it.groupValues[1].trim()}*"
        }

        // Chrome & Web styled spans: line-through
        md = md.replace(Regex("(?is)<span[^>]*style=[\"'][^\"']*text-decoration:[^\"']*line-through[^\"']*[\"'][^>]*>(.*?)</span>")) {
            "~~${it.groupValues[1].trim()}~~"
        }

        // Headers
        md = md.replace(Regex("(?is)<h1[^>]*>(.*?)</h1>")) { "# ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?is)<h2[^>]*>(.*?)</h2>")) { "## ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?is)<h3[^>]*>(.*?)</h3>")) { "### ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?is)<h4[^>]*>(.*?)</h4>")) { "#### ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?is)<h5[^>]*>(.*?)</h5>")) { "##### ${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("(?is)<h6[^>]*>(.*?)</h6>")) { "###### ${it.groupValues[1].trim()}\n\n" }

        // Bold and Italic tags (run twice to handle nesting like <b><i>text</i></b>)
        for (i in 0..1) {
            md = md.replace(Regex("(?is)<(b|strong)[^>]*>(.*?)</\\1>")) { "**${it.groupValues[2].trim()}**" }
            md = md.replace(Regex("(?is)<(i|em)[^>]*>(.*?)</\\1>")) { "*${it.groupValues[2].trim()}*" }
            md = md.replace(Regex("(?is)<(s|del|strike)[^>]*>(.*?)</\\1>")) { "~~${it.groupValues[2].trim()}~~" }
            md = md.replace(Regex("(?is)<u[^>]*>(.*?)</u>")) { "__${it.groupValues[1].trim()}__" }
        }

        // Code
        md = md.replace(Regex("(?is)<pre[^>]*><code[^>]*>(.*?)</code></pre>")) { "```\n${it.groupValues[1].trim()}\n```\n\n" }
        md = md.replace(Regex("(?is)<pre[^>]*>(.*?)</pre>")) { "```\n${it.groupValues[1].trim()}\n```\n\n" }
        md = md.replace(Regex("(?is)<code[^>]*>(.*?)</code>")) { "`${it.groupValues[1].trim()}`" }

        // Lists
        md = md.replace(Regex("(?is)<li[^>]*>(.*?)</li>")) { "- ${it.groupValues[1].trim()}\n" }
        md = md.replace(Regex("(?is)</?[ou]l[^>]*>"), "\n")

        // Blockquotes
        md = md.replace(Regex("(?is)<blockquote[^>]*>(.*?)</blockquote>")) {
            val content = it.groupValues[1].trim()
            content.lines().joinToString("\n") { line -> "> $line" } + "\n\n"
        }

        // Links
        md = md.replace(Regex("(?is)<a\\s+[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>")) {
            val href = it.groupValues[1].trim()
            val text = it.groupValues[2].trim()
            if (text.isNotBlank() && text != href) "[$text]($href)" else href
        }

        // Strip remaining HTML tags
        md = md.replace(Regex("<[^>]+>"), "")

        // Decode HTML entities
        md = decodeHtmlEntities(md)

        // Fix malformed markdown tags (e.g., "** text **" -> "**text**") without stripping spaces between words
        md = md.replace(Regex("\\*\\*\\s*([^\\*\\r\\n]+?)\\s*\\*\\*")) {
            "**${it.groupValues[1].trim()}**"
        }
        md = md.replace(Regex("(?<!\\*)\\*\\s*([^\\*\\r\\n]+?)\\s*\\*(?!\\*)")) {
            "*${it.groupValues[1].trim()}*"
        }

        // Clean excess blank lines
        md = md.replace(Regex("\n{3,}"), "\n\n")

        return md.trim()
    }

    private fun decodeHtmlEntities(input: String): String {
        var res = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&bull;", "•")

        // Numeric entities: &#123; or &#x7b;
        res = res.replace(Regex("&#([0-9]+);")) {
            try {
                it.groupValues[1].toInt().toChar().toString()
            } catch (_: Exception) {
                it.value
            }
        }
        res = res.replace(Regex("&#x([0-9a-fA-F]+);")) {
            try {
                it.groupValues[1].toInt(16).toChar().toString()
            } catch (_: Exception) {
                it.value
            }
        }
        return res
    }

    fun smartFormatClipboard(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null

        val item = clip.getItemAt(0)

        // 1. Direct HTML text from clipboard (Chrome, web browsers, rich apps)
        val html = item.htmlText
        if (!html.isNullOrBlank() && containsHtml(html)) {
            val converted = htmlToMarkdown(html)
            if (converted.isNotBlank()) return converted
        }

        // 2. Spanned text with styles (Android apps, Docs, Keep, Word, etc.)
        val text = item.text
        if (text is Spanned && text.isNotEmpty()) {
            try {
                val convertedHtml = Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                if (containsHtml(convertedHtml)) {
                    val converted = htmlToMarkdown(convertedHtml)
                    if (converted.isNotBlank()) return converted
                }
            } catch (_: Exception) {
                // Fallback gracefully
            }
        }

        // 3. Coerce to HTML text fallback
        try {
            val coercedHtml = item.coerceToHtmlText(context)
            if (!coercedHtml.isNullOrBlank() && containsHtml(coercedHtml)) {
                val converted = htmlToMarkdown(coercedHtml)
                if (converted.isNotBlank()) return converted
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }

        // 4. Plain text fallback
        val plain = text?.toString() ?: item.coerceToText(context)?.toString() ?: ""
        if (containsHtml(plain)) {
            return htmlToMarkdown(plain)
        }

        return plain.ifBlank { null }
    }

    fun smartConvertPastedText(pastedText: String, context: Context): String {
        if (pastedText.isBlank()) return pastedText

        // Check if clipboard contains a richer formatted version of this text
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            val clipItem = if (clip != null && clip.itemCount > 0) clip.getItemAt(0) else null
            val clipPlain = (clipItem?.text?.toString() ?: clipItem?.coerceToText(context)?.toString() ?: "").trim()
            val pastedTrimmed = pastedText.trim()

            val formatted = smartFormatClipboard(context)
            if (!formatted.isNullOrBlank() && formatted != pastedText) {
                val normPasted = pastedTrimmed.replace(Regex("[^\\p{L}\\p{N}]"), "")
                val normClip = clipPlain.replace(Regex("[^\\p{L}\\p{N}]"), "")
                val normFormatted = formatted.replace(Regex("[^\\p{L}\\p{N}]"), "")

                val plainOfFormatted = formatted.replace(Regex("[-#*`~\\[\\]()•]"), "").trim()

                if (clipPlain == pastedTrimmed ||
                    clipPlain.contains(pastedTrimmed) ||
                    pastedTrimmed.contains(clipPlain) ||
                    plainOfFormatted == pastedTrimmed ||
                    plainOfFormatted.contains(pastedTrimmed) ||
                    pastedTrimmed.contains(plainOfFormatted) ||
                    (normPasted.isNotEmpty() && (normClip == normPasted || normFormatted == normPasted || normFormatted.contains(normPasted) || normPasted.contains(normFormatted)))
                ) {
                    return formatted
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }

        // Direct HTML inside pasted string
        if (containsHtml(pastedText)) {
            return htmlToMarkdown(pastedText)
        }

        return pastedText
    }

    /**
     * Compose VisualTransformation that live-styles Markdown inside OutlinedTextField:
     * - Headers: larger, bold, accented
     * - Bold (**text**): FontWeight.Bold
     * - Italic (*text*): FontStyle.Italic
     * - Inline code (`code`): Monospace font
     * - Links ([text](url)): Underlined, accented
     */
    fun createMarkdownVisualTransformation(isDarkTheme: Boolean = false): VisualTransformation {
        return VisualTransformation { text ->
            val raw = text.text
            val builder = AnnotatedString.Builder(raw)

            // Bold: **text**
            val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
            boldRegex.findAll(raw).forEach { match ->
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Italic: *text* (excluding **)
            val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
            italicRegex.findAll(raw).forEach { match ->
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Strikethrough: ~~text~~
            val strikeRegex = Regex("~~(.+?)~~")
            strikeRegex.findAll(raw).forEach { match ->
                builder.addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Code: `code`
            val codeRegex = Regex("`([^`]+)`")
            codeRegex.findAll(raw).forEach { match ->
                builder.addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Headers: # Heading
            val headerRegex = Regex("^(#{1,6})\\s*(.+)$", RegexOption.MULTILINE)
            headerRegex.findAll(raw).forEach { match ->
                val level = match.groupValues[1].length
                val fontSize = when (level) {
                    1 -> 20.sp
                    2 -> 18.sp
                    3 -> 16.sp
                    else -> 15.sp
                }
                builder.addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = IndigoPrimary
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Links: [text](url)
            val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)")
            linkRegex.findAll(raw).forEach { match ->
                builder.addStyle(
                    SpanStyle(
                        color = IndigoPrimary,
                        textDecoration = TextDecoration.Underline
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    /**
     * Parses markdown into a clean AnnotatedString for display in Note cards and Detail view.
     */
    fun parseMarkdownToAnnotatedString(markdown: String, isDarkTheme: Boolean = false): AnnotatedString {
        if (markdown.isBlank()) return AnnotatedString("")

        val builder = AnnotatedString.Builder(markdown)

        // Bold
        Regex("\\*\\*(.+?)\\*\\*").findAll(markdown).forEach { match ->
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        // Italic
        Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)").findAll(markdown).forEach { match ->
            builder.addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
        }

        // Headers
        Regex("^(#{1,6})\\s*(.+)$", RegexOption.MULTILINE).findAll(markdown).forEach { match ->
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = IndigoPrimary),
                match.range.first,
                match.range.last + 1
            )
        }

        // Strikethrough
        Regex("~~(.+?)~~").findAll(markdown).forEach { match ->
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough),
                match.range.first,
                match.range.last + 1
            )
        }

        // Code
        Regex("`([^`]+)`").findAll(markdown).forEach { match ->
            builder.addStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        // Links
        Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)").findAll(markdown).forEach { match ->
            builder.addStyle(
                SpanStyle(
                    color = IndigoPrimary,
                    textDecoration = TextDecoration.Underline
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        return builder.toAnnotatedString()
    }
}
