package com.callbackdev.passo.widget

import com.callbackdev.passo.widget.glance.BARS_LABEL_SP
import com.callbackdev.passo.widget.glance.ROW_HERO_SP
import com.callbackdev.passo.widget.glance.SENTENCE_LINE_SP
import com.callbackdev.passo.widget.words.DETAIL_LABEL_SP
import com.callbackdev.passo.widget.words.WORDS_SENTENCE_SP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The widgets' type scale: every fixed size a card prints in is a step of [WidgetType]. */
class WidgetTypeTest {
    private val everySize = mapOf(
        "fact" to FACT_SP,
        "status footnote" to STATUS_SP,
        "one-row count" to ROW_HERO_SP,
        "sentence line" to SENTENCE_LINE_SP,
        "chart label" to BARS_LABEL_SP,
        "detail label" to DETAIL_LABEL_SP,
    )

    @Test
    fun `every fixed widget size is a step of the scale`() {
        everySize.forEach { (role, sp) ->
            assertTrue("$role is $sp sp, not a step of the scale", sp in WidgetType.steps)
        }
    }

    @Test
    fun `the words card's sentence is on the scale too`() {
        assertTrue(WORDS_SENTENCE_SP in WidgetType.steps)
        assertEquals(WidgetType.BODY, WORDS_SENTENCE_SP)
    }
}
