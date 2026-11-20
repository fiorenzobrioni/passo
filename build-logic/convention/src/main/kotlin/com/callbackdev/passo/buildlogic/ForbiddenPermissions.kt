package com.callbackdev.passo.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.xml.parsers.DocumentBuilderFactory

/** Stands for the variant's application id in [AllowedPermissions]: `.debug` differs from the release. */
const val APPLICATION_ID_PLACEHOLDER: String = "\${applicationId}"

/**
 * The permissions Passo holds: the whole list (PLANNING.md §10, docs/adr/0016-location.md
 * decision 2). The merged manifest, the app's own plus every library's, must request exactly
 * these: one more fails the build, and so does one fewer, so the list cannot keep a name that
 * nothing needs any longer.
 *
 * Privacy and battery are enforced here rather than promised. Letting location in made this
 * gate stricter than the list of forbidden names it was: a library that brings any permission
 * at all now turns the build red, until the permission is written here with its reason or
 * stripped in the app manifest with `tools:node="remove"`.
 */
val AllowedPermissions: List<String> =
    listOf(
        // The step counter, and the prerequisite of the `health` foreground service.
        "android.permission.ACTIVITY_RECOGNITION",
        // The tracking and the goals' notifications; optional, asked in context.
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_HEALTH",
        // Counting again after a reboot without being opened.
        "android.permission.RECEIVE_BOOT_COMPLETED",
        // An outing's signals in their own patterns, and the outing as a Live Update.
        "android.permission.VIBRATE",
        "android.permission.POST_PROMOTED_NOTIFICATIONS",
        // An outing on a phone whose step counter cannot wake it (§9.7); WorkManager holds it
        // too, while a widget card is drawn.
        "android.permission.WAKE_LOCK",
        // WorkManager's, which Glance runs its widget sessions on. Without INTERNET it lets
        // nothing leave the phone; Passo's own code never reads it.
        "android.permission.ACCESS_NETWORK_STATE",
        // An outing that asks to be measured with GPS (docs/adr/0016-location.md): asked
        // together, in context; read only while that outing counts, by a service of type
        // `location`. Never ACCESS_BACKGROUND_LOCATION.
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.FOREGROUND_SERVICE_LOCATION",
        // AndroidX's own, declared by the app for itself: it guards the receivers registered
        // at run time as not exported. Named after the application id.
        "$APPLICATION_ID_PLACEHOLDER.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    )

/**
 * The permissions Passo must never hold (PLANNING.md §10), as patterns over the full name. Any
 * permission outside [AllowedPermissions] fails the build; these fail it by name, as forbidden,
 * so the message says that the answer is never to add it to the list: no network (so nothing
 * can be sent), no location with the app out of use, nothing that lets the app escape Doze or
 * wake the processor on a schedule, no high-rate or body sensors.
 */
val ForbiddenPermissionPatterns: List<String> =
    listOf(
        "android\\.permission\\.INTERNET",
        "android\\.permission\\.ACCESS_BACKGROUND_LOCATION",
        "android\\.permission\\.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android\\.permission\\.SCHEDULE_EXACT_ALARM",
        "android\\.permission\\.USE_EXACT_ALARM",
        "android\\.permission\\.HIGH_SAMPLING_RATE_SENSORS",
        "android\\.permission\\.BODY_SENSORS[A-Z_]*",
    )

/**
 * What the gate found: the [forbidden] permissions requested, the ones requested and not on the
 * list ([unlisted], the forbidden ones left out: they are told as what they are), and the ones
 * on the list that nothing requests ([missing]).
 */
data class PermissionVerdict(val forbidden: List<String>, val unlisted: List<String>, val missing: List<String>) {
    val passes: Boolean get() = forbidden.isEmpty() && unlisted.isEmpty() && missing.isEmpty()
}

/**
 * Compares what a merged manifest [requested] with the [allowed] list, where
 * [APPLICATION_ID_PLACEHOLDER] is read as [applicationId]. Pure, so its rules are tested
 * without a build.
 */
fun permissionVerdict(
    requested: Collection<String>,
    allowed: Collection<String>,
    forbiddenPatterns: Collection<String>,
    applicationId: String,
): PermissionVerdict {
    val held = requested.toSortedSet()
    val listed = allowed.mapTo(sortedSetOf()) { it.replace(APPLICATION_ID_PLACEHOLDER, applicationId) }
    val patterns = forbiddenPatterns.map(::Regex)
    val forbidden = held.filter { permission -> patterns.any { it.matches(permission) } }
    return PermissionVerdict(
        forbidden = forbidden,
        unlisted = held.filter { it !in listed && it !in forbidden },
        missing = listed.filter { it !in held },
    )
}

/**
 * Reads a MERGED manifest (the app's own plus every library's) and fails the build unless it
 * requests exactly the permissions of [AllowedPermissions]. Wired to `check` and run by CI on
 * every push, so adding `INTERNET`, or any permission at all, to a manifest in the dependency
 * graph turns the build red. It keeps the name it had as a list of forbidden names
 * (`checkForbiddenPermissions`): the workflows and the documents call it.
 */
@CacheableTask
abstract class CheckForbiddenPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val allowedPermissions: ListProperty<String>

    @get:Input
    abstract val forbiddenPatterns: ListProperty<String>

    @get:Input
    abstract val applicationId: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val requested = requestedPermissions()
        val verdict = permissionVerdict(
            requested = requested,
            allowed = allowedPermissions.get(),
            forbiddenPatterns = forbiddenPatterns.get(),
            applicationId = applicationId.get(),
        )

        report.get().asFile.writeText(
            buildString {
                appendLine("Requested permissions (${requested.size}):")
                requested.forEach { appendLine("  $it") }
                appendLine("Forbidden found: ${verdict.forbidden.orNone()}")
                appendLine("Not on the list: ${verdict.unlisted.orNone()}")
                appendLine("On the list, not requested: ${verdict.missing.orNone()}")
            },
        )

        if (!verdict.passes) {
            throw GradleException(
                buildString {
                    appendLine("The merged manifest does not request exactly Passo's permissions (PLANNING.md §10).")
                    if (verdict.forbidden.isNotEmpty()) {
                        appendLine(
                            "Forbidden: ${verdict.forbidden.joinToString()}. Passo holds no network, " +
                                "background-location, exact-alarm, battery-exemption or body-sensor permission, " +
                                "ever. Remove the dependency that adds it, or strip it in " +
                                "app/src/main/AndroidManifest.xml with tools:node=\"remove\".",
                        )
                    }
                    if (verdict.unlisted.isNotEmpty()) {
                        appendLine(
                            "Not on the list: ${verdict.unlisted.joinToString()}. Remove what adds it, strip it " +
                                "with tools:node=\"remove\", or, if Passo is to hold it, write it in " +
                                "AllowedPermissions (build-logic) with its reason, and in PLANNING.md §10.",
                        )
                    }
                    if (verdict.missing.isNotEmpty()) {
                        appendLine(
                            "On the list, requested by nothing: ${verdict.missing.joinToString()}. Take it off " +
                                "AllowedPermissions and PLANNING.md §10: the list is what Passo holds, no more.",
                        )
                    }
                    append("Manifest: ${mergedManifest.get().asFile}")
                },
            )
        }
    }

    private fun List<String>.orNone(): String = if (isEmpty()) "none" else joinToString()

    private fun requestedPermissions(): List<String> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)
        return PermissionTags.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).mapNotNull { index ->
                nodes.item(index).attributes
                    ?.getNamedItemNS(ANDROID_NAMESPACE, "name")
                    ?.nodeValue
            }
        }.distinct().sorted()
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        val PermissionTags = listOf("uses-permission", "uses-permission-sdk-23")
    }
}
