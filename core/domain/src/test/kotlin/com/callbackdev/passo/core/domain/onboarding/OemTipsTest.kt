package com.callbackdev.passo.core.domain.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OemTipsTest {
    @Test
    fun `known manufacturers get the tip`() {
        assertThat(OemTips.needsTip("Xiaomi")).isTrue()
        assertThat(OemTips.needsTip("POCO")).isTrue()
        assertThat(OemTips.needsTip("HMD Global")).isTrue()
        assertThat(OemTips.needsTip(" OnePlus ")).isTrue()
    }

    @Test
    fun `phones without a known battery manager get no tip`() {
        assertThat(OemTips.needsTip("Google")).isFalse()
        assertThat(OemTips.needsTip("Fairphone")).isFalse()
        assertThat(OemTips.needsTip("")).isFalse()
    }

    @Test
    fun `samsung phones get no tip, since One UI 6 leaves typed foreground services alone`() {
        assertThat(OemTips.needsTip("samsung")).isFalse()
        assertThat(OemTips.needsTip("Samsung")).isFalse()
    }
}
