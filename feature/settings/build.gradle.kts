plugins {
    alias(libs.plugins.passo.android.feature)
}

android {
    namespace = "com.callbackdev.passo.feature.settings"
}

dependencies {
    implementation(project(":core:tracking"))
    implementation(libs.androidx.core.ktx)
    // The notification permission, asked when a goal notification is turned on.
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
}
