package com.callbackdev.passo.widget

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.passo.core.designsystem.theme.WidgetDress
import com.callbackdev.passo.core.designsystem.theme.widgetCardContainer
import com.callbackdev.passo.core.designsystem.theme.widgetDress
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.tracking.TrackingControl
import java.util.Locale
import kotlin.math.floor

/*
 * The widgets' side of the design system: Chiaro's card (`WidgetUi.kt`), carried over so a
 * Passo card and a Chiaro card on one home screen are the same piece of furniture. The same
 * 24 dp corner, the same insets, the same grounds and the same inks, the same type scale; the
 * system face, because a launcher draws `RemoteViews` in it whatever the app's own setting is.
 */

/** The dress a card writes in: the reader's palette, or the wallpaper's schemes when they asked. */
fun widgetDressFor(context: Context, settings: UserSettings): WidgetDress = if (settings.dynamicColor) {
    widgetDress(settings.palette, dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
} else {
    widgetDress(settings.palette)
}

/**
 * The inks and marks a card draws with, resolved once for the ground it really has.
 *
 * @property accent the ring's arc and the current hour's bar.
 * @property track the ring's track and the hours still to come: the ink, faint, so it reads on
 *   any ground, a see-through card over a wallpaper included.
 * @property goal a met goal, Chiaro's pass ink for this ground.
 * @property halo the card's own ground, opaque, drawn under a mark that crosses the arc; null on
 *   a see-through card, whose ground is the wallpaper.
 */
data class WidgetPalette(
    val primary: Color,
    val secondary: Color,
    val attention: Color,
    val accent: Color,
    val track: Color,
    val goal: Color,
    val halo: Color?,
    val darkGround: Boolean,
) {
    val primaryInk: ColorProvider get() = ColorProvider(primary)
    val secondaryInk: ColorProvider get() = ColorProvider(secondary)
    val attentionInk: ColorProvider get() = ColorProvider(attention)
}

/** [widgetInk]'s answer, dressed in the colours it names. */
fun widgetPalette(context: Context, look: WidgetLook, dress: WidgetDress): WidgetPalette {
    val night = isNight(context)
    val delegates = look.background == WidgetBackground.SYSTEM || look.background == WidgetBackground.COLOR
    val bright = look.opacityPct < INK_TRUST_FLOOR_PCT && delegates && wallpaperWantsDarkInk(context)
    val ink = widgetInk(look.background, look.opacityPct, night, bright)
    val solid = look.opacityPct >= INK_TRUST_FLOOR_PCT
    val ground = cardGround(look, dress, night)
    return when (ink) {
        WidgetInk.OVER_COLOR -> WidgetPalette(
            primary = Color.White,
            secondary = Color.White.copy(alpha = QUIET_ALPHA),
            attention = Color.White.copy(alpha = ATTENTION_ALPHA),
            accent = Color.White,
            track = Color.White.copy(alpha = TRACK_ALPHA),
            goal = dress.darkColors.goal,
            halo = ground.takeIf { solid },
            darkGround = true,
        )

        WidgetInk.ON_LIGHT, WidgetInk.ON_DARK -> {
            val dark = ink.darkGround
            val scheme = dress.scheme(dark)
            WidgetPalette(
                primary = scheme.onSurface,
                secondary = scheme.onSurfaceVariant,
                attention = dress.colors(dark).attention,
                accent = scheme.primary,
                track = scheme.onSurface.copy(alpha = TRACK_ALPHA),
                goal = dress.colors(dark).goal,
                halo = ground.takeIf { solid },
                darkGround = dark,
            )
        }
    }
}

/** The card's ground at full strength: what [widgetCardFill] thins by the reader's opacity. */
private fun cardGround(look: WidgetLook, dress: WidgetDress, night: Boolean): Color = when (look.background) {
    WidgetBackground.LIGHT -> dress.lightScheme.surface
    WidgetBackground.DARK -> dress.darkScheme.surface
    WidgetBackground.SYSTEM -> dress.scheme(night).surface
    WidgetBackground.COLOR -> widgetCardContainer(look.cardColor)
}

/** The card's fill at the reader's opacity: the ground thins, the ink never does. */
fun widgetCardFill(look: WidgetLook, dress: WidgetDress, night: Boolean): Color =
    cardGround(look, dress, night).copy(alpha = look.opacityPct.coerceIn(0, 100) / 100f)

/**
 * The phone's night mode at render time. Glance's day/night providers are resolved by the
 * launcher, and a host that flips the card without the words leaves dark ink on a dark card
 * (Chiaro, device report of 3 Sep 2026): every colour here is resolved against one answer.
 */
fun isNight(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * Whether the wallpaper behind a see-through card can carry dark text, by the system's own
 * account ([WallpaperColors.HINT_SUPPORTS_DARK_TEXT]). Read as the affirmative signal it is:
 * dark ink only when the system says the ground is bright, light ink whenever it says nothing.
 */
fun wallpaperWantsDarkInk(context: Context): Boolean {
    val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return false
    val colors = runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        ?: runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_LOCK) }.getOrNull()
        ?: return false
    return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
}

/**
 * What a tap does: open the app, which lands on Today; on a paused card, open it asking to
 * resume, as the card says ([TrackingControl.EXTRA_RESUME]). The launcher's own intent, so the
 * tap brings back the app's task rather than starting a second one.
 */
fun widgetTapIntent(context: Context, state: CountingState): Intent? =
    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        if (state == CountingState.PAUSED) putExtra(TrackingControl.EXTRA_RESUME, true)
    }

/**
 * The card every widget lives in: the 24 dp corner, the ground at the reader's opacity, the
 * insets for what sits against each edge, and one door into the app for the whole card.
 */
@Composable
internal fun WidgetCard(
    model: WidgetModel,
    dress: WidgetDress,
    paddingHorizontal: Dp = WidgetCardPadding,
    paddingVertical: Dp = WidgetCardPadding,
    content: @Composable (WidgetPalette) -> Unit,
) {
    val context = LocalContext.current
    val palette = remember(model.look, dress) { widgetPalette(context, model.look, dress) }
    val tap = widgetTapIntent(context, model.state)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(WidgetCorner)
            .then(if (tap != null) GlanceModifier.clickable(actionStartActivity(tap)) else GlanceModifier),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(widgetCardFill(model.look, dress, isNight(context)))),
        ) {}
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        ) {
            content(palette)
        }
    }
}

/**
 * The model this widget draws, re-read inside the composition whenever [WidgetRefresh] ticks.
 * [initial] is what `provideGlance` loaded at revision [loadedAt], so the first frame costs
 * nothing more and only a real change reloads.
 */
@Composable
internal fun rememberWidgetModel(initial: WidgetModel, loadedAt: Long, reload: suspend () -> WidgetModel): WidgetModel {
    val revision by WidgetRefresh.revision.collectAsState()
    val model by produceState(initial, revision) {
        if (revision != loadedAt) value = reload()
    }
    return model
}

/**
 * The card with nothing to count yet, or nowhere to count it: one message, centred on the card,
 * with the tap that fixes it where there is one (Chiaro's empty states).
 */
@Composable
internal fun MessageContent(title: String, hint: String?, palette: WidgetPalette) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = title,
            style = TextStyle(color = palette.primaryInk, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 2,
        )
        if (hint != null) {
            Text(text = hint, style = TextStyle(color = palette.secondaryInk, fontSize = 12.sp), maxLines = 2)
        }
    }
}

/**
 * The footnote that says the count is not moving, on a card with no place for a sentence: the
 * whole phrase where [room] holds it, the word alone where it does not (Chiaro's stale marker,
 * in the same freshness ink, on a line no budget may drop).
 */
@Composable
internal fun StatusFootnote(state: CountingState, palette: WidgetPalette, room: Dp) {
    val context = LocalContext.current
    val long = statusText(context, state) ?: return
    val text = if (measureWidgetText(context, long, STATUS_SP, TextWeight.MEDIUM).withSlack() <= room) {
        long
    } else {
        statusText(context, state, short = true) ?: return
    }
    Text(
        text = text,
        style = TextStyle(color = palette.attentionInk, fontSize = STATUS_SP.sp, fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
}

/** The locale a card formats in, off the context Glance composes with. */
internal fun Context.widgetLocale(): Locale = resources.configuration.locales[0]

internal fun fontScale(context: Context): Float = context.resources.configuration.fontScale

/**
 * The width one line of text really takes, measured with the face, size and weight the launcher
 * will draw it in (Chiaro's `measureWidgetText`): Glance cannot measure, this process can.
 */
internal fun measureWidgetText(
    context: Context,
    text: String,
    sizeSp: Float,
    weight: TextWeight = TextWeight.REGULAR,
): Dp {
    val metrics = context.resources.displayMetrics
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = weight.typeface
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
    }
    return (paint.measureText(text) / metrics.density).dp
}

/**
 * How many lines [text] takes at [width], laid out as a `TextView` would lay it out: measured,
 * where Chiaro's first budgets reserved the most a sentence could take and left the rest as air.
 */
internal fun measureWidgetLines(context: Context, text: String, sizeSp: Float, width: Dp, weight: TextWeight): Int {
    val metrics = context.resources.displayMetrics
    val widthPx = (width.value * metrics.density).toInt()
    if (widthPx <= 0 || text.isEmpty()) return if (text.isEmpty()) 0 else Int.MAX_VALUE
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = weight.typeface
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
    }
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx).setIncludePad(true).build().lineCount
}

/** The three weights a card sets text in, and the faces Glance's weights resolve to on the launcher. */
internal enum class TextWeight {
    REGULAR,
    MEDIUM,
    BOLD,
    ;

    val typeface: Typeface
        get() = when (this) {
            REGULAR -> Typeface.DEFAULT
            MEDIUM -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            BOLD -> Typeface.DEFAULT_BOLD
        }
}

/** A measured width, given the few dp a launcher on another face may want more of. */
internal fun Dp.withSlack(): Dp = this + RowFitSlack

/**
 * The height one line of text occupies in a Glance `Text` (font padding on): about 1.32 em for
 * the system font, times the reader's font scale. An estimate, named as one (Chiaro's).
 */
internal fun textLineHeight(fontSizeSp: Float, fontScale: Float): Dp = (fontSizeSp * LINE_BOX_EM * fontScale).dp

/** [textLineHeight] read backwards: the largest size whose line fits [room]. Never negative. */
internal fun textSizeForLine(room: Dp, fontScale: Float): Float =
    (room.value / (LINE_BOX_EM * fontScale.coerceAtLeast(0.1f))).coerceAtLeast(0f)

/** Lines of [sizeSp] that fit in [room], at least 0. */
internal fun linesThatFit(room: Dp, sizeSp: Float, fontScale: Float): Int =
    (room / textLineHeight(sizeSp, fontScale)).toInt().coerceAtLeast(0)

/** Rounds a size down to a quarter of a point, so a number that grows with the card does not jitter. */
internal fun quarterPoint(sp: Float): Float = floor(sp * 4f) / 4f

internal const val LINE_BOX_EM = 1.32f

/** Chiaro's card: the corner, the words' inset, and the snug inset of a one-row card. */
internal val WidgetCorner = 24.dp
internal val WidgetCardPadding = 14.dp
internal val WidgetCardPaddingSnug = 6.dp

/** The air a measured width is given before it is used as one. */
internal val RowFitSlack = 4.dp

/** The household's type (Chiaro's): the hero is Bold and sized per form; a fact is 16 sp. */
internal const val FACT_SP = 16f

/** Chiaro's stale marker is 11 sp; this one carries an action («tap to resume»), so a step up, Medium. */
internal const val STATUS_SP = 12f

private const val QUIET_ALPHA = 0.75f
private const val ATTENTION_ALPHA = 0.85f
private const val TRACK_ALPHA = 0.2f

/**
 * The width of [text] in ems of the system face at [weight]: independent of the density and of
 * the reader's font scale, which the layouts apply themselves.
 */
internal fun textEm(text: String, weight: TextWeight): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = weight.typeface
        textSize = EM_PROBE_PX
    }
    return paint.measureText(text) / EM_PROBE_PX
}

private const val EM_PROBE_PX = 100f

/**
 * The band of empty leading a block of words carries above its capitals, so the ring beside it
 * can balance the same band underneath (Chiaro's `textInkBalance`: centre a block against a
 * drawing and the drawing reads high by half that band).
 */
internal fun textInkBalance(fontSizeSp: Float, fontScale: Float): Dp = (fontSizeSp * LEADING_ABOVE_CAPS * fontScale).dp

private const val LEADING_ABOVE_CAPS = 0.24f

/**
 * The narrowest width at which [text] still takes the lines it takes at [width]: a sentence set
 * at that width breaks into lines of even length rather than a full line and an orphan
 * («1,484 steps ahead of / usual»). What `TextView`'s balanced break strategy would do, which a
 * `RemoteViews` cannot ask for. [width] itself when the text fits one line or is cut anyway.
 */
internal fun balancedWidth(
    context: Context,
    text: String,
    sizeSp: Float,
    width: Dp,
    weight: TextWeight,
    maxLines: Int,
): Dp {
    val lines = measureWidgetLines(context, text, sizeSp, width, weight)
    if (lines < 2 || lines > maxLines) return width
    var low = width / 2
    var high = width
    while (high - low > 1.dp) {
        val mid = (low + high) / 2
        if (measureWidgetLines(context, text, sizeSp, mid, weight) == lines) high = mid else low = mid
    }
    return minOf(width, high + RowFitSlack)
}
