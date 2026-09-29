package com.callbackdev.passo.core.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.components.DENSE_TARGETS_TAG
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The checks catch what they claim to, and let through what Material makes right. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class AccessibilityChecksTest {
    @get:Rule val compose = createComposeRule()

    private fun problems(): String? = runCatching { compose.assertAccessible() }.exceptionOrNull()?.message

    @Test
    fun `a lone small control passes on the touch area Compose widens it to`() {
        compose.setContent { Box(Modifier.size(32.dp).clickable {}.semantics { contentDescription = "Alone" }) }

        assertThat(problems()).isNull()
    }

    @Test
    fun `small controls side by side are caught`() {
        compose.setContent {
            Row {
                Box(Modifier.size(32.dp).clickable {}.semantics { contentDescription = "Left" })
                Box(Modifier.size(32.dp).clickable {}.semantics { contentDescription = "Right" })
            }
        }

        // Each widened to 48dp, they overlap by 16dp; halved, each keeps 40.
        assertThat(problems()).contains("«Left»: 40 by 48dp for a finger")
    }

    @Test
    fun `buttons that share a border, as Material's segmented ones do, pass`() {
        compose.setContent {
            Row {
                Box(Modifier.size(96.dp, 40.dp).clickable {}.semantics { contentDescription = "Week" })
                Box(
                    Modifier.offset(x = (-1).dp).size(96.dp, 40.dp).clickable {}.semantics {
                        contentDescription =
                            "Month"
                    },
                )
            }
        }

        assertThat(problems()).isNull()
    }

    @Test
    fun `a control with nothing to read is caught`() {
        compose.setContent { Box(Modifier.size(48.dp).clickable {}) }

        assertThat(problems()).contains("A control with nothing to read")
    }

    @Test
    fun `a small icon button passes on the touch bounds Material gives it`() {
        compose.setContent {
            IconButton(onClick = {}) { Icon(ImageVector.Builder("x", 24.dp, 24.dp, 24f, 24f).build(), "Settings") }
        }

        assertThat(problems()).isNull()
    }

    @Test
    fun `dense data is measured for its label, not its size`() {
        compose.setContent {
            Column {
                Row(Modifier.testTag(DENSE_TARGETS_TAG)) {
                    Box(Modifier.size(12.dp).clickable {}.semantics { contentDescription = "Monday" })
                    Box(Modifier.size(12.dp).clickable {}.semantics { contentDescription = "Tuesday" })
                    Box(Modifier.size(12.dp).clickable {})
                }
                Text("A page")
            }
        }

        val found = problems()
        assertThat(found).doesNotContain("Monday")
        assertThat(found).contains("A control with nothing to read")
    }
}
