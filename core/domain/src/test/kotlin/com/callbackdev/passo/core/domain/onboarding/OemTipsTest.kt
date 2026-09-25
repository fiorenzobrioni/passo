package com.callbackdev.passo.core.domain.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OemTipsTest {
    @Test
    fun `known manufacturers get their page`() {
        assertThat(OemTips.slugFor("Xiaomi")).isEqualTo("xiaomi")
        assertThat(OemTips.slugFor("POCO")).isEqualTo("xiaomi")
        assertThat(OemTips.slugFor("samsung")).isEqualTo("samsung")
        assertThat(OemTips.slugFor("HMD Global")).isEqualTo("nokia")
        assertThat(OemTips.pageFor("samsung")).isEqualTo("https://dontkillmyapp.com/samsung")
    }

    @Test
    fun `phones without a known battery manager get no tip`() {
        assertThat(OemTips.slugFor("Google")).isNull()
        assertThat(OemTips.slugFor("Fairphone")).isNull()
        assertThat(OemTips.slugFor("")).isNull()
    }
}
