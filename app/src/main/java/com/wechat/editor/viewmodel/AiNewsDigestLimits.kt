package com.wechat.editor.viewmodel

/**
 * Bounds for how many scraped headlines are sent to the digest model.
 * Upper limit follows the current selection (items with a non-blank URL).
 */
internal object AiNewsDigestLimits {

    const val DEFAULT_MAX_ITEMS_FOR_MODEL = 80

    /** Minimum selectable count when enough headlines exist. */
    private const val PREFERRED_MIN_ITEMS = 20

    fun sendableCount(items: List<com.wechat.editor.model.AiNewsItem>): Int =
        items.count { it.url.isNotBlank() }

    /**
     * Inclusive range for the model-input slider, or null when nothing can be sent.
     */
    fun modelInputBounds(sendableItemCount: Int): IntRange? {
        if (sendableItemCount <= 0) return null
        val min = if (sendableItemCount >= PREFERRED_MIN_ITEMS) PREFERRED_MIN_ITEMS else 1
        return min..sendableItemCount
    }

    fun coerceMaxItemsForModel(requested: Int, sendableItemCount: Int): Int {
        val bounds = modelInputBounds(sendableItemCount) ?: return requested.coerceAtLeast(1)
        return requested.coerceIn(bounds)
    }

    fun sliderSteps(bounds: IntRange): Int {
        val span = bounds.last - bounds.first
        if (span <= 1) return 0
        return (span / 5).coerceAtMost(200)
    }
}
