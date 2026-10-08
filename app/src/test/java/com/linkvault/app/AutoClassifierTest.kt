package com.linkvault.app

import com.linkvault.app.classifier.AutoClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoClassifierTest {

    @Test
    fun testDomainBasedClassification() {
        assertEquals("Development", AutoClassifier.classify("github.com", null, null))
        assertEquals("Development", AutoClassifier.classify("stackoverflow.com", null, null))
        assertEquals("Entertainment", AutoClassifier.classify("youtube.com", null, null))
        assertEquals("Entertainment", AutoClassifier.classify("spotify.com", null, null))
        assertEquals("Shopping", AutoClassifier.classify("amazon.com", null, null))
        assertEquals("Finance", AutoClassifier.classify("bloomberg.com", null, null))
        assertEquals("Work", AutoClassifier.classify("notion.so", null, null))
    }

    @Test
    fun testSubdomainClassification() {
        assertEquals("Development", AutoClassifier.classify("api.github.com", null, null))
        assertEquals("Reading", AutoClassifier.classify("blog.medium.com", null, null))
    }

    @Test
    fun testKeywordBasedClassification() {
        val result = AutoClassifier.classify(
            domain = "randomblog.org",
            title = "Introduction to Kotlin and Python compiler architecture",
            description = "A deep dive tutorial into programming language design."
        )
        assertEquals("Development", result)
    }

    @Test
    fun testUncategorizedFallback() {
        val result = AutoClassifier.classify(
            domain = "someunrelateddomain.xyz",
            title = "Personal Diary Entry",
            description = "Today was a nice sunny afternoon."
        )
        assertEquals("Uncategorized", result)
    }
}
