plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.settings"
}

dependencies {
    implementation(project(":core:tracking"))
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.junit)
}
