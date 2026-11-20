package com.callbackdev.passo.core.designsystem.share

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/** What the reader shares or saves: said written only when it was. */
@RunWith(AndroidJUnit4::class)
class SharedFilesTest {
    @get:Rule val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a file whose content could not be encoded is not said saved`() = runBlocking {
        val file = folder.newFile("route.gpx")
        val uri = Uri.fromFile(file)

        assertThat(SharedFiles.save(context, uri) { false }).isFalse()
        assertThat(SharedFiles.save(context, uri) { it.writeDocument("<gpx/>") }).isTrue()
        assertThat(file.readText()).isEqualTo("<gpx/>")
    }

    @Test
    fun `an image that could not be encoded is not handed to the share sheet`() = runBlocking {
        assertThat(SharedFiles.cache(context, "page.png") { false }).isNull()
        assertThat(File(context.cacheDir, "share/page.png").exists()).isFalse()
    }
}
