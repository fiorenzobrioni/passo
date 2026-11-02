package com.callbackdev.passo.feature.year

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.padding
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.year.YearPage
import kotlinx.coroutines.launch

/** Test tags of the year's pages. */
object YearTags {
    const val PAGE = "year_page"
    const val PROGRESS = "year_progress"
    const val SHARE = "year_share"
    const val SAVE = "year_save"
}

/** The pages of [year], from Insights, History's year or Today's card in season. */
@Composable
fun YearRoute(year: Int, onBack: () -> Unit, viewModel: YearViewModel = hiltViewModel()) {
    LaunchedEffect(year) { viewModel.open(year) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    YearScreen(state = state?.takeIf { it.year == year }, onBack = onBack)
}

/**
 * Your year on foot (PLANNING.md §11 Phase 12): full-screen pages, one thing each. A touch goes
 * on (on the leading third, back), a swipe goes back or on; the marks at the top say where the
 * reader is. Under reduced motion a page arrives by a fade. TalkBack reads each page as one
 * sentence, with "Next page" and "Previous page" as its actions; at twice the text size a page
 * scrolls. Each page can be shared or saved as an image, drawn from what it shows.
 */
@Composable
fun YearScreen(state: YearUiState?, onBack: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        val review = state?.review
        when {
            state == null -> Box(Modifier.fillMaxSize())
            review == null -> NotOffered(onBack)
            else -> Pages(state, onBack)
        }
    }
}

@Composable
private fun NotOffered(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CloseButton(onBack)
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.year_not_offered),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Pages(state: YearUiState, onBack: () -> Unit) {
    val review = checkNotNull(state.review)
    val pages = review.pages
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val reduced = reducedMotion()
    val context = LocalContext.current
    val format = rememberMeasureFormatter(state.units)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val layer = rememberGraphicsLayer()
    val ground = MaterialTheme.colorScheme.surface.toArgb()
    val ink = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val signature = stringResource(R.string.year_signature)
    val saved = stringResource(R.string.year_saved)
    val saveFailed = stringResource(R.string.year_save_failed)
    val chooser = stringResource(R.string.year_share_chooser)
    var pendingSave by remember { mutableStateOf<Bitmap?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PNG)) { uri: Uri? ->
        val image = pendingSave
        pendingSave = null
        if (uri != null && image != null) {
            scope.launch {
                val ok = YearImage.save(context, image, uri)
                Toast.makeText(context, if (ok) saved else saveFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun go(to: Int) {
        val target = to.coerceIn(0, pages.lastIndex)
        if (target == pager.currentPage) return
        scope.launch { if (reduced) pager.scrollToPage(target) else pager.animateScrollToPage(target) }
    }

    suspend fun image(): Bitmap = YearImage.compose(
        page = layer.toImageBitmap().asAndroidBitmap(),
        ground = ground,
        ink = ink,
        signature = signature,
    )

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(pageGutter())) {
            CloseButton(onBack)
            Progress(
                current = pager.currentPage,
                count = pages.size,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                // What the page draws, recorded for its image; the bars and the buttons stay out.
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
                .pointerInput(pages.size, rtl) {
                    detectTapGestures { offset ->
                        val leading = if (rtl) offset.x > size.width * 2 / 3f else offset.x < size.width / 3f
                        go(if (leading) pager.currentPage - 1 else pager.currentPage + 1)
                    }
                },
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
                val page = pages[index]
                val words = pageWords(page, review, format)
                val eyebrow = yearEyebrow(review)
                // The first page says whose year it is, and since when; the others are its pages.
                val spoken = if (index == 0) "$eyebrow. ${words.sentence}" else words.sentence
                val next = stringResource(R.string.year_next)
                val previous = stringResource(R.string.year_previous)
                val fade = remember { Animatable(if (reduced) 0f else 1f) }
                LaunchedEffect(pager.currentPage == index, reduced) {
                    if (!reduced) {
                        fade.snapTo(1f)
                    } else if (pager.currentPage == index) {
                        fade.animateTo(1f, PassoMotion.fade())
                    } else {
                        fade.snapTo(0f)
                    }
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = fade.value }
                        .verticalScroll(rememberScrollState())
                        .then(if (pager.currentPage == index) Modifier.testTag(YearTags.PAGE) else Modifier)
                        .padding(pageGutter())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
                            contentDescription = spoken
                            heading()
                            customActions = listOfNotNull(
                                CustomAccessibilityAction(next) {
                                    go(index + 1)
                                    true
                                }.takeIf { index < pages.lastIndex },
                                CustomAccessibilityAction(previous) {
                                    go(index - 1)
                                    true
                                }.takeIf { index > 0 },
                            )
                        },
                    ) {
                        PageText(words, eyebrow)
                        PageVisual(page, state, format)
                        if (index == 0) {
                            Text(
                                stringResource(R.string.year_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(pageGutter()).padding(16.dp),
        ) {
            TextButton(
                onClick = {
                    scope.launch {
                        pendingSave = image()
                        runCatching { save.launch(YearImage.fileName(state.year, pager.currentPage)) }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag(YearTags.SAVE),
            ) { Text(stringResource(R.string.year_save)) }
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val uri = YearImage.cache(context, image(), YearImage.fileName(state.year, pager.currentPage))
                        uri?.let { YearImage.share(context, it, chooser) }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag(YearTags.SHARE),
            ) {
                Icon(PassoIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.year_share))
            }
        }
    }
}

@Composable
private fun CloseButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(PassoIcons.Close, contentDescription = stringResource(R.string.year_close))
    }
}

/** The marks at the top: one a page, the ones read filled. */
@Composable
private fun Progress(current: Int, count: Int, modifier: Modifier) {
    val label = stringResource(R.string.year_progress, current + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag(YearTags.PROGRESS).semantics { contentDescription = label },
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
    }
}

private const val PNG = "image/png"
