package com.callbackdev.passo.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * A sentence whose last word is never left alone on a line of its own («…at a brisk / pace»,
 * «…sforzo / moderato»): its last two words are held together, so a line that must break does
 * it one word earlier. Read aloud exactly as before.
 *
 * Not at a large text size, nor for a long pair of words: held together they could be wider than
 * the line itself, and Android would then cut one of them in the middle.
 */
@Composable
fun String.unorphaned(): String = if (LocalDensity.current.fontScale > MAX_FONT_SCALE) this else unorphan(this)

/**
 * [unorphaned]'s rule, apart for its tests. A number is not parted from its unit to do it: in
 * «3 min brisk» the three go together, or the line would end on a bare «3».
 */
fun unorphan(sentence: String): String {
    val last = sentence.lastIndexOf(' ')
    if (last <= 0) return sentence
    val before = sentence.lastIndexOf(' ', last - 1)
    // Two words are a line already: there is nothing before them to break after.
    if (before < 0) return sentence
    val third = sentence.lastIndexOf(' ', before - 1)
    val measured = before > 0 && sentence.substring(third + 1, before).all { it.isDigit() || it in NUMBER_MARKS }
    val from = if (measured) third else before
    if (sentence.length - from - 1 > MAX_HELD) return sentence
    val held = sentence.substring(from + 1).replace(' ', NO_BREAK_SPACE)
    return sentence.substring(0, from + 1) + held
}

private const val NUMBER_MARKS = ".,"
private const val NO_BREAK_SPACE = '\u00A0'

/** The most held together, in characters: about a third of a phone's line. */
private const val MAX_HELD = 20
private const val MAX_FONT_SCALE = 1.3f
