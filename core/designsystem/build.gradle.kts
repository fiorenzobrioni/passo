// The Material 3 theme, typography and the shared components (progress ring, metric card, the
// Canvas charts), for the app screens (PLANNING.md §2). No chart library: the charts are ours.
plugins {
    alias(libs.plugins.passo.android.library)
    alias(libs.plugins.passo.android.compose)
}

android {
    namespace = "com.callbackdev.passo.core.designsystem"
}

dependencies {
    // The formatter (numbers for the locale) and the units it formats into.
    api(project(":core:domain"))

    // The BOM is exported with the libraries, so a module that reaches Compose through this one
    // (the widget, for GlanceTheme's colors) resolves the same versions.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.androidx.junit)
}
