package com.momostack.app

import com.momostack.app.util.RichTextFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextFormatterTest {

    @Test
    fun testChromeStyledSpansConvertCorrectly() {
        // Chrome copy output with font-weight: 700 / bold and font-style: italic
        val chromeHtml = """
            <meta charset='utf-8'>
            <span style="color: rgb(32, 33, 36); font-family: Roboto; font-weight: 700;">Chrome Bold Title</span>
            <span style="font-style: italic;">Chrome Italic Text</span>
        """.trimIndent()

        val markdown = RichTextFormatter.htmlToMarkdown(chromeHtml)
        assertTrue(markdown.contains("**Chrome Bold Title**"))
        assertTrue(markdown.contains("*Chrome Italic Text*"))
    }

    @Test
    fun testStandardHtmlTagsConvertCorrectly() {
        val html = """
            <h1>Main Title</h1>
            <p>This is <b>bold text</b> and <i>italic words</i>.</p>
            <ul>
                <li>First bullet</li>
                <li>Second bullet</li>
            </ul>
            <p>Check <a href="https://example.com">Example Site</a></p>
        """.trimIndent()

        val markdown = RichTextFormatter.htmlToMarkdown(html)
        assertTrue(markdown.contains("# Main Title"))
        assertTrue(markdown.contains("**bold text**"))
        assertTrue(markdown.contains("*italic words*"))
        assertTrue(markdown.contains("- First bullet"))
        assertTrue(markdown.contains("- Second bullet"))
        assertTrue(markdown.contains("[Example Site](https://example.com)"))
    }

    @Test
    fun testNestedTagsAndEntities() {
        val html = "<p><strong><em>Important&nbsp;&amp;&nbsp;Cozy</em></strong></p>"
        val markdown = RichTextFormatter.htmlToMarkdown(html)
        assertTrue(markdown.contains("**") && markdown.contains("*Important & Cozy*"))
    }
}
