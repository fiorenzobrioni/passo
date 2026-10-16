package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import com.callbackdev.passo.core.designsystem.theme.PassoMotion

/**
 * Scrolls the least that brings the item [key] whole into view, on the page's spatial spring:
 * what the reader just did shows where they did it, and the page moves only as far as that
 * takes. An item taller than the list keeps its top in view. An item not laid out (scrolled away)
 * is first brought in from [index] (the page's top, say). Under reduced motion the page jumps.
 */
suspend fun LazyListState.reveal(key: Any, index: Int, reduced: Boolean) {
    // The item that just changed is measured on the next frame, not before.
    withFrameNanos { }
    if (layoutInfo.visibleItemsInfo.none { it.key == key }) {
        if (reduced) scrollToItem(index) else animateScrollToItem(index)
        withFrameNanos { }
    }
    val layout = layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.key == key } ?: return
    val top = layout.viewportStartOffset + layout.beforeContentPadding
    val bottom = layout.viewportEndOffset - layout.afterContentPadding
    val delta = when {
        item.offset < top -> item.offset - top
        item.offset + item.size > bottom -> minOf(item.offset + item.size - bottom, item.offset - top)
        else -> return
    }.toFloat()
    if (reduced) scrollBy(delta) else animateScrollBy(delta, PassoMotion.spatial())
}
