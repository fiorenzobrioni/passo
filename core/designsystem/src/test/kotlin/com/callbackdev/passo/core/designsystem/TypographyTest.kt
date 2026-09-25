package com.callbackdev.passo.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.callbackdev.passo.core.designsystem.theme.familyFor
import com.callbackdev.passo.core.designsystem.theme.passoType
import com.callbackdev.passo.core.designsystem.theme.passoTypography
import com.callbackdev.passo.core.model.AppFont
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The typeface setting reaches every line of the app (Chiaro's TypographyFamilyTest): Material's
 * roles are found by reflection, so a role added by a new Material version and not copied would
 * fail here instead of setting one line of the app in the platform face.
 */
class TypographyTest {
    @Test
    fun `every Material role is set in the chosen family`() {
        for (font in AppFont.entries) {
            val typography = passoTypography(font)
            val roles = Typography::class.java.methods
                .filter {
                    it.parameterCount == 0 && it.returnType == TextStyle::class.java && it.name.startsWith("get")
                }
                .filterNot { it.name.contains("Emphasized") }
            assertThat(roles).isNotEmpty()
            for (role in roles) {
                val style = role.invoke(typography) as TextStyle
                assertWithMessage("${font.name}.${role.name}").that(style.fontFamily).isEqualTo(familyFor(font))
            }
        }
    }

    @Test
    fun `the hero and the reading are tabular, in the chosen family`() {
        for (font in AppFont.entries) {
            val type = passoType(font)
            assertThat(type.heroNumber.fontFamily).isEqualTo(familyFor(font))
            assertThat(type.heroNumber.fontFeatureSettings).isEqualTo("tnum")
            assertThat(type.readingValue.fontFeatureSettings).isEqualTo("tnum")
        }
    }
}
