package com.callbackdev.passo.core.designsystem

import com.callbackdev.passo.core.designsystem.format.unorphan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LinesTest {
    @Test
    fun `a sentence's last two words are held together`() {
        assertThat(unorphan("20 min at a brisk pace")).isEqualTo("20 min at a brisk\u00A0pace")
        assertThat(unorphan("Da 100 passi al minuto: sforzo moderato"))
            .isEqualTo("Da 100 passi al minuto: sforzo\u00A0moderato")
    }

    @Test
    fun `one word or two are left as they are`() {
        assertThat(unorphan("Run")).isEqualTo("Run")
        assertThat(unorphan("Brisk walk")).isEqualTo("Brisk walk")
    }

    @Test
    fun `a pair too long to hold on a line is left to break`() {
        val sentence = "Chiusa dopo una interminabilissima sospensione"
        assertThat(unorphan(sentence)).isEqualTo(sentence)
    }

    @Test
    fun `a number is not parted from its unit`() {
        assertThat(unorphan("5 sets: 3 min slow, 3 min brisk")).isEqualTo("5 sets: 3 min slow, 3\u00A0min\u00A0brisk")
        assertThat(unorphan("30 min running")).isEqualTo("30\u00A0min\u00A0running")
    }

    @Test
    fun `a measure already held together stays whole with the word before it`() {
        assertThat(unorphan("with your step of 73\u00A0cm.")).isEqualTo("with your step of\u00A073\u00A0cm.")
    }
}
