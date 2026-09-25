// Room (step data, tracker state) and DataStore (settings, profile), behind repositories that
// expose Flow (PLANNING.md §5).
plugins {
    alias(libs.plugins.passo.android.library)
    alias(libs.plugins.passo.android.hilt)
    alias(libs.plugins.passo.android.room)
}

android {
    namespace = "com.callbackdev.passo.core.data"

    // The exported schemas, for the migration tests (MigrationTestHelper reads them as assets).
    sourceSets {
        getByName("test") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
