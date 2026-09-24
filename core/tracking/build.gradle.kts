// The step tracking engine's Android half: StepTrackingService (foreground service of type
// health), the sensor source, the boot/shutdown/screen receivers and the ongoing notification
// (PLANNING.md §4). The accounting itself is pure Kotlin, in core:domain.
plugins {
    alias(libs.plugins.passo.android.library)
    alias(libs.plugins.passo.android.hilt)
}

android {
    namespace = "com.callbackdev.passo.core.tracking"
}

dependencies {
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)
}
