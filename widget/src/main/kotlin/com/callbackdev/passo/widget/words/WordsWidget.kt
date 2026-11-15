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
import com.callbackdev.passo.core.designsystem.format.sessionBrief
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.widget.CardModels
import com.callbackdev.passo.widget.FACT_SP
import com.callbackdev.passo.widget.MessageContent
import com.callbackdev.passo.widget.PassoWidgetReceiver
import com.callbackdev.passo.widget.R
import com.callbackdev.passo.widget.SentenceForms
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
import com.callbackdev.passo.widget.statusText
import com.callbackdev.passo.widget.textEm
import com.callbackdev.passo.widget.widgetDressFor
import com.callbackdev.passo.widget.widgetFormatter
import com.callbackdev.passo.widget.withSlack

/**
 * «In words»: the same day as «At a glance», with no drawing on the card
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
    val heroEm: Float = textEm(context, format.steps(maxOf(overview.steps, overview.goalSteps)), TextWeight.BOLD)
    val reached = overview.goalReachedAt != null

    /**
     * What the sentence's place says: what a tap does while the count is not moving, else the
     * outing under way, else the sentence.
     */
    val forms = SentenceForms.of(context, model.state, model.session, overview, format, look.showSentence)
    val slotIsStatus = status

    fun slotLines(text: String, width: Dp): Int =
        measureWidgetLines(context, text, WORDS_SENTENCE_SP, width, TextWeight.MEDIUM)

    /** The form a column of [width] can give its lines whole ([plan] of those lines), with that plan. */
    fun <P> fitSlot(width: Dp, plan: (Int) -> P, lines: (P) -> Int): Pair<P, String?> =
        forms.fit({ slotLines(it, width) }, plan, lines)

    /** The metrics line is drawn whole or not at all: a cut estimate is a wrong one. */
    fun metrics(width: Dp): Boolean = look.showMetrics &&
        measureWidgetText(context, metricsText(context, overview, format), FACT_SP, TextWeight.MEDIUM).withSlack() <=
        width

    /** The day in figures, figure and words, in the order a table prints them. */
    fun detailLines(): List<Pair<String, String>> {
        val res = context.resources
        val metrics = overview.metrics
        return listOf(
            res.format(format.distance(metrics.distanceMeters)) to R.string.widget_detail_distance,
            res.format(format.energy(metrics.activeKcal)) to R.string.widget_detail_calories,
            res.format(format.minutes(metrics.activeMinutes)) to R.string.widget_detail_active,
            res.format(format.minutes(metrics.briskMinutes)) to R.string.widget_detail_brisk,
        ).map { (value, label) -> value to context.getString(label) }
    }

    fun detailValueColumn(lines: List<Pair<String, String>>): Dp =
        lines.maxOf { measureWidgetText(context, it.first, FACT_SP, TextWeight.MEDIUM) } + DetailValueGap

    /** The day in figures where [width] holds every line of it whole, figure and words; else none. */
    fun details(width: Dp): Boolean {
        if (!look.showDetails) return false
        val lines = detailLines()
        val words = lines.maxOf { measureWidgetText(context, it.second, DETAIL_LABEL_SP) }.withSlack()
        return detailValueColumn(lines) + words <= width
    }

    /** «Steps today», «Today» where that does not fit [room], nothing where neither does. */
    fun eyebrow(room: Dp): String? = listOf(false, true)
        .map { eyebrowText(context, short = it) }
        .firstOrNull { measureWidgetText(context, it, FACT_SP).withSlack() <= room }

    /**
     * The goal as [room] holds it whole: «84% of 10,000», else «84%», each with the check mark
     * once it is met, else the share alone (past 100% it says the goal is met); null where not
     * even that fits.
     */
    fun goal(room: Dp): GoalShown? {
        val mark = markSize(scale) + MarkGap
        fun width(text: String) = measureWidgetText(context, text, FACT_SP, TextWeight.MEDIUM).withSlack()
        val share = goalShareText(context, overview, format)
        val percent = goalPercentText(overview, format)
        val tries = listOf(share to reached, percent to reached, percent to false).distinct()
        return tries.firstOrNull { (text, marked) -> width(text) + (if (marked) mark else 0.dp) <= room }
            ?.let { (text, marked) -> GoalShown(text, marked) }
    }
}

/** The goal as a card prints it: the words, and whether the check mark goes before them. */
private data class GoalShown(val text: String, val marked: Boolean)

// --- Lines -------------------------------------------------------------------------------------

@Composable
private fun Eyebrow(parts: WordsParts, room: Dp) {
    val text = parts.eyebrow(room) ?: return
    Text(
        text = text,
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
private fun SentenceText(parts: WordsParts, text: String?, lines: Int, align: TextAlign, column: Dp) {
    if (text == null || lines <= 0) return
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

/** Rank 3's figures: Medium, in the strong ink. */
private fun factStyle(parts: WordsParts, align: TextAlign = TextAlign.Start) = TextStyle(
    color = parts.palette.primaryInk,
    fontSize = FACT_SP.sp,
    fontWeight = FontWeight.Medium,
    textAlign = align,
)

/**
 * The goal as a fact, with a check before it once it is met: a mark at the line's own size,
 * in the line's own ink, which is punctuation rather than a drawing. The air after it is a wrapper's padding, never the image's own: an `Image`
 * scales its drawing into what its padding leaves (20 Sep 2026).
 */
@Composable
private fun GoalFact(parts: WordsParts, room: Dp) {
    val goal = parts.goal(room) ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (goal.marked) {
            Box(modifier = GlanceModifier.padding(end = MarkGap)) {
                Image(
                    provider = ImageProvider(R.drawable.widget_mark_check),
                    contentDescription = parts.context.getString(R.string.widget_goal_reached_mark),
                    colorFilter = ColorFilter.tint(parts.palette.primaryInk),
                    modifier = GlanceModifier.size(markSize(parts.scale)),
                )
            }
        }
        Text(text = goal.text, style = factStyle(parts), maxLines = 1)
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
    val lines = parts.detailLines().take(rows)
    val valueColumn = parts.detailValueColumn(lines)
    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = DetailGap)) {
        lines.forEach { (value, label) ->
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, style = factStyle(parts), maxLines = 1, modifier = GlanceModifier.width(valueColumn))
                Text(
                    text = label,
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
    val metrics = parts.metrics(column)
    val (plan, slot) = parts.fitSlot(
        column,
        plan = { rowColumnPlan(size, parts.scale, it, parts.look.showGoal, metrics) },
        lines = { it.sentenceLines },
    )
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.width(leading)) {
            Eyebrow(parts, leading)
            Count(parts, hero)
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = GlanceModifier.padding(start = ColumnGap).defaultWeight(),
        ) {
            SentenceText(parts, slot, plan.sentenceLines, TextAlign.End, column)
            if (plan.goal) GoalFact(parts, column)
            if (plan.metrics) MetricsFact(parts, TextAlign.End)
        }
    }
}

/** Tall and narrow: the eyebrow on top, the rest on the bottom edge, the air between them. */
@Composable
private fun StackContent(parts: WordsParts, size: DpSize) {
    val width = size.width - WidgetCardPadding * 2
    val metrics = parts.metrics(width)
    val (plan, slot) = parts.fitSlot(
        width,
        plan = {
            stackPlan(
                size,
                parts.scale,
                parts.heroEm,
                it,
                goal = parts.look.showGoal,
                metrics = metrics,
                details = parts.details(width),
            )
        },
        lines = { it.column.sentenceLines },
    )
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Eyebrow(parts, width)
        Spacer(modifier = GlanceModifier.defaultWeight())
        Count(parts, plan.heroSp)
        SentenceText(parts, slot, plan.column.sentenceLines, TextAlign.Start, width)
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
    val metrics = parts.metrics(sentenceColumn)
    val (plan, slot) = parts.fitSlot(
        sentenceColumn,
        plan = {
            panelPlan(
                size,
                parts.scale,
                parts.heroEm,
                it,
                goal = parts.look.showGoal,
                metrics = metrics,
                details = parts.details(width),
            )
        },
        lines = { it.column.sentenceLines },
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
                    SentenceText(parts, slot, plan.column.sentenceLines, TextAlign.End, sentenceColumn)
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
