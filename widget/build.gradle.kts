// The home-screen widgets (Jetpack Glance, PLANNING.md §7): «At a glance» and «In words», their
// receivers, the per-widget settings screen, and the update coordinator the tracking service
// reports to. The card, its inks and its colours are Chiaro's, so the two apps' widgets sit
// side by side as one family (docs/adr/0005-widgets.md).
plugins {
    alias(libs.plugins.passo.android.library)
    alias(libs.plugins.passo.android.compose)
    alias(libs.plugins.passo.android.hilt)
}

android {
    namespace = "com.callbackdev.passo.widget"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:tracking"))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.androidx.junit)
}
