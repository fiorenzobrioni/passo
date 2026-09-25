package com.callbackdev.passo.widget.words

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The forms and budgets of «In words», at the household's reference grants. */
class WordsLayoutTest {
    private val fiveDigits = 3.07f
    private val oneByOne = DpSize(85.dp, 85.dp)
    private val twoByOne = DpSize(159.dp, 85.dp)
    private val threeByOne = DpSize(250.dp, 85.dp)
    private val fourByOne = DpSize(340.dp, 85.dp)
    private val twoByTwo = DpSize(159.dp, 189.dp)
    private val fourByTwo = DpSize(340.dp, 189.dp)
    private val fourByThree = DpSize(340.dp, 293.dp)

    @Test
    fun `every grant has its form`() {
        assertThat(wordsForm(oneByOne)).isEqualTo(WordsForm.LINE)
        assertThat(wordsForm(twoByOne)).isEqualTo(WordsForm.LINE)
        assertThat(wordsForm(threeByOne)).isEqualTo(WordsForm.ROW)
        assertThat(wordsForm(fourByOne)).isEqualTo(WordsForm.ROW)
        assertThat(wordsForm(twoByTwo)).isEqualTo(WordsForm.STACK)
        assertThat(wordsForm(DpSize(250.dp, 189.dp))).isEqualTo(WordsForm.STACK)
        assertThat(wordsForm(fourByTwo)).isEqualTo(WordsForm.PANEL)
    }

    @Test
    fun `a sentence is never cut to make room for a fact`() {
        val three = rowColumnPlan(fourByOne, 1f, sentenceNeeds = 3, goal = true, metrics = true)
        assertThat(three.sentenceLines).isEqualTo(3)
        assertThat(three.goal).isFalse()
        val two = rowColumnPlan(fourByOne, 1f, sentenceNeeds = 2, goal = true, metrics = true)
        assertThat(two.sentenceLines).isEqualTo(2)
        assertThat(two.goal).isTrue()
        assertThat(two.metrics).isFalse()
        val one = rowColumnPlan(fourByOne, 1f, sentenceNeeds = 1, goal = true, metrics = true)
        assertThat(one.sentenceLines).isEqualTo(1)
        assertThat(one.goal).isTrue()
        assertThat(one.metrics).isTrue()
    }

    @Test
    fun `without a sentence the facts have the column`() {
        val plan = rowColumnPlan(fourByOne, 1f, sentenceNeeds = 0, goal = true, metrics = true)
        assertThat(plan.sentenceLines).isEqualTo(0)
        assertThat(plan.goal).isTrue()
        assertThat(plan.metrics).isTrue()
    }

    @Test
    fun `the leading column holds the number and leaves the sentence its minimum`() {
        val leading = rowLeading(threeByOne, 1f, fiveDigits, eyebrow = 80.dp)
        assertThat(rowSentenceColumn(threeByOne, leading)).isAtLeast(SentenceColumnMin)
        val hero = rowHeroSp(threeByOne, 1f, fiveDigits, leading)
        assertThat(hero * fiveDigits).isAtMost(leading.value)
        assertThat(
            rowHeroSp(fourByOne, 1f, fiveDigits, rowLeading(fourByOne, 1f, fiveDigits, 80.dp)),
        ).isAtMost(ROW_HERO_MAX)
    }

    @Test
    fun `the one-cell card keeps its goal only where it costs the number nothing`() {
        assertThat(linePlan(oneByOne, 1f, fiveDigits, goal = true, status = false).goal).isTrue()
        assertThat(linePlan(twoByOne, 1f, fiveDigits, goal = true, status = false).goal).isFalse()
        val one = linePlan(oneByOne, 1f, fiveDigits, goal = true, status = false)
        assertThat(one.heroSp * fiveDigits).isAtMost((85f - 28f))
    }

    @Test
    fun `a narrow tall card reserves its title first and grows it with what is left`() {
        val plan = stackPlan(twoByTwo, 1f, fiveDigits, sentenceNeeds = 2, goal = true, metrics = true, details = true)
        assertThat(plan.column.sentenceLines).isEqualTo(2)
        assertThat(plan.column.goal).isTrue()
        assertThat(plan.heroSp).isAtLeast(STACK_HERO_FLOOR)
        assertThat(plan.heroSp * fiveDigits).isAtMost(159f - 28f)
        assertThat(plan.detailRows).isEqualTo(0)
    }

    @Test
    fun `with height to spare the day in figures replaces the metrics line`() {
        val plan =
            stackPlan(
                DpSize(250.dp, 400.dp),
                1f,
                fiveDigits,
                sentenceNeeds = 2,
                goal = true,
                metrics = true,
                details = true,
            )
        assertThat(plan.detailRows).isEqualTo(DETAIL_MAX_ROWS)
        assertThat(plan.column.metrics).isFalse()
        val off =
            stackPlan(
                DpSize(250.dp, 400.dp),
                1f,
                fiveDigits,
                sentenceNeeds = 2,
                goal = true,
                metrics = true,
                details = false,
            )
        assertThat(off.detailRows).isEqualTo(0)
        assertThat(off.column.metrics).isTrue()
    }

    @Test
    fun `the panel's number stands beside the words and leaves them their column`() {
        val plan = panelPlan(fourByTwo, 1f, fiveDigits, sentenceNeeds = 2, goal = true, metrics = false, details = true)
        assertThat(plan.heroSp).isAtMost(PANEL_HERO_MAX)
        assertThat(340.dp - 28.dp - ColumnGap - plan.leading).isAtLeast(PanelSentenceMin - 4.dp)
        assertThat(plan.detailRows).isEqualTo(0)
        val tall =
            panelPlan(fourByThree, 1f, fiveDigits, sentenceNeeds = 2, goal = true, metrics = false, details = true)
        assertThat(tall.detailRows).isAtLeast(DETAIL_MIN_ROWS)
    }

    @Test
    fun `the words' last line sits on the number's baseline`() {
        assertThat(baselineLift(56f, 16f, 1f).value).isWithin(0.01f).of(40f * DESCENT_EM)
        assertThat(baselineLift(12f, 16f, 1f)).isEqualTo(0.dp)
    }

    @Test
    fun `the table holds two lines at least and four at most`() {
        assertThat(detailRows(40.dp, 1f)).isEqualTo(0)
        assertThat(detailRows(60.dp, 1f)).isEqualTo(2)
        assertThat(detailRows(400.dp, 1f)).isEqualTo(DETAIL_MAX_ROWS)
        assertThat(detailHeight(0, 1f)).isEqualTo(0.dp)
    }
}
