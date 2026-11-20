plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.year"
}

dependencies {
    // A page shared as an image goes through the shared provider (`:core:designsystem`).
    implementation(libs.androidx.core.ktx)
    // Saving a page where the reader chooses: the Storage Access Framework's launcher.
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
}
