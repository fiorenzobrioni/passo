package com.callbackdev.passo.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * The platform levels, in one place (VISION.md, Key decisions; PLANNING.md §1).
 *
 * minSdk 34 (Android 14): foreground service types, the `health` type and per-app languages
 * are all native, so no legacy code path exists anywhere in the app. 37 is Android 17.
 */
object PassoSdk {
    const val MIN = 34
    const val TARGET = 37
    const val COMPILE = 37
}

/**
 * Java 21 everywhere (PLANNING.md §1), set as source/target levels rather than through a
 * Gradle toolchain: a toolchain would make every build look for, or download, a separate
 * JDK 21, when the JDK running Gradle (Android Studio's own, or CI's) already is one.
 */
internal val PassoJavaVersion = JavaVersion.VERSION_21
internal val PassoJvmTarget = JvmTarget.JVM_21

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()
