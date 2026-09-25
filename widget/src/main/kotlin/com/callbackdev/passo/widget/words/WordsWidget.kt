package com.callbackdev.passo.widget.words

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
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.widget.CardModels
import com.callbackdev.passo.widget.FACT_SP
import com.callbackdev.passo.widget.MessageContent
import com.callbackdev.passo.widget.PassoWidgetReceiver
import com.callbackdev.passo.widget.R
import com.callbackdev.passo.widget.StatusFootnote
import com.callbackdev.passo.widget.TextWeight
import com.callbackdev.passo.widget.WidgetCard
import com.callbackdev.passo.widget.WidgetCardPadding
import com.callbackdev.passo.widget.WidgetCardPaddingSnug
import com.callbackdev.passo.widget.WidgetDay
import com.callbackdev.passo.widget.WidgetModel
import com.callbackdev.passo.widget.WidgetPalette
import com.callbackdev.passo.widget.WidgetRefresh
import com.callbackdev.passo.widget.WidgetSamples
import com.callbackdev.passo.widget.balancedWidth
import com.callbackdev.passo.widget.eyebrowText
import com.callbackdev.passo.widget.fontScale
import com.callbackdev.passo.widget.goalPercentText
import com.callbackdev.passo.widget.goalShareText
import com.callbackdev.passo.widget.measureWidgetLines
import com.callbackdev.passo.widget.measureWidgetText
import com.callbackdev.passo.widget.messageHint
import com.callbackdev.passo.widget.messageTitle
import com.callbackdev.passo.widget.metricsText
import com.callbackdev.passo.widget.rememberWidgetModel
import com.callbackdev.passo.widget.sentence
import com.callbackdev.passo.widget.statusText
import com.callbackdev.passo.widget.textEm
import com.callbackdev.passo.widget.widgetDressFor
import com.callbackdev.passo.widget.widgetFormatter
import com.callbackdev.passo.widget.withSlack

/**
 * «In words» (Chiaro's «In parole»): the same day as «At a glance», with no drawing on the card
 * but two marks at the size of the line they belong to (a check before a goal that is met, and
 * the pause of a count that is not moving). The number is the drawing now. [WordsLayout] holds
 * the ranks, the forms and every number's reason.
 */
class WordsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetSamples.FourByOne, WidgetSamples.FourByTwo))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val models = CardModels(context, appWidgetId)
        // Read before the load, so only a change after it reloads (WidgetRefresh).
        val loadedAt = WidgetRefresh.revision.value
        val initial = models.load()
        provideContent {
            WordsWidgetContent(rememberWidgetModel(initial, loadedAt) { models.load() })
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { WordsWidgetContent(WidgetSamples.model()) }
    }
}

class WordsWidgetReceiver : PassoWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordsWidget()
}

/** The whole card for [model] at [LocalSize], shared with the settings preview and the tests. */
@Composable
internal fun WordsWidgetContent(model: WidgetModel) {
    val context = LocalContext.current
    val size = LocalSize.current
    val dress =
        remember(model.settings.palette, model.settings.dynamicColor) { widgetDressFor(context, model.settings) }
    val day = model.day
    val form = day?.let { wordsForm(size) }
    val oneRow = form == WordsForm.LINE || form == WordsForm.ROW
    WidgetCard(model, dress, paddingVertical = if (oneRow) WidgetCardPaddingSnug else WidgetCardPadding) { palette ->
        if (day == null || form == null) {
            MessageContent(messageTitle(context, model), messageHint(context, model), palette)
            return@WidgetCard
        }
        val parts = WordsParts(context, model, day, palette, widgetFormatter(context, model.settings))
        when (form) {
            WordsForm.LINE -> LineContent(parts, size)
            WordsForm.ROW -> RowContent(parts, size)
            WordsForm.STACK -> StackContent(parts, size)
            WordsForm.PANEL -> PanelContent(parts, size)
        }
    }
}

private class WordsParts(
    val context: Context,
    val model: WidgetModel,
    val day: WidgetDay,
    val palette: WidgetPalette,
    val format: MeasureFormatter,
) {
    val overview = day.overview
    val look = model.look
    val status = model.state != CountingState.COUNTING
    val scale = fontScale(context)
    val count: String = format.steps(overview.steps)
    val heroEm: Float = textEm(format.steps(maxOf(overview.steps, overview.goalSteps)), TextWeight.BOLD)
    val reached = overview.goalReachedAt != null

    /** What the sentence's place says: what a tap does while the count is not moving, else the sentence. */
    val slot: String? =
        statusText(context, model.state) ?: if (look.showSentence) sentence(context, overview, format) else null
    val slotIsStatus = status

    fun slotNeeds(width: Dp): Int =
        slot?.let { measureWidgetLines(context, it, WORDS_SENTENCE_SP, width, TextWeight.MEDIUM) } ?: 0

    /** The metrics line is drawn whole or not at all: a cut estimate is a wrong one. */
    fun metrics(width: Dp): Boolean = look.showMetrics &&
        measureWidgetText(context, metricsText(context, overview, format), FACT_SP, TextWeight.MEDIUM).withSlack() <=
        width

    fun eyebrow(room: Dp): String {
        val long = eyebrowText(context, short = false)
        return if (measureWidgetText(context, long, FACT_SP).withSlack() <=
            room
        ) {
            long
        } else {
            eyebrowText(context, short = true)
        }
    }

    /** «84% of 10,000», or «84%» where the whole fact does not fit [room] (the check mark included). */
    fun goal(room: Dp): String {
        val share = goalShareText(context, overview, format)
        val mark = if (reached) markSize(scale) + MarkGap else 0.dp
        return if (measureWidgetText(context, share, FACT_SP, TextWeight.MEDIUM).withSlack() + mark <= room) {
            share
        } else {
            goalPercentText(overview, format)
        }
    }
}

// --- Lines -------------------------------------------------------------------------------------

@Composable
private fun Eyebrow(parts: WordsParts, room: Dp) {
    Text(
        text = parts.eyebrow(room),
        style = TextStyle(color = parts.palette.secondaryInk, fontSize = FACT_SP.sp),
        maxLines = 1,
    )
}

@Composable
private fun Count(parts: WordsParts, sp: Float) {
    Text(
        text = parts.count,
        style = TextStyle(color = parts.palette.primaryInk, fontSize = sp.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

@Composable
private fun SentenceText(parts: WordsParts, lines: Int, align: TextAlign, column: Dp) {
    val text = parts.slot ?: return
    if (lines <= 0) return
    val width = balancedWidth(parts.context, text, WORDS_SENTENCE_SP, column, TextWeight.MEDIUM, lines)
    Text(
        text = text,
        style = TextStyle(
            color = if (parts.slotIsStatus) parts.palette.attentionInk else parts.palette.primaryInk,
            fontSize = WORDS_SENTENCE_SP.sp,
            fontWeight = FontWeight.Medium,
            textAlign = align,
        ),
        maxLines = lines,
        modifier = GlanceModifier.width(width),
    )
}

/** Rank 3's figures: Medium, in the strong ink, as Chiaro sets the day's range. */
private fun factStyle(parts: WordsParts, align: TextAlign = TextAlign.Start) = TextStyle(
    color = parts.palette.primaryInk,
    fontSize = FACT_SP.sp,
    fontWeight = FontWeight.Medium,
    textAlign = align,
)

/**
 * The goal as a fact, with a check before it once it is met: a mark at the line's own size,
 * in the line's own ink, which is punctuation rather than a drawing (Chiaro's rule for the
 * range's arrows). The air after it is a wrapper's padding, never the image's own: an `Image`
 * scales its drawing into what its padding leaves (Chiaro's shrinking arrow, 20 Sep 2026).
 */
@Composable
private fun GoalFact(parts: WordsParts, room: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (parts.reached) {
            Box(modifier = GlanceModifier.padding(end = MarkGap)) {
                Image(
                    provider = ImageProvider(R.drawable.widget_mark_check),
                    contentDescription = parts.context.getString(R.string.widget_goal_reached_mark),
                    colorFilter = ColorFilter.tint(parts.palette.primaryInk),
                    modifier = GlanceModifier.size(markSize(parts.scale)),
                )
            }
        }
        Text(text = parts.goal(room), style = factStyle(parts), maxLines = 1)
    }
}

@Composable
private fun MetricsFact(parts: WordsParts, align: TextAlign) {
    Text(text = metricsText(parts.context, parts.overview, parts.format), style = factStyle(parts, align), maxLines = 1)
}

private fun markSize(scale: Float): Dp = (FACT_SP * scale).dp

private val MarkGap = 4.dp

/**
 * The day in figures: the quantities Today shows in tiles, one per line, the figure first and
 * its words after it, the estimates saying so in words («walked, estimated»). The figures are a
 * fixed column, so they line up down the table the way a timetable's do.
 */
@Composable
private fun Details(parts: WordsParts, rows: Int) {
    if (rows <= 0) return
    val context = parts.context
    val res = context.resources
    val metrics = parts.overview.metrics
    val lines = listOf(
        res.format(parts.format.distance(metrics.distanceMeters)) to R.string.widget_detail_distance,
        res.format(parts.format.energy(metrics.activeKcal)) to R.string.widget_detail_calories,
        res.format(parts.format.minutes(metrics.activeMinutes)) to R.string.widget_detail_active,
        res.format(parts.format.minutes(metrics.briskMinutes)) to R.string.widget_detail_brisk,
    ).take(rows)
    val valueColumn = lines.maxOf { measureWidgetText(context, it.first, FACT_SP, TextWeight.MEDIUM) } + DetailValueGap
    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = DetailGap)) {
        lines.forEach { (value, label) ->
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, style = factStyle(parts), maxLines = 1, modifier = GlanceModifier.width(valueColumn))
                Text(
                    text = context.getString(label),
                    style = TextStyle(color = parts.palette.secondaryInk, fontSize = DETAIL_LABEL_SP.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

private val DetailValueGap = 10.dp

// --- The forms ---------------------------------------------------------------------------------

/** One or two cells in one row: the eyebrow, the number, the goal where it fits, the status. */
@Composable
private fun LineContent(parts: WordsParts, size: DpSize) {
    val plan = linePlan(size, parts.scale, parts.heroEm, parts.look.showGoal, parts.status)
    val width = size.width - WidgetCardPadding * 2
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Eyebrow(parts, width)
        Count(parts, plan.heroSp)
        if (plan.goal) GoalFact(parts, width)
        StatusFootnote(parts.model.state, parts.palette, width)
    }
}

/** One row: the eyebrow over the number on the leading side; the sentence and the facts at the far edge. */
@Composable
private fun RowContent(parts: WordsParts, size: DpSize) {
    val context = parts.context
    val eyebrowWidth = measureWidgetText(context, eyebrowText(context, short = false), FACT_SP)
    val leading = rowLeading(size, parts.scale, parts.heroEm, eyebrowWidth)
    val column = rowSentenceColumn(size, leading)
    val hero = rowHeroSp(size, parts.scale, parts.heroEm, leading)
    val plan = rowColumnPlan(size, parts.scale, parts.slotNeeds(column), parts.look.showGoal, parts.metrics(column))
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.width(leading)) {
            Eyebrow(parts, leading)
            Count(parts, hero)
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = GlanceModifier.padding(start = ColumnGap).defaultWeight(),
        ) {
            SentenceText(parts, plan.sentenceLines, TextAlign.End, column)
            if (plan.goal) GoalFact(parts, column)
            if (plan.metrics) MetricsFact(parts, TextAlign.End)
        }
    }
}

/** Tall and narrow: the eyebrow on top, the rest on the bottom edge, the air between them. */
@Composable
private fun StackContent(parts: WordsParts, size: DpSize) {
    val width = size.width - WidgetCardPadding * 2
    val plan = stackPlan(
        size,
        parts.scale,
        parts.heroEm,
        parts.slotNeeds(width),
        goal = parts.look.showGoal,
        metrics = parts.metrics(width),
        details = parts.look.showDetails,
    )
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Eyebrow(parts, width)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Count(parts, plan.heroSp)
        SentenceText(parts, plan.column.sentenceLines, TextAlign.Start, width)
        if (plan.column.goal) GoalFact(parts, width)
        if (plan.column.metrics) MetricsFact(parts, TextAlign.Start)
        Details(parts, plan.detailRows)
    }
}

/**
 * Wide and tall: the eyebrow across the top; along the bottom the number beside the words,
 * right-aligned, their last lines on the number's baseline; the day in figures under both.
 */
@Composable
private fun PanelContent(parts: WordsParts, size: DpSize) {
    val width = size.width - WidgetCardPadding * 2
    val sentenceColumn = width - ColumnGap - leadingFor(parts, size)
    val plan = panelPlan(
        size,
        parts.scale,
        parts.heroEm,
        parts.slotNeeds(sentenceColumn),
        goal = parts.look.showGoal,
        metrics = parts.metrics(sentenceColumn),
        details = parts.look.showDetails,
    )
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Eyebrow(parts, width)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(modifier = GlanceModifier.width(plan.leading)) {
                Count(parts, plan.heroSp)
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier.padding(start = ColumnGap).defaultWeight(),
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = GlanceModifier.padding(bottom = plan.baselineLift),
                ) {
                    SentenceText(parts, plan.column.sentenceLines, TextAlign.End, sentenceColumn)
                    if (plan.column.goal) GoalFact(parts, width - plan.leading - ColumnGap)
                    if (plan.column.metrics) MetricsFact(parts, TextAlign.End)
                }
            }
        }
        Details(parts, plan.detailRows)
    }
}

/** The panel's leading column before the plan exists: the same arithmetic, so the sentence is measured at its real width. */
private fun leadingFor(parts: WordsParts, size: DpSize): Dp = panelPlan(
    size,
    parts.scale,
    parts.heroEm,
    sentenceNeeds = 2,
    goal = false,
    metrics = false,
    details = false,
).leading
