package com.callbackdev.passo.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.passo.core.designsystem.theme.WidgetDress
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.widget.FACT_SP
import com.callbackdev.passo.widget.MessageContent
import com.callbackdev.passo.widget.PassoWidgetReceiver
import com.callbackdev.passo.widget.R
import com.callbackdev.passo.widget.StatusFootnote
import com.callbackdev.passo.widget.TextWeight
import com.callbackdev.passo.widget.WidgetArrangement
import com.callbackdev.passo.widget.WidgetCard
import com.callbackdev.passo.widget.WidgetCardPadding
import com.callbackdev.passo.widget.WidgetCardPaddingSnug
import com.callbackdev.passo.widget.WidgetDay
import com.callbackdev.passo.widget.WidgetModel
import com.callbackdev.passo.widget.WidgetPalette
import com.callbackdev.passo.widget.WidgetRefresh
import com.callbackdev.passo.widget.WidgetSamples
import com.callbackdev.passo.widget.axisHour
import com.callbackdev.passo.widget.balancedWidth
import com.callbackdev.passo.widget.compactCount
import com.callbackdev.passo.widget.fontScale
import com.callbackdev.passo.widget.goalOfText
import com.callbackdev.passo.widget.measureWidgetLines
import com.callbackdev.passo.widget.measureWidgetText
import com.callbackdev.passo.widget.messageHint
import com.callbackdev.passo.widget.messageTitle
import com.callbackdev.passo.widget.rememberWidgetModel
import com.callbackdev.passo.widget.ringDescription
import com.callbackdev.passo.widget.sentence
import com.callbackdev.passo.widget.statusText
import com.callbackdev.passo.widget.textEm
import com.callbackdev.passo.widget.textInkBalance
import com.callbackdev.passo.widget.widgetDressFor
import com.callbackdev.passo.widget.widgetEntryPoint
import com.callbackdev.passo.widget.widgetFormatter
import com.callbackdev.passo.widget.withSlack

/**
 * «At a glance» (Chiaro's «Colpo d'occhio»): today's ring, the count, the goal, the day's
 * sentence where the grant has room, and on a wide tall card the day hour by hour. The forms
 * and their arithmetic are [GlanceLayout]'s; this file draws them.
 */
class GlanceWidget : GlanceAppWidget() {
    /** Exact, so [LocalSize] is the size the launcher granted: every form is read off it. */
    override val sizeMode: SizeMode = SizeMode.Exact

    /** The picker's generated preview (API 35+): the default placement and the panel. */
    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetSamples.FourByOne, WidgetSamples.FourByTwo))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val loader = context.widgetEntryPoint().loader()
        // Read before the load, so only a change after it reloads (WidgetRefresh).
        val loadedAt = WidgetRefresh.revision.value
        val initial = loader.load(appWidgetId)
        provideContent {
            GlanceWidgetContent(rememberWidgetModel(initial, loadedAt) { loader.load(appWidgetId) })
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceWidgetContent(WidgetSamples.model()) }
    }
}

class GlanceWidgetReceiver : PassoWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceWidget()
}

/**
 * The whole card for [model] at [LocalSize]: split off the widget so the settings screen's
 * preview and the tests draw the same composition the launcher does.
 */
@Composable
internal fun GlanceWidgetContent(model: WidgetModel) {
    val context = LocalContext.current
    val size = LocalSize.current
    val dress =
        remember(model.settings.palette, model.settings.dynamicColor) { widgetDressFor(context, model.settings) }
    val day = model.day
    val scale = fontScale(context)
    val form = day?.let { glanceForm(size, model.look.showHours, scale) }
    val (horizontal, vertical) = when (form) {
        GlanceForm.DOT -> DotPadding to DotPadding
        GlanceForm.NARROW, GlanceForm.WIDE -> WidgetCardPadding to WidgetCardPaddingSnug
        else -> WidgetCardPadding to WidgetCardPadding
    }
    WidgetCard(model, dress, paddingHorizontal = horizontal, paddingVertical = vertical) { palette ->
        if (day == null || form == null) {
            MessageContent(messageTitle(context, model.state), messageHint(context, model.state), palette)
            return@WidgetCard
        }
        val parts = CardParts(context, model, day, palette, widgetFormatter(context, model.settings))
        when (form) {
            GlanceForm.DOT -> DotContent(parts, dotRingSize(size))
            GlanceForm.NARROW, GlanceForm.WIDE -> RowContent(parts, size.width, size.height, form == GlanceForm.WIDE)
            GlanceForm.TALL -> TallContent(parts, size)
            GlanceForm.PANEL -> PanelContent(parts, size)
        }
    }
}

/** What every form draws from, read once. */
private class CardParts(
    val context: Context,
    val model: WidgetModel,
    val day: WidgetDay,
    val palette: WidgetPalette,
    val format: MeasureFormatter,
) {
    val overview = day.overview
    val live = model.state == CountingState.COUNTING
    val status = !live
    val scale = fontScale(context)
    val count: String = format.steps(overview.steps)

    /** The widest count this day is likely to print, so the number keeps its size as it grows. */
    val heroEm: Float = textEm(format.steps(maxOf(overview.steps, overview.goalSteps)), TextWeight.BOLD)

    /** What the sentence's place says: what a tap does while the count is not moving, else the sentence. */
    val sentence: String? =
        statusText(context, model.state) ?: if (model.look.showSentence) sentence(context, overview, format) else null
    val sentenceIsStatus = status

    fun goalLine(room: Dp): String {
        val long = goalOfText(context, overview.goalSteps, format, short = false)
        return if (measureWidgetText(context, long, FACT_SP).withSlack() <= room) {
            long
        } else {
            goalOfText(context, overview.goalSteps, format, short = true)
        }
    }

    fun ringSpec() = RingSpec(
        progress = overview.progress.toFloat(),
        usual = overview.usualProgress?.toFloat(),
        reached = overview.goalReachedAt != null,
        live = live,
    )
}

// --- The ring --------------------------------------------------------------------------------

/**
 * The ring at [side], and inside it either the count ([dot]) or the share of the goal: a check
 * once the goal is met, a pause mark while the count is not moving.
 */
@Composable
private fun Ring(parts: CardParts, side: Dp, dot: Boolean = false) {
    val context = parts.context
    val palette = parts.palette
    val density = context.resources.displayMetrics.density
    val spec = parts.ringSpec()
    val inks = RingInks(
        accent = palette.accent,
        quiet = palette.secondary,
        track = palette.track,
        goal = palette.goal,
        notch = palette.primary,
        halo = palette.halo,
    )
    val bitmap = remember(side, density, spec, inks) { RingPainter.paint(side, density, spec, inks) }
    Box(modifier = GlanceModifier.size(side), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = ringDescription(context, parts.overview, parts.format),
            modifier = GlanceModifier.size(side),
        )
        when {
            dot && !parts.live -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // A one-cell card has no line for the status: the pause mark goes under the count.
                RingFigure(
                    compactCount(context, parts.overview.steps),
                    side * DOT_PAUSED_SHARE,
                    parts,
                    DOT_COUNT_MAX_SP,
                    bold = true,
                )
                RingMark(R.drawable.widget_mark_pause, side * DOT_PAUSED_MARK, palette.attention)
            }

            dot -> RingFigure(compactCount(context, parts.overview.steps), side, parts, DOT_COUNT_MAX_SP, bold = true)

            !parts.live -> RingMark(R.drawable.widget_mark_pause, side, palette.attention)

            spec.reached -> RingMark(R.drawable.widget_mark_check, side, palette.goal)

            else -> RingFigure(
                parts.format.percent(parts.overview.progress),
                side,
                parts,
                RING_PERCENT_MAX_SP,
                bold = false,
            )
        }
    }
}

@Composable
private fun RingFigure(text: String, ring: Dp, parts: CardParts, maxSp: Float, bold: Boolean) {
    val em = textEm(text, if (bold) TextWeight.BOLD else TextWeight.MEDIUM)
    val sp = insideSp(ring, em, parts.scale, maxSp) ?: return
    Text(
        text = text,
        style = TextStyle(
            color = parts.palette.primaryInk,
            fontSize = sp.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        maxLines = 1,
    )
}

@Composable
private fun RingMark(drawable: Int, ring: Dp, color: androidx.compose.ui.graphics.Color) {
    val side = ringInner(ring) * MARK_SHARE
    if (side < MarkMin) return
    Image(
        provider = ImageProvider(drawable),
        contentDescription = null, // the ring's own description says it
        colorFilter = ColorFilter.tint(ColorProvider(color)),
        modifier = GlanceModifier.size(side),
    )
}

private const val MARK_SHARE = 0.5f

/** A paused one-cell ring shares its hole between the count and the mark under it. */
private const val DOT_PAUSED_SHARE = 0.8f
private const val DOT_PAUSED_MARK = 0.55f
private val MarkMin = 12.dp

// --- The forms -------------------------------------------------------------------------------

/** One cell: the ring and the count inside it; the ring's description says the rest. */
@Composable
private fun DotContent(parts: CardParts, ring: Dp) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Ring(parts, ring, dot = true)
    }
}

/** The count, Bold at [sp]: the card's hero. One line, always. */
@Composable
private fun Count(parts: CardParts, sp: Float) {
    Text(
        text = parts.count,
        style = TextStyle(color = parts.palette.primaryInk, fontSize = sp.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

@Composable
private fun GoalLine(parts: CardParts, room: Dp) {
    Text(
        text = parts.goalLine(room),
        style = TextStyle(color = parts.palette.secondaryInk, fontSize = FACT_SP.sp),
        maxLines = 1,
    )
}

@Composable
private fun Sentence(parts: CardParts, lines: Int, align: TextAlign, modifier: GlanceModifier = GlanceModifier) {
    val text = parts.sentence ?: return
    if (lines <= 0) return
    Text(
        text = text,
        style = TextStyle(
            color = if (parts.sentenceIsStatus) parts.palette.attentionInk else parts.palette.primaryInk,
            fontSize = SENTENCE_LINE_SP.sp,
            fontWeight = FontWeight.Medium,
            textAlign = align,
        ),
        maxLines = lines,
        modifier = modifier,
    )
}

/**
 * The one-row card: the ring, the count over the goal, and on a wide card the day's sentence
 * against the far edge (or at the number's shoulder, mirrored). Also the panel's header.
 */
@Composable
private fun RowContent(
    parts: CardParts,
    width: Dp,
    height: Dp,
    wide: Boolean,
    ring: Dp = rowRingSize(DpSize(width, height)),
) {
    val context = parts.context
    val scale = parts.scale
    val words = rowWordsWidth(width, ring)
    val mirrored = parts.model.look.arrangement == WidgetArrangement.RING_END
    if (mirrored) {
        MirroredRow(parts, width, height, ring, wide)
        return
    }
    val sentenceText = parts.sentence.takeIf { wide }
    val footnote = parts.status && sentenceText == null
    // The words' ink and the ring's share a centre line (Chiaro's `textInkBalance`); a footnote
    // under them already fills that band.
    val balance = if (footnote) GlanceModifier else GlanceModifier.padding(bottom = textInkBalance(ROW_HERO_SP, scale))
    val column = if (sentenceText == null) {
        words
    } else {
        val need = maxOf(
            measureWidgetText(context, parts.count, ROW_HERO_SP, TextWeight.BOLD),
            measureWidgetText(
                context,
                goalOfText(context, parts.overview.goalSteps, parts.format, short = false),
                FACT_SP,
            ),
        ).withSlack()
        val keep = measureWidgetText(context, sentenceText, SENTENCE_LINE_SP, TextWeight.MEDIUM).withSlack()
        rowWordsColumn(width, ring, need, keep)
    }
    val hero = rowHeroSp(height, column, parts.heroEm, scale, footnote)
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Ring(parts, ring)
        // A width beside a sentence, where the split was measured; the whole row without one.
        val sized = if (sentenceText !=
            null
        ) {
            GlanceModifier.width(RingTextGap + column)
        } else {
            GlanceModifier.defaultWeight()
        }
        Column(modifier = GlanceModifier.padding(start = RingTextGap).then(balance).then(sized)) {
            Count(parts, hero)
            GoalLine(parts, column)
            if (footnote) StatusFootnote(parts.model.state, parts.palette, column)
        }
        if (sentenceText != null) {
            val sentenceColumn = words - column - SentenceGap
            val measured =
                measureWidgetLines(context, sentenceText, SENTENCE_LINE_SP, sentenceColumn, TextWeight.MEDIUM)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier.padding(start = SentenceGap).defaultWeight(),
            ) {
                val lines = rowSentenceLines(height, scale, measured)
                val balanced =
                    balancedWidth(context, sentenceText, SENTENCE_LINE_SP, sentenceColumn, TextWeight.MEDIUM, lines)
                Sentence(parts, lines, TextAlign.End, GlanceModifier.width(balanced))
            }
        }
    }
}

/**
 * The row the other way round (Chiaro's `ICON_END`): the count with the sentence at its
 * shoulder, the goal under both, the ring closing the row. The sentence only where it clears
 * the same minimum as the standard row's column.
 */
@Composable
private fun MirroredRow(parts: CardParts, width: Dp, height: Dp, ring: Dp, wide: Boolean) {
    val context = parts.context
    val scale = parts.scale
    val words = rowWordsWidth(width, ring)
    val countWidth = measureWidgetText(context, parts.count, ROW_HERO_SP, TextWeight.BOLD).withSlack()
    val shoulder = mirroredSentenceWidth(width, ring, countWidth)
    val showSentence = wide && parts.sentence != null && shoulder >= SentenceColumnMin
    val footnote = parts.status && !showSentence
    val hero = rowHeroSp(height, words, parts.heroEm, scale, footnote)
    val balance = if (footnote) 0.dp else textInkBalance(ROW_HERO_SP, scale)
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = GlanceModifier.defaultWeight().padding(end = RingTextGap, bottom = balance),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Count(parts, hero)
                if (showSentence) {
                    val sentenceText = parts.sentence.orEmpty()
                    val measured =
                        measureWidgetLines(context, sentenceText, SENTENCE_LINE_SP, shoulder, TextWeight.MEDIUM)
                    Sentence(
                        parts,
                        minOf(measured, TALL_SENTENCE_MAX_LINES),
                        TextAlign.Start,
                        GlanceModifier.padding(start = SentenceGap).defaultWeight(),
                    )
                }
            }
            GoalLine(parts, words)
            if (footnote) StatusFootnote(parts.model.state, parts.palette, words)
        }
        Ring(parts, ring)
    }
}

/**
 * Two rows and up: the ring alone in the top trailing corner, the words stacked from the bottom
 * leading one in Chiaro's order: the number, the sentence, the goal.
 */
@Composable
private fun TallContent(parts: CardParts, size: DpSize) {
    val context = parts.context
    val width = size.width - WidgetCardPadding * 2
    val measured =
        parts.sentence?.let { measureWidgetLines(context, it, SENTENCE_LINE_SP, width, TextWeight.MEDIUM) } ?: 0
    val plan = tallPlan(size, parts.scale, parts.heroEm, measured)
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Ring(parts, plan.ring)
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            Count(parts, plan.heroSp)
            val sentenceWidth = parts.sentence?.let {
                balancedWidth(context, it, SENTENCE_LINE_SP, width, TextWeight.MEDIUM, plan.sentenceLines)
            } ?: width
            Sentence(parts, plan.sentenceLines, TextAlign.Start, GlanceModifier.width(sentenceWidth))
            GoalLine(parts, width)
        }
    }
}

/**
 * Wide and tall: the one-row card on top, and under it the day hour by hour, with the air
 * between the two when the card is taller than the bars want.
 */
@Composable
private fun PanelContent(parts: CardParts, size: DpSize) {
    val wide = rowSentenceColumn(size.width, RowRingMax) >= SentenceColumnMin
    val header = panelHeaderHeight(parts.scale, footnote = parts.status && !wide)
    val bars = panelBarsHeight(size, parts.scale).coerceAtLeast(BarsMinHeight)
    val inner = size.width - WidgetCardPadding * 2
    // The header is laid out as a one-row card of its own height (the row's snug inset included).
    val rowHeight = header + WidgetCardPaddingSnug * 2
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(header)) {
            RowContent(parts, size.width, rowHeight, wide, ring = RowRingMax)
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        HourBars(parts, bars, inner)
    }
}

// --- The day hour by hour --------------------------------------------------------------------

/**
 * Today in 24 bars (PLANNING.md §7): boxes, no bitmap, each hour's height against the day's
 * busiest, the hour under way in the ring's own ink and the hours gone in the same ink, lighter.
 * The hours still to come are a hairline of track: the day is visibly not over.
 *
 * Glance drops the eleventh child of a container without a word, so the 24 bars are four
 * groups of six, and a group's first bar is where its label goes: 00, 06, 12, 18.
 */
@Composable
private fun HourBars(parts: CardParts, height: Dp, width: Dp) {
    val context = parts.context
    val palette = parts.palette
    val hourly = parts.day.hourly
    val nowHour = (parts.day.nowMinute / MINUTES_PER_HOUR).coerceIn(0, HOURS - 1)
    val bars = hourBars(hourly.toList(), nowHour, height)
    val current = if (parts.live) palette.accent else palette.secondary
    val past = current.copy(alpha = PAST_ALPHA)
    val description = if (hourly.busiest > 0) {
        val busiestHour = (0 until HOURS).maxBy { hourly[it] }
        context.getString(
            R.string.widget_hours_description,
            axisHour(context, busiestHour),
            parts.format.steps(hourly.busiest),
        )
    } else {
        context.getString(R.string.widget_hours_description_empty)
    }
    val gap = barGap(width)
    Column(modifier = GlanceModifier.fillMaxWidth().semantics { contentDescription = description }) {
        Row(modifier = GlanceModifier.fillMaxWidth().height(height)) {
            for (group in 0 until GROUPS) {
                Row(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (i in 0 until HOURS_PER_GROUP) {
                        val bar = bars[group * HOURS_PER_GROUP + i]
                        val color = when (bar.kind) {
                            BarKind.NOW -> current
                            BarKind.PAST -> past
                            BarKind.EMPTY -> palette.track
                        }
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = gap / 2),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(bar.height)
                                    .cornerRadius(BarCorner)
                                    .background(ColorProvider(color)),
                            ) {}
                        }
                    }
                }
            }
        }
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = BarsLabelGap)) {
            for (group in 0 until GROUPS) {
                Text(
                    text = axisHour(context, group * HOURS_PER_GROUP),
                    style = TextStyle(color = palette.secondaryInk, fontSize = BARS_LABEL_SP.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight().padding(start = gap / 2),
                )
            }
        }
    }
}

private const val PAST_ALPHA = 0.55f
private val BarCorner = 2.dp
