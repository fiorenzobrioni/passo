package com.callbackdev.passo.widget

/**
 * **The widgets' type scale** (13 Oct 2026, the family's widget review): the sizes a
 * home-screen card prints in, by role, so a fact, a footnote or a label is the same size on
 * every card of the family. Six steps, each a role:
 *
 * - [CAPTION]: read last, a chart's hour labels.
 * - [LABEL]: a row's label and a footnote that carries an action.
 * - [BODY]: the second thing read, a fact and a sentence.
 * - [TITLE]: the figure of a card whose hero is a drawing.
 * - [DISPLAY]: a hero wider than a count.
 * - [HERO]: the count on a one-row card; where a form sizes its number to the grant (the tall
 *   card, the words card, the figure inside the ring) the grant decides, and those sizes are
 *   not steps.
 *
 * One size stays off the scale on purpose: the words card's sentence at 18 sp
 * ([WORDS_SENTENCE_SP]). On a card with no drawing the sentence is the second thing read,
 * and 17 over a 16 sp fact is not a rank. `WidgetTypeTest` holds every fixed size to a step.
 */
internal object WidgetType {
    const val CAPTION = 11f
    const val LABEL = 13f
    const val BODY = 16f
    const val TITLE = 22f
    const val DISPLAY = 30f
    const val HERO = 34f

    /** Every step, for the test that holds the cards to them. */
    val steps: Set<Float> = setOf(CAPTION, LABEL, BODY, TITLE, DISPLAY, HERO)
}
