// The home-screen widget (Jetpack Glance, PLANNING.md §7): its receiver, the responsive
// layouts and the update coordinator. Glance comes in with the widget itself, in Phase 4.
plugins {
    alias(libs.plugins.passo.android.library)
    alias(libs.plugins.passo.android.hilt)
}

android {
    namespace = "com.callbackdev.passo.widget"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
}
