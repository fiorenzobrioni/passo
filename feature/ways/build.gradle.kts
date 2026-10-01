// The Ways page and each way's page (PLANNING.md §11 Phase 11).
plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.ways"
}

dependencies {
    // A city walk is an outing: started, paused and stopped through the tracking service.
    implementation(project(":core:tracking"))
    implementation(libs.androidx.core.ktx)
    // The notification permission, asked when a walk starts without it.
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
}
