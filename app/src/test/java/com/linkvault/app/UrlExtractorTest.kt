package com.linkvault.app

import com.linkvault.app.util.UrlExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun testCleanUrlExtraction() {
        val input = "Check out this library https://github.com/google/jetpack-compose it's really cool"
        val result = UrlExtractor.extract(input)

        assertEquals("https://github.com/google/jetpack-compose", result.url)
        assertEquals("github.com", result.domain)
        assertEquals("Check out this library  it's really cool", result.accompanyingText)
    }

    @Test
    fun testTrackingParameterStripping() {
        val input = "https://example.com/product?utm_source=twitter&utm_medium=social&id=123"
        val normalized = UrlExtractor.normalizeUrl(input)

        assertEquals("https://example.com/product?id=123", normalized)
    }

    @Test
    fun testPlainTextWithoutUrl() {
        val input = "Remember to buy milk and eggs tomorrow"
        val result = UrlExtractor.extract(input)

        assertNull(result.url)
        assertNull(result.domain)
        assertEquals("Remember to buy milk and eggs tomorrow", result.accompanyingText)
    }

    @Test
    fun testDomainExtractionWithWww() {
        val domain = UrlExtractor.extractDomain("https://www.theverge.com/tech/article")
        assertEquals("theverge.com", domain)
    }
}
