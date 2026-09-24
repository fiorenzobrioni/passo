// Pure Kotlin/JVM, and it must stay that way: StepAccountant, the metric calculators, streaks,
// records, WalkDetector, TypicalDayCalculator. All business logic lives here and is tested on
// the JVM without Android (PLANNING.md §2, §12).
plugins {
    alias(libs.plugins.passo.jvm.library)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}
