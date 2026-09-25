plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.history"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
