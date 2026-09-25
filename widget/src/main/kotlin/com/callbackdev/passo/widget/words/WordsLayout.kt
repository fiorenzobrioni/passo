package com.callbackdev.passo.widget.words

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.widget.FACT_SP
import com.callbackdev.passo.widget.STATUS_SP
import com.callbackdev.passo.widget.WidgetCardPadding
import com.callbackdev.passo.widget.WidgetCardPaddingSnug
import com.callbackdev.passo.widget.quarterPoint
import com.callbackdev.passo.widget.textLineHeight
import com.callbackdev.passo.widget.textSizeForLine

/*
 * «In words» (Chiaro's «In parole»): the day with nothing drawn on it but two small marks, and
 * its whole hierarchy built out of type, in Chiaro's four ranks, each differing by size AND
 * weight AND ink:
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | today's steps | scaled to the grant | Bold | strong |
 * | 2 | the day's sentence | [WORDS_SENTENCE_SP] | Medium | strong |
 * | 3 | «Steps today», the goal, distance and calories, the day in figures | 16 (14 for a table's words) | Regular / Medium figures | quiet / strong |
 * | 2 | or, when the count is not moving, what a tap does about it | [WORDS_SENTENCE_SP] | Medium | attention |
 * | 4 | the same, on the one-cell card, as a footnote | 12 | Medium | attention |
 *
 * The number is sized by the grant, the way «At a glance» sizes its ring. Four forms, Chiaro's:
 *
 * - [WordsForm.LINE], one row too narrow for two columns: the eyebrow, the number, the goal.
 * - [WordsForm.ROW], one row with a second column: the number on the leading side, the
 *   sentence and the facts against the far edge (the reference 4×1, and three cells too).
 * - [WordsForm.STACK], two rows and up, narrow: the eyebrow at the top, everything else at the
 *   bottom, the air between them.
 * - [WordsForm.PANEL], two rows and up, four cells wide: the number beside the words, both on
 *   the bottom edge, their last lines on one baseline.
 *
 * On a card tall enough, what is left buys «the day in figures» ([detailRows]): the quantities
 * Today shows in tiles, one per line, the figure first and its words after it.
 */
internal enum class WordsForm { LINE, ROW, STACK, PANEL }

internal fun wordsForm(size: DpSize): WordsForm = when {
    size.height >= TallMinHeight && size.width >= PanelMinWidth -> WordsForm.PANEL
    size.height >= TallMinHeight -> WordsForm.STACK
    size.width >= RowMinWidth -> WordsForm.ROW
    else -> WordsForm.LINE
}

internal val TallMinHeight = 150.dp

/** Four cells on every grid measured (~340 dp, ~320 on a five-column grid); three are ~250. */
internal val PanelMinWidth = 300.dp

/** Where a row has a leading column at its minimum and a sentence column at its own. */
internal val RowMinWidth: Dp get() = WidgetCardPadding * 2 + ColumnGap + LeadingMin + SentenceColumnMin

internal val ColumnGap = 12.dp

/** A count at the hero's floor and «Steps today» beside nothing: the leading column's least. */
internal val LeadingMin = 96.dp

/** About eleven characters of 18 sp: the narrowest column worth a sentence (Chiaro's 104). */
internal val SentenceColumnMin = 104.dp

internal const val WORDS_SENTENCE_SP = 18f
internal const val DETAIL_LABEL_SP = 14f

// --- One row ---------------------------------------------------------------------------------

/**
 * The one-row card's leading column: what its words measured (the eyebrow, the number at the
 * size the height allows), never under [LeadingMin] and never out of the sentence's minimum.
 */
internal fun rowLeading(size: DpSize, fontScale: Float, heroEm: Float, eyebrow: Dp): Dp {
    val words = size.width - WidgetCardPadding * 2 - ColumnGap
    val byHeight = rowHeroByHeight(size.height, fontScale)
    val need = maxOf(eyebrow, (heroEm * byHeight * fontScale).dp) + 4.dp
    return need.coerceIn(LeadingMin, (words - SentenceColumnMin).coerceAtLeast(LeadingMin))
}

internal fun rowSentenceColumn(size: DpSize, leading: Dp): Dp = size.width - WidgetCardPadding * 2 - ColumnGap - leading

private fun rowHeroByHeight(height: Dp, fontScale: Float): Float =
    textSizeForLine(height - WidgetCardPaddingSnug * 2 - textLineHeight(FACT_SP, fontScale), fontScale)

/**
 * The number on a one-row card: what the height leaves under the eyebrow, what the column holds, between [HERO_FLOOR_SP] and [ROW_HERO_MAX]. The cap is
 * Chiaro's reason: the number never outgrows the block of words beside it.
 */
internal fun rowHeroSp(size: DpSize, fontScale: Float, heroEm: Float, column: Dp): Float {
    val byWidth = column.value / (heroEm * fontScale.coerceAtLeast(0.1f))
    return quarterPoint(
        minOf(rowHeroByHeight(size.height, fontScale), byWidth, ROW_HERO_MAX).coerceAtLeast(HERO_FLOOR_SP),
    )
}

internal const val ROW_HERO_MAX = 44f
internal const val HERO_FLOOR_SP = 26f

/** What a column of words holds, in the order the hierarchy spends. */
internal data class ColumnPlan(val sentenceLines: Int, val goal: Boolean, val metrics: Boolean, val left: Dp)

/**
 * Fills a column of [room] in the order the hierarchy spends: the sentence first, every line it
 * measured ([sentenceNeeds], up to [maxLines]), because a sentence cut off mid-phrase says less
 * than no sentence; then the goal; then the metrics. A fact that does not fit is not drawn.
 */
internal fun fillColumn(
    room: Dp,
    fontScale: Float,
    sentenceNeeds: Int,
    goal: Boolean,
    metrics: Boolean,
    maxLines: Int,
): ColumnPlan {
    val line = textLineHeight(WORDS_SENTENCE_SP, fontScale)
    val fact = textLineHeight(FACT_SP, fontScale)
    var left = room
    var lines = 0
    while (lines < minOf(sentenceNeeds, maxLines) && left >= line) {
        left -= line
        lines++
    }
    val showGoal = goal && left >= fact
    if (showGoal) left -= fact
    val showMetrics = metrics && left >= fact
    if (showMetrics) left -= fact
    return ColumnPlan(lines, showGoal, showMetrics, left)
}

/** The trailing column of a one-row card: up to three lines of sentence, then the facts. */
internal fun rowColumnPlan(
    size: DpSize,
    fontScale: Float,
    sentenceNeeds: Int,
    goal: Boolean,
    metrics: Boolean,
): ColumnPlan = fillColumn(
    room = size.height - WidgetCardPaddingSnug * 2,
    fontScale = fontScale,
    sentenceNeeds = sentenceNeeds,
    goal = goal,
    metrics = metrics,
    maxLines = ROW_SENTENCE_MAX_LINES,
)

internal const val ROW_SENTENCE_MAX_LINES = 3
internal const val TALL_SENTENCE_MAX_LINES = 2

/**
 * The narrow one-row card: the number sized to the cell, and the goal under it only where it
 * costs the number nothing, or the number is still a hero with it (at least [LINE_HERO_COMFORT]).
 */
internal data class LinePlan(val heroSp: Float, val goal: Boolean)

internal fun linePlan(size: DpSize, fontScale: Float, heroEm: Float, goal: Boolean, status: Boolean): LinePlan {
    val width = size.width - WidgetCardPadding * 2
    val room = size.height - WidgetCardPaddingSnug * 2 - textLineHeight(FACT_SP, fontScale) -
        (if (status) textLineHeight(STATUS_SP, fontScale) else 0.dp)
    val byWidth = width.value / (heroEm * fontScale.coerceAtLeast(0.1f))
    fun hero(height: Dp) =
        minOf(textSizeForLine(height, fontScale), byWidth, ROW_HERO_MAX).coerceAtLeast(LINE_HERO_FLOOR)
    val without = hero(room)
    val with = hero(room - textLineHeight(FACT_SP, fontScale))
    val showGoal = goal && (with >= without || with >= LINE_HERO_COMFORT)
    return LinePlan(quarterPoint(if (showGoal) with else without), showGoal)
}

internal const val LINE_HERO_FLOOR = 16f
internal const val LINE_HERO_COMFORT = 30f

// --- Tall --------------------------------------------------------------------------------------

/**
 * A tall narrow card, in the order it buys its lines: the number at [STACK_HERO_FLOOR] first
 * (on a tall card it is the page's title), then the sentence, the goal and the metrics; what is
 * left grows the number up to [STACK_HERO_MAX]; what the number cannot use buys the day in
 * figures. Where the figures are drawn, the metrics line would repeat two of them, so it goes.
 */
internal data class StackPlan(val heroSp: Float, val column: ColumnPlan, val detailRows: Int)

internal fun stackPlan(
    size: DpSize,
    fontScale: Float,
    heroEm: Float,
    sentenceNeeds: Int,
    goal: Boolean,
    metrics: Boolean,
    details: Boolean,
): StackPlan {
    val floor = textLineHeight(STACK_HERO_FLOOR, fontScale)
    val room = size.height - WidgetCardPadding * 2 - textLineHeight(FACT_SP, fontScale) - floor
    val column = fillColumn(room, fontScale, sentenceNeeds, goal, metrics, maxLines = TALL_SENTENCE_MAX_LINES)
    val heroRoom = floor + column.left.coerceAtLeast(0.dp)
    val byWidth = (size.width - WidgetCardPadding * 2).value / (heroEm * fontScale.coerceAtLeast(0.1f))
    val hero =
        quarterPoint(minOf(textSizeForLine(heroRoom, fontScale), byWidth, STACK_HERO_MAX).coerceAtLeast(HERO_FLOOR_SP))
    val spare = heroRoom - textLineHeight(hero, fontScale)
    return withDetails(StackPlan(hero, column, 0), spare, fontScale, details, column.metrics) { plan, rows, freed ->
        plan.copy(column = plan.column.copy(metrics = !freed && plan.column.metrics), detailRows = rows)
    }
}

internal const val STACK_HERO_FLOOR = 40f
internal const val STACK_HERO_MAX = 56f

/**
 * The wide tall card: the eyebrow across the top, and along the bottom the number on the
 * leading side with the words right-aligned beside it. The two columns are budgeted separately
 * out of the same height, so neither can overflow; the air left over between the eyebrow and
 * the block ([PanelAir] kept for the composition) buys the day in figures.
 */
internal data class PanelPlan(
    val heroSp: Float,
    val leading: Dp,
    val column: ColumnPlan,
    val detailRows: Int,
    val baselineLift: Dp,
)

internal fun panelPlan(
    size: DpSize,
    fontScale: Float,
    heroEm: Float,
    sentenceNeeds: Int,
    goal: Boolean,
    metrics: Boolean,
    details: Boolean,
    descentEm: Float = DESCENT_EM,
): PanelPlan {
    val top = size.height - WidgetCardPadding * 2 - textLineHeight(FACT_SP, fontScale)
    val maxLeading = size.width - WidgetCardPadding * 2 - ColumnGap - PanelSentenceMin
    val byWidth = maxLeading.value / (heroEm * fontScale.coerceAtLeast(0.1f))
    val hero =
        quarterPoint(minOf(textSizeForLine(top, fontScale), byWidth, PANEL_HERO_MAX).coerceAtLeast(HERO_FLOOR_SP))
    val leading = (heroEm * hero * fontScale).dp + 4.dp
    val column = fillColumn(top, fontScale, sentenceNeeds, goal, metrics, maxLines = TALL_SENTENCE_MAX_LINES)
    val trailingLast = when {
        column.metrics || column.goal -> FACT_SP
        column.sentenceLines > 0 -> WORDS_SENTENCE_SP
        else -> null
    }
    val lift = trailingLast?.let {
        baselineLift(hero, it, fontScale, descentEm).coerceAtMost(column.left.coerceAtLeast(0.dp))
    } ?: 0.dp
    val block = maxOf(textLineHeight(hero, fontScale), top - column.left + lift)
    val spare = top - block - PanelAir
    return withDetails(PanelPlan(hero, leading, column, 0, lift), spare, fontScale, details, column.metrics) {
            plan,
            rows,
            freed,
        ->
        plan.copy(column = plan.column.copy(metrics = !freed && plan.column.metrics), detailRows = rows)
    }
}

/**
 * The number and the words beside it on one baseline (Chiaro's `textPanelBaselineLift`): a line
 * box keeps the font's descent under its baseline in proportion to its size, so the big number
 * stands higher than a 16 sp line bottom-aligned with it. The difference, under the words, lands
 * their last line on the number's baseline.
 */
internal fun baselineLift(leadingSp: Float, trailingSp: Float, fontScale: Float, descentEm: Float = DESCENT_EM): Dp =
    ((leadingSp - trailingSp) * descentEm * fontScale).coerceAtLeast(0f).dp

/** Roboto's line box under the baseline (555 of 2048 units), which a device measures for its own face. */
internal const val DESCENT_EM = 0.271f

internal const val PANEL_HERO_MAX = 56f

/** The sentence's column on the panel: a little more than a row's, since it has two lines only. */
internal val PanelSentenceMin = 120.dp

/** The air a panel keeps between its eyebrow and its block even with figures under it. */
internal val PanelAir = 16.dp

// --- The day in figures ----------------------------------------------------------------------

/**
 * How many lines of figures [room] holds, between [DETAIL_MIN_ROWS] (one line is not a table) and
 * [DETAIL_MAX_ROWS] (distance, calories, active and brisk minutes); 0 when fewer than two fit.
 */
internal fun detailRows(room: Dp, fontScale: Float): Int {
    val rows = ((room - DetailGap) / textLineHeight(FACT_SP, fontScale)).toInt()
    return if (rows >= DETAIL_MIN_ROWS) minOf(rows, DETAIL_MAX_ROWS) else 0
}

/** What [rows] of figures cost, gap included. */
internal fun detailHeight(rows: Int, fontScale: Float): Dp =
    if (rows <= 0) 0.dp else DetailGap + textLineHeight(FACT_SP, fontScale) * rows

/**
 * Adds the figures to a plan from its [spare] height. If they fit only with the metrics line's
 * room, or fit at all while that line is drawn, the line goes: the figures say the same.
 */
private fun <P> withDetails(
    plan: P,
    spare: Dp,
    fontScale: Float,
    details: Boolean,
    metricsDrawn: Boolean,
    apply: (P, Int, Boolean) -> P,
): P {
    if (!details) return plan
    val freed = if (metricsDrawn) textLineHeight(FACT_SP, fontScale) else 0.dp
    val rows = detailRows(spare + freed, fontScale)
    return if (rows > 0) apply(plan, rows, metricsDrawn) else plan
}

internal val DetailGap = 10.dp
internal const val DETAIL_MIN_ROWS = 2
internal const val DETAIL_MAX_ROWS = 4
