package com.wechat.editor

import com.wechat.editor.utils.AiNewsAggregatorApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiNewsAggregatorApiTest {

    @Test
    fun feedUrlsFor24h_includesThreeMirrors() {
        val urls = AiNewsAggregatorApi.feedUrlsForWindow(24)
        assertNotNull(urls)
        assertEquals(3, urls!!.size)
        assertTrue(urls.all { it.endsWith("latest-24h.json") })
        assertTrue(urls.any { it.contains("suyxh.github.io") })
        assertTrue(urls.any { it.contains("raw.githubusercontent.com") })
        assertTrue(urls.any { it.contains("cdn.jsdelivr.net") })
    }

    @Test
    fun feedUrlsFor7d_usesSevenDayFile() {
        val urls = AiNewsAggregatorApi.feedUrlsForWindow(168)
        assertNotNull(urls)
        assertTrue(urls!!.all { it.endsWith("latest-7d.json") })
    }

    @Test
    fun feedUrlsForUnsupportedWindow_returnsNull() {
        assertEquals(null, AiNewsAggregatorApi.feedUrlsForWindow(48))
    }
}
