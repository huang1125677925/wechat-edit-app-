package com.wechat.editor

import com.wechat.editor.viewmodel.AiNewsDigestLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiNewsDigestLimitsTest {

    @Test
    fun modelInputBounds_empty_returnsNull() {
        assertNull(AiNewsDigestLimits.modelInputBounds(0))
    }

    @Test
    fun modelInputBounds_smallFeed_usesActualCount() {
        assertEquals(1..5, AiNewsDigestLimits.modelInputBounds(5))
    }

    @Test
    fun modelInputBounds_largeFeed_startsAtTwenty() {
        assertEquals(20..120, AiNewsDigestLimits.modelInputBounds(120))
    }

    @Test
    fun coerceMaxItemsForModel_clampsToSendable() {
        assertEquals(50, AiNewsDigestLimits.coerceMaxItemsForModel(800, 50))
        assertEquals(80, AiNewsDigestLimits.coerceMaxItemsForModel(80, 200))
    }
}
