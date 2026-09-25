plugins {
    alias(libs.plugins.passo.android.application)
    alias(libs.plugins.passo.android.compose)
    alias(libs.plugins.passo.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The version lives in gradle.properties; versionCode follows from it (PLANNING.md §11 Phase 8),
// so the two can never disagree. A pre-release suffix (0.2.0-rc1) shares its final's code.
val passoVersionName = providers.gradleProperty("passo.versionName").get()
val passoVersionCode =
    passoVersionName.substringBefore('-').split('.').map(String::toInt).let { (major, minor, patch) ->
        major * 10_000 + minor * 100 + patch
    }

android {
    namespace = "com.callbackdev.passo"

    defaultConfig {
        applicationId = "com.callbackdev.passo"
        versionCode = passoVersionCode
        versionName = passoVersionName
    }

    // Passo speaks English and Italian (VISION.md). Without this the APK would also carry every
    // language the AndroidX libraries ship, and their system strings would mix into a screen
    // that is otherwise in one of the two.
    androidResources {
        localeFilters += listOf("en", "it")
    }

    signingConfigs {
        // Shared debug keystore, committed on purpose (see CLAUDE.md): debug builds from CI and
        // from any machine carry one signature and can update each other's installs.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "passo-debug"
            keyPassword = "android"
        }

        // Release key. The keystore lives OUTSIDE the repo; the four properties come from
        // ~/.gradle/gradle.properties locally and from ORG_GRADLE_PROJECT_* env vars in the
        // release workflow. Created only when fully configured, so a clean checkout still builds.
        val releaseStore = findProperty("PASSO_KEYSTORE") as String?
        val releaseStorePassword = findProperty("PASSO_KEYSTORE_PASSWORD") as String?
        val releaseKeyAlias = findProperty("PASSO_KEY_ALIAS") as String?
        val releaseKeyPassword = findProperty("PASSO_KEY_PASSWORD") as String?
        if (!releaseStore.isNullOrBlank() && !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = rootProject.file(releaseStore)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Different app id, so the dev build installs side by side with the release one.
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // The release key wins whenever configured. Otherwise -PsignReleaseWithDebugKey signs
            // the minified build with the debug key so it can be smoke-tested (R8 breakage shows
            // up nowhere else). Opt-in, so an unconfigured checkout can never produce an
            // installable release by accident.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
                    .takeIf { project.hasProperty("signReleaseWithDebugKey") }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:tracking"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:today"))
    implementation(project(":feature:history"))
    implementation(project(":feature:insights"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:sessions"))
    implementation(project(":widget"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
