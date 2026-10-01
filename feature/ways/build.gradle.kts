// The Ways page and each way's page (PLANNING.md §11 Phase 11).
plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.ways"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
