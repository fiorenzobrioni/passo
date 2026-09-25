package com.callbackdev.passo.widget.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.widget.FACT_SP
import com.callbackdev.passo.widget.STATUS_SP
import com.callbackdev.passo.widget.WidgetCardPadding
import com.callbackdev.passo.widget.WidgetCardPaddingSnug
import com.callbackdev.passo.widget.linesThatFit
import com.callbackdev.passo.widget.quarterPoint
import com.callbackdev.passo.widget.textInkBalance
import com.callbackdev.passo.widget.textLineHeight
import com.callbackdev.passo.widget.textSizeForLine

/*
 * «At a glance»: Chiaro's «Colpo d'occhio» with the ring where the weather glyph was. The
 * glyph there is the picture the card is read by across a room; here the ring is, and it says
 * the same kind of thing at a glance (how far along the day is) before any number is read.
 *
 * Five forms, picked by the grant the launcher really makes, so every size is a composition
 * and not a squeezed one:
 *
 * - [GlanceForm.DOT], one cell wide: the ring alone, with the count inside it.
 * - [GlanceForm.NARROW], one row: the ring, the number over «of 10,000 steps».
 * - [GlanceForm.WIDE], one row with room for a column more: the day's sentence at the far edge
 *   (Chiaro's split, the reference 4×1).
 * - [GlanceForm.TALL], two rows and up: the ring in the top trailing corner, the words hanging
 *   from the bottom leading one (Chiaro's tall form).
 * - [GlanceForm.PANEL], two rows and up, three cells or wider: the one-row card on top and,
 *   under it, the day hour by hour (PLANNING.md §7's 4×2 mini chart, 24 bars built from boxes).
 *
 * Everything is arithmetic on dp and sp (`GlanceLayoutTest` pins it at the household's
 * reference grants: a row is ~85 dp tall, two cells ~159 dp wide, three ~250, four ~340, a
 * 4×2 ~340 × 189). What only the launcher's face can answer (how wide a count is, how many
 * lines a sentence takes) is measured by the caller and passed in.
 */
internal enum class GlanceForm { DOT, NARROW, WIDE, TALL, PANEL }

internal fun glanceForm(size: DpSize, showHours: Boolean, fontScale: Float = 1f): GlanceForm = when {
    size.width < DotMaxWidth -> GlanceForm.DOT

    size.height >= TallMinHeight ->
        if (showHours && size.width >= PanelMinWidth && panelBarsHeight(size, fontScale) >= BarsMinHeight) {
            GlanceForm.PANEL
        } else {
            GlanceForm.TALL
        }

    rowSentenceColumn(size.width, rowRingSize(size)) >= SentenceColumnMin -> GlanceForm.WIDE

    else -> GlanceForm.NARROW
}

/** Under this a row has no room for the ring and a column of words beside it: the ring alone. */
internal val DotMaxWidth = 120.dp

/** Two rows, on every grid measured (Chiaro's `TallMinHeight`). */
internal val TallMinHeight = 150.dp

/** Three cells: 24 bars need about 7 dp each to read as a day and not as a comb. */
internal val PanelMinWidth = 230.dp

// --- One row -------------------------------------------------------------------------------

/**
 * The ring on a one-row card: the height the grant leaves, but never so large that the words
 * beside it lose their minimum, and never larger than [RowRingMax]. Smaller than Chiaro's
 * 66 dp glyph box on purpose: a Meteocons drawing keeps 9 to 12 dp of its box empty, a ring
 * inks it to the edge, so a 56 dp ring has the same weight on the card as a 66 dp glyph.
 */
internal fun rowRingSize(size: DpSize): Dp {
    val byHeight = size.height - WidgetCardPaddingSnug * 2
    val byWidth = size.width - WidgetCardPadding * 2 - RingTextGap - WordsColumnMin
    return minOf(byHeight, byWidth).coerceIn(RowRingMin, RowRingMax)
}

internal val RowRingMin = 44.dp
internal val RowRingMax = 56.dp

/** The air between the ring and the words: Chiaro's 8 dp plus the margin a glyph brings and a ring does not. */
internal val RingTextGap = 12.dp

/** The narrowest the words beside the ring may be: a count and «of 10,000» still whole (Chiaro's 84). */
internal val WordsColumnMin = 84.dp

/** What is left of a one-row card once the insets, the ring and its gap are paid. */
internal fun rowWordsWidth(width: Dp, ring: Dp): Dp = width - WidgetCardPadding * 2 - ring - RingTextGap

/** The sentence's column if the row were split evenly: whether a card is [GlanceForm.WIDE]. */
internal fun rowSentenceColumn(width: Dp, ring: Dp): Dp = (rowWordsWidth(width, ring) - SentenceGap) / 2

/**
 * Where the boundary between the words and the sentence falls (Chiaro's
 * `heroWordsColumnWidth`): the words take what they measured, at least the even split, and stop
 * the moment the sentence would lose any of what it measured.
 */
internal fun rowWordsColumn(width: Dp, ring: Dp, wordsNeed: Dp, sentenceKeep: Dp): Dp {
    val slack = rowWordsWidth(width, ring) - SentenceGap
    val even = slack / 2
    val keep = sentenceKeep.coerceAtMost(even)
    return maxOf(wordsNeed, WordsColumnMin).coerceIn(even, slack - keep)
}

/**
 * The count on a one-row card: Chiaro's 34 sp hero, smaller only where the column or the height
 * cannot hold it. [heroEm] is the widest count the day may print, in ems of the bold face (the
 * caller measures it on `max(steps, goal)`, so the number does not shrink as the day goes on).
 */
internal fun rowHeroSp(height: Dp, column: Dp, heroEm: Float, fontScale: Float, footnote: Boolean): Float {
    val room = height - WidgetCardPaddingSnug * 2 - textLineHeight(FACT_SP, fontScale) -
        (if (footnote) textLineHeight(STATUS_SP, fontScale) else textInkBalance(ROW_HERO_SP, fontScale))
    val byWidth = column.value / (heroEm * fontScale.coerceAtLeast(0.1f))
    return quarterPoint(minOf(ROW_HERO_SP, textSizeForLine(room, fontScale), byWidth).coerceAtLeast(HERO_FLOOR_SP))
}

internal const val ROW_HERO_SP = 34f
internal const val HERO_FLOOR_SP = 20f

/** The sentence beside a one-row card: what it measured, what the height holds, three at most. */
internal fun rowSentenceLines(height: Dp, fontScale: Float, measured: Int): Int = minOf(
    measured,
    linesThatFit(height - WidgetCardPaddingSnug * 2, SENTENCE_LINE_SP, fontScale),
    ROW_SENTENCE_MAX_LINES,
)

internal const val ROW_SENTENCE_MAX_LINES = 3
internal const val SENTENCE_LINE_SP = 16f

/** The column a sentence needs before it is worth printing (Chiaro's 96). */
internal val SentenceColumnMin = 96.dp
internal val SentenceGap = 12.dp

/** The mirrored row's sentence, at the number's shoulder: what the row leaves beside the count. */
internal fun mirroredSentenceWidth(width: Dp, ring: Dp, countWidth: Dp): Dp =
    rowWordsWidth(width, ring) - countWidth - SentenceGap

// --- Tall ------------------------------------------------------------------------------------

/**
 * The tall card, bottom up: the words (the number, the sentence, the goal) take what they need, and the ring takes the rest of the height in the top trailing corner, between
 * [TallRingMin] and [TallRingMax] (Chiaro's hero glyph range). When even two lines of sentence
 * would leave the ring under its floor, the sentence gives a line, then the other: the ring is
 * what the card is read by. Past the ring's ceiling the number grows instead, up to [TALL_HERO_MAX].
 */
internal data class TallPlan(val ring: Dp, val heroSp: Float, val sentenceLines: Int)

internal fun tallPlan(size: DpSize, fontScale: Float, heroEm: Float, sentenceLines: Int): TallPlan {
    val inner = size.height - WidgetCardPadding * 2
    val width = size.width - WidgetCardPadding * 2
    val fixed = textLineHeight(FACT_SP, fontScale)
    val byWidth = width.value / (heroEm * fontScale.coerceAtLeast(0.1f))
    val baseHero = minOf(ROW_HERO_SP, byWidth).coerceAtLeast(HERO_FLOOR_SP)
    var lines = minOf(sentenceLines, TALL_SENTENCE_MAX_LINES)
    fun ringRoom(l: Int) =
        inner - fixed - textLineHeight(baseHero, fontScale) - textLineHeight(SENTENCE_LINE_SP, fontScale) * l
    while (lines > 0 && ringRoom(lines) < TallRingMin) lines--
    val room = ringRoom(lines)
    val ring = room.coerceIn(TallRingMin, minOf(TallRingMax, width))
    val surplus = room - TallRingMax
    val hero = if (surplus > 0.dp) {
        minOf(textSizeForLine(textLineHeight(baseHero, fontScale) + surplus, fontScale), byWidth, TALL_HERO_MAX)
            .coerceAtLeast(baseHero)
    } else {
        baseHero
    }
    return TallPlan(ring = ring, heroSp = quarterPoint(hero), sentenceLines = lines)
}

internal val TallRingMin = 44.dp
internal val TallRingMax = 104.dp
internal const val TALL_HERO_MAX = 56f
internal const val TALL_SENTENCE_MAX_LINES = 2

// --- Panel -----------------------------------------------------------------------------------

/**
 * The panel's header: the one-row composition at the height its tallest column needs (the
 * ring, or the number over the goal, with its ink balance or the footnote of a card with no
 * room for a sentence).
 */
internal fun panelHeaderHeight(fontScale: Float, footnote: Boolean): Dp = maxOf(
    RowRingMax,
    textLineHeight(ROW_HERO_SP, fontScale) + textLineHeight(FACT_SP, fontScale) +
        (if (footnote) textLineHeight(STATUS_SP, fontScale) else textInkBalance(ROW_HERO_SP, fontScale)),
)

/**
 * What the bars get under the header: the rest of the card, less the gap and the hour labels,
 * up to [BarsMaxHeight] (past it a day's bars read as a bar chart of something else, and the
 * air goes between the header and the bars instead). Measured without a status line, so the
 * form does not change the moment counting pauses.
 */
internal fun panelBarsHeight(size: DpSize, fontScale: Float): Dp {
    val rest = size.height - WidgetCardPadding * 2 - panelHeaderHeight(fontScale, footnote = false) -
        BarsGap - barsLabelHeight(fontScale)
    return rest.coerceAtMost(BarsMaxHeight)
}

internal fun barsLabelHeight(fontScale: Float): Dp = textLineHeight(BARS_LABEL_SP, fontScale) + BarsLabelGap

internal val BarsGap = 12.dp
internal val BarsLabelGap = 2.dp
internal const val BARS_LABEL_SP = 11f
internal val BarsMinHeight = 28.dp
internal val BarsMaxHeight = 112.dp

// --- The ring's inside -----------------------------------------------------------------------

/** The one-cell ring: as large as the cell allows, centred. */
internal fun dotRingSize(size: DpSize): Dp =
    (minOf(size.width, size.height) - DotPadding * 2).coerceIn(DotRingMin, TallRingMax)

internal val DotPadding = 8.dp
internal val DotRingMin = 36.dp

/** The hole in the middle of a ring of side [ring]. */
internal fun ringInner(ring: Dp): Dp = ring - RingPainter.strokeOf(ring) * 2

/**
 * A figure set inside the ring: as large as fits [INNER_FILL] of the hole, never over [maxSp].
 * Null when the hole is too small for anything a reader could read ([MIN_INSIDE_SP]).
 */
internal fun insideSp(ring: Dp, textEm: Float, fontScale: Float, maxSp: Float): Float? {
    val sp = ringInner(ring).value * INNER_FILL / (textEm * fontScale.coerceAtLeast(0.1f))
    return quarterPoint(minOf(sp, maxSp)).takeIf { it >= MIN_INSIDE_SP }
}

internal const val INNER_FILL = 0.72f
internal const val MIN_INSIDE_SP = 10f
internal const val DOT_COUNT_MAX_SP = 28f
internal const val RING_PERCENT_MAX_SP = 18f
