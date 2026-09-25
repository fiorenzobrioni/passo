plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.insights"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
