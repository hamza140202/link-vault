package com.momostack.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-performance document exporter for MomoStack notes.
 * Supports exporting and sharing notes as:
 * - Plain Text (.txt)
 * - Microsoft Word / Office Document (.doc) with full typography & styles
 * - Markdown (.md)
 * - Standalone HTML (.html)
 */
object NoteExporter {

    enum class ExportFormat(
        val extension: String,
        val mimeType: String,
        val displayName: String,
        val icon: String,
        val description: String
    ) {
        TXT(
            extension = "txt",
            mimeType = "text/plain",
            displayName = "Text File (.txt)",
            icon = "📄",
            description = "Standard UTF-8 text file, universal compatibility"
        ),
        DOC(
            extension = "doc",
            mimeType = "application/msword",
            displayName = "Word Document (.doc)",
            icon = "📝",
            description = "Office document with rich headings, typography & styles"
        ),
        MARKDOWN(
            extension = "md",
            mimeType = "text/markdown",
            displayName = "Markdown (.md)",
            icon = "📑",
            description = "Preserves raw markdown formatting for Obsidian & editors"
        ),
        HTML(
            extension = "html",
            mimeType = "text/html",
            displayName = "Web Document (.html)",
            icon = "🌐",
            description = "Self-contained responsive page, opens in any browser"
        )
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileTimestampFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Creates and shares note as a file via Android Intent Chooser.
     */
    fun shareNoteAsFile(
        context: Context,
        title: String,
        content: String,
        format: ExportFormat
    ) {
        try {
            val file = createExportFile(context, title, content, format)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val subject = title.ifBlank { "MomoStack Note" }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "$subject (${format.displayName})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Note as ${format.displayName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Saves note file directly to user's device Downloads directory.
     */
    fun saveNoteToDownloads(
        context: Context,
        title: String,
        content: String,
        format: ExportFormat
    ): Boolean {
        return try {
            val fileContent = buildFormattedContent(title, content, format)
            val fileName = buildSafeFileName(title, format.extension)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MomoStack")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Could not create Downloads entry")

                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(fileContent.toByteArray(Charsets.UTF_8))
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "MomoStack").apply { if (!exists()) mkdirs() }
                val targetFile = File(targetDir, fileName)
                FileOutputStream(targetFile).use { stream ->
                    stream.write(fileContent.toByteArray(Charsets.UTF_8))
                }
            }

            Toast.makeText(context, "Saved to Downloads/MomoStack/$fileName ✅", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Creates temporary export file in private storage for FileProvider sharing.
     */
    private fun createExportFile(
        context: Context,
        title: String,
        content: String,
        format: ExportFormat
    ): File {
        val exportDir = File(context.filesDir, "exported_notes").apply {
            if (!exists()) mkdirs()
        }
        val fileName = buildSafeFileName(title, format.extension)
        val file = File(exportDir, fileName)
        val fileContent = buildFormattedContent(title, content, format)
        file.writeText(fileContent, Charsets.UTF_8)
        return file
    }

    private fun buildSafeFileName(title: String, extension: String): String {
        val sanitized = title.trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(40)
            .ifBlank { "MomoStack_Note" }
        val stamp = fileTimestampFormatter.format(Date())
        return "${sanitized}_$stamp.$extension"
    }

    private fun buildFormattedContent(
        title: String,
        content: String,
        format: ExportFormat
    ): String {
        val now = dateFormatter.format(Date())
        val wordCount = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).size
        val charCount = content.length
        val displayTitle = title.ifBlank { "Untitled Note" }

        return when (format) {
            ExportFormat.TXT -> {
                buildString {
                    appendLine("=".repeat(50))
                    appendLine(displayTitle)
                    appendLine("Exported from MomoStack · $now")
                    appendLine("$wordCount words · $charCount characters")
                    appendLine("=".repeat(50))
                    appendLine()
                    append(content.trim())
                    appendLine()
                }
            }

            ExportFormat.MARKDOWN -> {
                buildString {
                    appendLine("# $displayTitle")
                    appendLine()
                    appendLine("> *Exported from MomoStack on $now*")
                    appendLine("> *$wordCount words · $charCount characters*")
                    appendLine()
                    appendLine("---")
                    appendLine()
                    append(content.trim())
                    appendLine()
                }
            }

            ExportFormat.DOC -> {
                buildWordDocHtml(displayTitle, content, now, wordCount, charCount)
            }

            ExportFormat.HTML -> {
                buildStandaloneHtml(displayTitle, content, now, wordCount, charCount)
            }
        }
    }

    /**
     * Builds standard Microsoft Word compatible HTML format.
     * When opened in Microsoft Word, Google Docs, WPS Office, or LibreOffice,
     * it renders with rich formatting, document margins, and clear typography.
     */
    private fun buildWordDocHtml(
        title: String,
        content: String,
        timestamp: String,
        wordCount: Int,
        charCount: Int
    ): String {
        val escapedTitle = escapeHtml(title)
        val bodyHtml = convertMarkdownToHtml(content)

        return """
            <html xmlns:o='urn:schemas-microsoft-com:office:office'
                  xmlns:w='urn:schemas-microsoft-com:office:word'
                  xmlns='http://www.w3.org/TR/REC-html40'>
            <head>
                <meta charset='utf-8'>
                <title>$escapedTitle</title>
                <!--[if gte mso 9]>
                <xml>
                    <w:WordDocument>
                        <w:View>Print</w:View>
                        <w:Zoom>100</w:Zoom>
                        <w:DoNotOptimizeForBrowser/>
                    </w:WordDocument>
                </xml>
                <![endif]-->
                <style>
                    @page {
                        size: 8.5in 11.0in;
                        margin: 1.0in 1.0in 1.0in 1.0in;
                    }
                    body {
                        font-family: 'Calibri', 'Segoe UI', Arial, sans-serif;
                        font-size: 11.5pt;
                        line-height: 1.6;
                        color: #1e293b;
                        background-color: #ffffff;
                    }
                    h1 {
                        font-family: 'Calibri Light', 'Segoe UI Semibold', Arial, sans-serif;
                        font-size: 22pt;
                        font-weight: bold;
                        color: #312e81;
                        margin-top: 0;
                        margin-bottom: 4pt;
                        border-bottom: 2pt solid #4f46e5;
                        padding-bottom: 6pt;
                    }
                    h2 {
                        font-size: 15pt;
                        font-weight: bold;
                        color: #4338ca;
                        margin-top: 16pt;
                        margin-bottom: 6pt;
                    }
                    h3 {
                        font-size: 12.5pt;
                        font-weight: bold;
                        color: #4f46e5;
                        margin-top: 12pt;
                        margin-bottom: 4pt;
                    }
                    p {
                        margin-top: 0;
                        margin-bottom: 9pt;
                    }
                    .metadata-card {
                        background-color: #f8fafc;
                        border: 1pt solid #e2e8f0;
                        border-left: 3pt solid #4f46e5;
                        padding: 8pt 12pt;
                        margin-bottom: 18pt;
                        font-size: 9.5pt;
                        color: #64748b;
                    }
                    blockquote {
                        margin: 10pt 0;
                        padding: 6pt 12pt;
                        border-left: 3pt solid #818cf8;
                        background-color: #f1f5f9;
                        color: #475569;
                        font-style: italic;
                    }
                    code {
                        font-family: 'Consolas', 'Courier New', monospace;
                        font-size: 10pt;
                        background-color: #f1f5f9;
                        color: #0f172a;
                        padding: 2pt 4pt;
                        border-radius: 3pt;
                    }
                    pre {
                        font-family: 'Consolas', 'Courier New', monospace;
                        font-size: 9.5pt;
                        background-color: #0f172a;
                        color: #f8fafc;
                        padding: 10pt 12pt;
                        border-radius: 4pt;
                        white-space: pre-wrap;
                        margin: 10pt 0;
                    }
                    ul, ol {
                        margin-top: 0;
                        margin-bottom: 10pt;
                        padding-left: 22pt;
                    }
                    li {
                        margin-bottom: 4pt;
                    }
                    hr {
                        border: none;
                        border-top: 1pt solid #cbd5e1;
                        margin: 18pt 0;
                    }
                    .footer-note {
                        margin-top: 30pt;
                        padding-top: 10pt;
                        border-top: 1pt solid #e2e8f0;
                        font-size: 9pt;
                        color: #94a3b8;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <h1>$escapedTitle</h1>
                <div class="metadata-card">
                    <strong>MomoStack Vault Document</strong> &bull; Exported: $timestamp &bull; $wordCount words &bull; $charCount characters
                </div>
                <div>
                    $bodyHtml
                </div>
                <div class="footer-note">
                    Created & Exported from MomoStack &bull; Offline-First Knowledge Vault
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds responsive standalone HTML document.
     */
    private fun buildStandaloneHtml(
        title: String,
        content: String,
        timestamp: String,
        wordCount: Int,
        charCount: Int
    ): String {
        val escapedTitle = escapeHtml(title)
        val bodyHtml = convertMarkdownToHtml(content)

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$escapedTitle</title>
                <style>
                    :root {
                        --bg: #090d16;
                        --card: #111827;
                        --border: #1f2937;
                        --primary: #6366f1;
                        --primary-glow: rgba(99, 102, 241, 0.2);
                        --text: #f9fafb;
                        --text-muted: #9ca3af;
                    }
                    @media (prefers-color-scheme: light) {
                        :root {
                            --bg: #f8fafc;
                            --card: #ffffff;
                            --border: #e2e8f0;
                            --primary: #4f46e5;
                            --primary-glow: rgba(79, 70, 229, 0.12);
                            --text: #0f172a;
                            --text-muted: #64748b;
                        }
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background-color: var(--bg);
                        color: var(--text);
                        margin: 0;
                        padding: 32px 16px;
                        line-height: 1.65;
                    }
                    .container {
                        max-width: 780px;
                        margin: 0 auto;
                        background: var(--card);
                        border: 1px solid var(--border);
                        border-radius: 16px;
                        padding: 36px 32px;
                        box-shadow: 0 10px 30px rgba(0,0,0,0.15);
                    }
                    h1 {
                        font-size: 2rem;
                        font-weight: 800;
                        margin-top: 0;
                        margin-bottom: 8px;
                        color: var(--text);
                    }
                    .badge {
                        display: inline-block;
                        padding: 4px 10px;
                        background: var(--primary-glow);
                        color: var(--primary);
                        border-radius: 9999px;
                        font-size: 0.8rem;
                        font-weight: 600;
                        margin-bottom: 16px;
                    }
                    .meta {
                        font-size: 0.875rem;
                        color: var(--text-muted);
                        padding-bottom: 16px;
                        border-bottom: 1px solid var(--border);
                        margin-bottom: 24px;
                    }
                    p { margin-top: 0; margin-bottom: 1rem; }
                    blockquote {
                        border-left: 3px solid var(--primary);
                        margin: 16px 0;
                        padding: 8px 16px;
                        background: var(--primary-glow);
                        border-radius: 0 8px 8px 0;
                        color: var(--text-muted);
                    }
                    code {
                        background: rgba(125, 125, 125, 0.15);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-family: 'JetBrains Mono', Consolas, monospace;
                        font-size: 0.9em;
                    }
                    pre {
                        background: #030712;
                        color: #f3f4f6;
                        padding: 16px;
                        border-radius: 10px;
                        overflow-x: auto;
                        font-family: 'JetBrains Mono', Consolas, monospace;
                        font-size: 0.875rem;
                    }
                    ul, ol { padding-left: 24px; margin-bottom: 1rem; }
                    li { margin-bottom: 4px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <span class="badge">MomoStack Note</span>
                    <h1>$escapedTitle</h1>
                    <div class="meta">Exported on $timestamp &bull; $wordCount words &bull; $charCount chars</div>
                    <div>$bodyHtml</div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Converts common Markdown syntax (Headings, bold, italic, code, quotes, lists) to clean HTML.
     */
    private fun convertMarkdownToHtml(markdown: String): String {
        val lines = markdown.lines()
        val result = StringBuilder()
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()
        var inList = false
        var inOrderedList = false

        for (line in lines) {
            val trimmed = line.trim()

            // Code block handling
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    result.append("<pre><code>")
                    result.append(escapeHtml(codeBlockContent.toString()))
                    result.append("</code></pre>\n")
                    codeBlockContent.clear()
                    inCodeBlock = false
                } else {
                    if (inList) { result.append("</ul>\n"); inList = false }
                    if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.appendLine(line)
                continue
            }

            // Empty line
            if (trimmed.isEmpty()) {
                if (inList) { result.append("</ul>\n"); inList = false }
                if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                continue
            }

            // Headings
            if (trimmed.startsWith("# ")) {
                if (inList) { result.append("</ul>\n"); inList = false }
                if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                result.append("<h1>").append(formatInlineMarkdown(trimmed.removePrefix("# ").trim())).append("</h1>\n")
                continue
            }
            if (trimmed.startsWith("## ")) {
                if (inList) { result.append("</ul>\n"); inList = false }
                if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                result.append("<h2>").append(formatInlineMarkdown(trimmed.removePrefix("## ").trim())).append("</h2>\n")
                continue
            }
            if (trimmed.startsWith("### ")) {
                if (inList) { result.append("</ul>\n"); inList = false }
                if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                result.append("<h3>").append(formatInlineMarkdown(trimmed.removePrefix("### ").trim())).append("</h3>\n")
                continue
            }

            // Horizontal Rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                if (inList) { result.append("</ul>\n"); inList = false }
                if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                result.append("<hr/>\n")
                continue
            }

            // Blockquote
            if (trimmed.startsWith("> ")) {
                if (inList) { result.append("</ul>\n"); inList = false }
                if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                result.append("<blockquote>").append(formatInlineMarkdown(trimmed.removePrefix("> ").trim())).append("</blockquote>\n")
                continue
            }

            // Unordered list items (- or *)
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) {
                    if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
                    result.append("<ul>\n")
                    inList = true
                }
                val itemText = trimmed.substring(2).trim()
                result.append("  <li>").append(formatInlineMarkdown(itemText)).append("</li>\n")
                continue
            }

            // Ordered list items (1. 2. etc.)
            val orderedMatch = Regex("^\\d+\\.\\s+(.*)").find(trimmed)
            if (orderedMatch != null) {
                if (!inOrderedList) {
                    if (inList) { result.append("</ul>\n"); inList = false }
                    result.append("<ol>\n")
                    inOrderedList = true
                }
                val itemText = orderedMatch.groupValues[1].trim()
                result.append("  <li>").append(formatInlineMarkdown(itemText)).append("</li>\n")
                continue
            }

            // Regular paragraph
            if (inList) { result.append("</ul>\n"); inList = false }
            if (inOrderedList) { result.append("</ol>\n"); inOrderedList = false }
            result.append("<p>").append(formatInlineMarkdown(trimmed)).append("</p>\n")
        }

        if (inList) result.append("</ul>\n")
        if (inOrderedList) result.append("</ol>\n")
        if (inCodeBlock) {
            result.append("<pre><code>")
            result.append(escapeHtml(codeBlockContent.toString()))
            result.append("</code></pre>\n")
        }

        return result.toString()
    }

    private fun formatInlineMarkdown(text: String): String {
        var str = escapeHtml(text)

        // Bold (**text** or __text__)
        str = str.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        str = str.replace(Regex("__(.+?)__"), "<strong>$1</strong>")

        // Italic (*text* or _text_)
        str = str.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "<em>$1</em>")
        str = str.replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)"), "<em>$1</em>")

        // Inline code (`code`)
        str = str.replace(Regex("`(.+?)`"), "<code>$1</code>")

        // Links [text](url)
        str = str.replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "<a href=\"$2\" target=\"_blank\">$1</a>")

        return str
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
