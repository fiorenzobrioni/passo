plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.year"
}

dependencies {
    // FileProvider, for a page shared as an image: androidx.core, already in the app.
    implementation(libs.androidx.core.ktx)
    // Saving a page where the reader chooses: the Storage Access Framework's launcher.
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
}
