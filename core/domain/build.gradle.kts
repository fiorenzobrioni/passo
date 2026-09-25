// Pure Kotlin/JVM, and it must stay that way: StepAccountant, the metric calculators, streaks,
// records, WalkDetector, TypicalDayCalculator, the backup format. All business logic lives here
// and is tested on the JVM without Android (PLANNING.md §2, §12).
plugins {
    alias(libs.plugins.passo.jvm.library)
    // The backup file (Phase 7): kotlinx.serialization, already in the catalog for the
    // navigation routes, pure Kotlin like the rest of this module.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
