package com.callbackdev.passo.widget.glance

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The forms and budgets of «At a glance», at the household's reference grants. */
class GlanceLayoutTest {
    private val oneByOne = DpSize(85.dp, 85.dp)
    private val twoByOne = DpSize(159.dp, 85.dp)
    private val threeByOne = DpSize(250.dp, 85.dp)
    private val fourByOne = DpSize(340.dp, 85.dp)
    private val twoByTwo = DpSize(159.dp, 189.dp)
    private val threeByTwo = DpSize(250.dp, 189.dp)
    private val fourByTwo = DpSize(340.dp, 189.dp)
    private val oneByTwo = DpSize(85.dp, 189.dp)

    /** «12,345» in Roboto Bold: five digits and a separator. */
    private val fiveDigits = 3.07f

    @Test
    fun `every grant has its form`() {
        assertThat(glanceForm(oneByOne, showHours = true)).isEqualTo(GlanceForm.DOT)
        assertThat(glanceForm(oneByTwo, showHours = true)).isEqualTo(GlanceForm.DOT)
        assertThat(glanceForm(twoByOne, showHours = true)).isEqualTo(GlanceForm.NARROW)
        assertThat(glanceForm(threeByOne, showHours = true)).isEqualTo(GlanceForm.NARROW)
        assertThat(glanceForm(fourByOne, showHours = true)).isEqualTo(GlanceForm.WIDE)
        assertThat(glanceForm(twoByTwo, showHours = true)).isEqualTo(GlanceForm.TALL)
        assertThat(glanceForm(threeByTwo, showHours = true)).isEqualTo(GlanceForm.PANEL)
        assertThat(glanceForm(fourByTwo, showHours = true)).isEqualTo(GlanceForm.PANEL)
    }

    @Test
    fun `without the hours a tall card keeps the tall form`() {
        assertThat(glanceForm(fourByTwo, showHours = false)).isEqualTo(GlanceForm.TALL)
    }

    @Test
    fun `a large font scale leaves no room for bars on a short card and it stays tall`() {
        val short = DpSize(340.dp, 150.dp)
        assertThat(glanceForm(short, showHours = true, fontScale = 1.3f)).isEqualTo(GlanceForm.TALL)
    }

    @Test
    fun `the row's ring fills the height up to its ceiling, and yields to the words on a narrow card`() {
        assertThat(rowRingSize(fourByOne)).isEqualTo(RowRingMax)
        assertThat(rowRingSize(twoByOne)).isEqualTo(RowRingMin)
        assertThat(rowRingSize(DpSize(340.dp, 60.dp))).isEqualTo(48.dp)
    }

    @Test
    fun `the row's count is about Chiaro's 34 sp where it fits, smaller where the column cannot hold it`() {
        // The reference 85 dp row pays the ink balance under the words out of the number: 33 sp.
        assertThat(rowHeroSp(85.dp, 150.dp, fiveDigits, 1f, footnote = false)).isEqualTo(33f)
        assertThat(rowHeroSp(101.dp, 150.dp, fiveDigits, 1f, footnote = false)).isEqualTo(ROW_HERO_SP)
        val narrow = rowHeroSp(85.dp, rowWordsWidth(159.dp, RowRingMin), fiveDigits, 1f, footnote = false)
        assertThat(narrow).isLessThan(ROW_HERO_SP)
        assertThat(narrow * fiveDigits).isAtMost(rowWordsWidth(159.dp, RowRingMin).value)
    }

    @Test
    fun `a footnote is paid for out of the number, not out of the card`() {
        val with = rowHeroSp(85.dp, 200.dp, fiveDigits, 1f, footnote = true)
        val without = rowHeroSp(85.dp, 200.dp, fiveDigits, 1f, footnote = false)
        assertThat(with).isLessThan(without)
        // number + goal + footnote fit the row's budget
        val used = with * 1.32f + 16f * 1.32f + 12f * 1.32f
        assertThat(used).isAtMost(85f - 12f)
    }

    @Test
    fun `the words take what they measured, never the sentence's share`() {
        val slack = rowWordsWidth(340.dp, RowRingMax) - SentenceGap
        val words = rowWordsColumn(340.dp, RowRingMax, wordsNeed = 130.dp, sentenceKeep = 200.dp)
        assertThat(words).isEqualTo(slack / 2)
        val roomy = rowWordsColumn(340.dp, RowRingMax, wordsNeed = 130.dp, sentenceKeep = 60.dp)
        assertThat(roomy).isEqualTo(130.dp)
    }

    @Test
    fun `the sentence beside a row takes what it measured, up to three lines`() {
        assertThat(rowSentenceLines(85.dp, 1f, measured = 2)).isEqualTo(2)
        assertThat(rowSentenceLines(85.dp, 1f, measured = 5)).isEqualTo(ROW_SENTENCE_MAX_LINES)
        assertThat(rowSentenceLines(60.dp, 1f, measured = 3)).isEqualTo(2)
    }

    @Test
    fun `the tall card gives the ring the rest, and the sentence yields before the ring does`() {
        val plan = tallPlan(twoByTwo, 1f, fiveDigits, sentenceLines = 2)
        assertThat(plan.sentenceLines).isEqualTo(2)
        assertThat(plan.ring).isAtLeast(TallRingMin)
        assertThat(plan.heroSp).isEqualTo(ROW_HERO_SP)
        val squeezed = tallPlan(DpSize(159.dp, 150.dp), 1.3f, fiveDigits, sentenceLines = 2)
        assertThat(squeezed.sentenceLines).isLessThan(2)
        assertThat(squeezed.ring).isAtLeast(TallRingMin)
    }

    @Test
    fun `past the ring's ceiling the number grows`() {
        val plan = tallPlan(DpSize(250.dp, 400.dp), 1f, fiveDigits, sentenceLines = 1)
        assertThat(plan.ring).isEqualTo(TallRingMax)
        assertThat(plan.heroSp).isGreaterThan(ROW_HERO_SP)
        assertThat(plan.heroSp).isAtMost(TALL_HERO_MAX)
    }

    @Test
    fun `the panel's bars get the rest of the card, capped`() {
        val bars = panelBarsHeight(fourByTwo, 1f)
        assertThat(bars).isAtLeast(BarsMinHeight)
        assertThat(bars.value).isWithin(0.5f).of(58.3f)
        assertThat(panelBarsHeight(DpSize(340.dp, 500.dp), 1f)).isEqualTo(BarsMaxHeight)
    }

    @Test
    fun `a figure inside the ring fits its hole or is not drawn`() {
        val sp = insideSp(56.dp, 1.65f, 1f, RING_PERCENT_MAX_SP)
        assertThat(sp).isNotNull()
        assertThat(checkNotNull(sp) * 1.65f).isAtMost(ringInner(56.dp).value)
        assertThat(insideSp(20.dp, 1.65f, 1f, RING_PERCENT_MAX_SP)).isNull()
    }

    @Test
    fun `the one-cell ring takes the cell`() {
        assertThat(dotRingSize(oneByOne)).isEqualTo(85.dp - DotPadding * 2)
        assertThat(dotRingSize(oneByTwo)).isEqualTo(85.dp - DotPadding * 2)
    }
}
