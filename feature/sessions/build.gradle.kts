// The Outings page and the outing editor (PLANNING.md §11 Phase 10).
plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.sessions"
}

dependencies {
    implementation(project(":core:tracking"))
    implementation(libs.androidx.core.ktx)
    // The notification permission, asked when the first outing starts without it.
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
}
