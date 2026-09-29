// What the UI tests share (PLANNING.md §12): the accessibility checks every screen's tests run.
// Test code only: modules take it as `testImplementation`, and nothing here reaches the app.
plugins {
    alias(libs.plugins.passo.android.library)
    alias(libs.plugins.passo.android.compose)
}

android {
    namespace = "com.callbackdev.passo.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.truth)
    implementation(project(":core:designsystem"))

    testImplementation(libs.androidx.junit)
}
