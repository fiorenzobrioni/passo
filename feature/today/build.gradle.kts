plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.today"
}

dependencies {
    implementation(project(":core:tracking"))
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
}
