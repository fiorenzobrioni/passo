plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.guide"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
