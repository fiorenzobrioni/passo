package com.callbackdev.passo.core.tracking

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.callbackdev.passo.core.designsystem.format.intensityPhrase
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.designsystem.ways.placeNoteRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.PaceSummary
import com.callbackdev.passo.core.domain.sessions.PaceVerdict
import com.callbackdev.passo.core.domain.sessions.SessionAmount
import com.callbackdev.passo.core.domain.sessions.SessionAnnouncement
import com.callbackdev.passo.core.domain.sessions.SpeechRoute
import com.callbackdev.passo.core.domain.ways.NextPlace
import com.callbackdev.passo.core.domain.ways.WalkPlaces
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.model.MeasureUnit
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.roundToInt

/** Whether this phone can speak an outing's signals in the app's language. */
enum class VoiceAvailability {
    /** Not asked yet: the engine answers when it is first bound. */
    UNKNOWN,

    /** An offline voice in the app's language is installed. */
    READY,

    /**
     * The engine has no offline voice for the language (or only one it would fetch from the
     * network): Passo does not speak rather than ask a server to.
     */
    NO_OFFLINE_VOICE,

    /** No text-to-speech engine on the phone, or it failed to start. */
    NO_ENGINE,
}

/**
 * An outing's signals, spoken (Phase 10, second iteration; `docs/adr/0010-voice.md`).
 *
 * - **The system's engine, an offline voice.** Android's text-to-speech engine, the one the
 *   reader chose in the system's settings, with a voice installed on the phone for the app's
 *   language; a voice that needs the network is never picked, so Passo still sends nothing.
 *   No voice, no speech: the vibrations stay.
 * - **Heard only by whom it should be** ([SpeechRoute]): through headphones, or out loud only
 *   when the outing asks for it and the phone is not silenced.
 * - **Over the music, not instead of it:** navigation-guidance audio with a transient focus that
 *   lets the music duck, given back as soon as the sentence ends.
 * - **Bound only for an outing that speaks**, from its start to its end, then released.
 * - **Not over a phone call.** Android gives a call the audio: a sentence due during one is kept
 *   (the latest only) and said when the call ends, through [onCallEnded], which may put it in
 *   today's words (an interval's time left has moved on). Kept no longer than
 *   [CALL_HOLD_MILLIS]: a signal an hour late says nothing.
 *
 * Main thread only, like the tracking service that drives it.
 */
class SessionSpeech(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audio: AudioManager? = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()

    private var tts: TextToSpeech? = null
    private var queued: String? = null

    // A sentence due during a call, with when it was due, and the route it was to take; the
    // call's end is heard only while one waits.
    private var missed: Missed? = null
    private var listeningForCallEnd = false

    /**
     * What to say when a call ends, given the sentence it hid: the same sentence, or one in
     * today's words. Set by the outing's service; null says the sentence as it was.
     */
    var onCallEnded: ((String) -> String)? = null
    private var releaseWhenQuiet = false
    private var utterance = 0

    // Sentences handed to the engine and not yet over. Counted here rather than asked of the
    // engine: it reports "not speaking" until synthesis starts, and the goal's sentence is
    // handed over a moment before the outing lets the engine go.
    private var unfinished = 0

    private val state = MutableStateFlow(VoiceAvailability.UNKNOWN)

    /** What the engine said when it was bound; [VoiceAvailability.UNKNOWN] before. */
    val availability: StateFlow<VoiceAvailability> = state.asStateFlow()

    /**
     * Binds the engine, if it is not yet: the first sentence then comes without a wait. An engine
     * about to be let go after its last sentence is kept instead: an outing taken back speaks on.
     */
    fun prepare() {
        if (tts != null) {
            releaseWhenQuiet = false
            return
        }
        state.value = VoiceAvailability.UNKNOWN
        tts = try {
            TextToSpeech(app) { status -> main.post { onInit(status) } }
        } catch (e: RuntimeException) {
            Log.w(TAG, "No text-to-speech engine", e)
            state.value = VoiceAvailability.NO_ENGINE
            null
        }
    }

    /** Says [text] if [voice] lets it be heard now (headphones, ringer), binding the engine if needed. */
    fun say(text: String, voice: SessionVoice) {
        if (inCall()) {
            missed = Missed(text, voice, SystemClock.elapsedRealtime())
            listenForCallEnd()
            return
        }
        if (!SpeechRoute.speaks(voice, headphonesConnected(), ringerNormal())) return
        speak(text)
    }

    /** Says [text] whatever the route: the editor's "Try it", which the reader just touched. */
    fun preview(text: String) = speak(text)

    /** Unbinds the engine, once every sentence handed to it (or waiting for it) is said. */
    fun release() {
        if (tts == null) return
        if (unfinished > 0 || queued != null || missed != null) releaseWhenQuiet = true else shutdown()
    }

    private fun speak(text: String) {
        when (state.value) {
            VoiceAvailability.READY -> speakNow(text)

            VoiceAvailability.UNKNOWN -> {
                queued = text
                prepare()
            }

            VoiceAvailability.NO_OFFLINE_VOICE, VoiceAvailability.NO_ENGINE -> Unit
        }
    }

    private fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            state.value = VoiceAvailability.NO_ENGINE
            queued = null
            if (releaseWhenQuiet) shutdown()
            return
        }
        val voice = chosenVoice(engine, app.resources.configuration.locales[0])
        if (voice == null) {
            state.value = VoiceAvailability.NO_OFFLINE_VOICE
            queued = null
            if (releaseWhenQuiet) shutdown()
            return
        }
        engine.voice = voice
        engine.setAudioAttributes(attributes)
        engine.setOnUtteranceProgressListener(progress)
        state.value = VoiceAvailability.READY
        queued?.let(::speakNow)
        queued = null
    }

    private fun speakNow(text: String) {
        val engine = tts ?: return
        audio?.requestAudioFocus(focus)
        val params = Bundle().apply {
            // Never the engine's network synthesis, whatever the voice would allow.
            putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "false")
        }
        if (engine.speak(text, TextToSpeech.QUEUE_ADD, params, "passo-${++utterance}") == TextToSpeech.SUCCESS) {
            unfinished++
        } else {
            quiet()
        }
    }

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            main.post(::finished)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            main.post(::finished)
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            main.post(::finished)
        }
    }

    private fun finished() {
        if (unfinished > 0) unfinished--
        quiet()
    }

    /** The music back as it was, and the engine let go if the outing is over. */
    private fun quiet() {
        if (tts == null || unfinished > 0) return
        audio?.abandonAudioFocusRequest(focus)
        if (releaseWhenQuiet) shutdown()
    }

    /**
     * A call, a VoIP call, or a phone ringing, and Android refuses Passo the audio: both, so that
     * a mode left behind by another app never silences the voice (the focus is then given, and
     * the sentence said as always).
     */
    private fun inCall(): Boolean {
        val manager = audio ?: return false
        if (manager.mode == AudioManager.MODE_NORMAL) return false
        val granted = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) manager.abandonAudioFocusRequest(focus)
        return !granted
    }

    private fun listenForCallEnd() {
        if (listeningForCallEnd || audio == null) return
        audio.addOnModeChangedListener(app.mainExecutor, modeListener)
        listeningForCallEnd = true
    }

    private fun stopListeningForCallEnd() {
        if (!listeningForCallEnd) return
        audio?.removeOnModeChangedListener(modeListener)
        listeningForCallEnd = false
    }

    private val modeListener = AudioManager.OnModeChangedListener { mode ->
        if (mode != AudioManager.MODE_NORMAL) return@OnModeChangedListener
        stopListeningForCallEnd()
        val waiting = missed ?: return@OnModeChangedListener
        missed = null
        val fresh = SystemClock.elapsedRealtime() - waiting.atElapsed <= CALL_HOLD_MILLIS
        if (fresh && SpeechRoute.speaks(waiting.voice, headphonesConnected(), ringerNormal())) {
            // A moment for the headphones to leave the call's audio, or the first words are lost.
            unfinished++
            main.postDelayed({
                if (unfinished > 0) unfinished--
                if (tts != null) speak(onCallEnded?.invoke(waiting.text) ?: waiting.text) else quiet()
            }, CALL_SETTLE_MILLIS)
        } else if (releaseWhenQuiet && unfinished == 0 && queued == null) {
            shutdown()
        }
    }

    private class Missed(val text: String, val voice: SessionVoice, val atElapsed: Long)

    private fun shutdown() {
        stopListeningForCallEnd()
        missed = null
        tts?.shutdown()
        tts = null
        queued = null
        releaseWhenQuiet = false
        unfinished = 0
        audio?.abandonAudioFocusRequest(focus)
        state.value = VoiceAvailability.UNKNOWN
    }

    private fun headphonesConnected(): Boolean =
        audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.any { it.type in PRIVATE_OUTPUTS } == true

    private fun ringerNormal(): Boolean = audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL

    companion object {
        private const val TAG = "PassoVoice"

        /** A sentence a call hid is said after it only within this long. */
        private const val CALL_HOLD_MILLIS = 15 * 60_000L

        /** How long after a call its hidden sentence waits, for the audio to come back. */
        private const val CALL_SETTLE_MILLIS = 1_500L

        /**
         * What counts as headphones: what the reader alone hears, or their car. A Bluetooth
         * speaker also reports itself as A2DP; the voice then is out loud, as the reader chose
         * by pairing it.
         */
        private val PRIVATE_OUTPUTS = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID,
        )

        /** The system's text-to-speech settings, where a voice is installed. */
        fun settingsIntent(): Intent = Intent("com.android.settings.TTS_SETTINGS")

        /**
         * The voice the reader chose for [locale]'s language in the system's text-to-speech
         * settings (where a voice is picked by ear, male or female, with a sample), when it is
         * installed and needs no network; else the best offline one. Passo offers no picker of
         * its own: the engine does not say which voice is which, and a guessed label would be
         * worse than none.
         */
        internal fun chosenVoice(engine: TextToSpeech, locale: Locale): Voice? {
            val preferred = try {
                engine.setLanguage(locale)
                engine.voice
            } catch (e: RuntimeException) {
                null
            }
            return preferred?.takeIf { it.usable(locale) } ?: offlineVoice(engine, locale)
        }

        private fun Voice.usable(locale: Locale): Boolean = this.locale.language == locale.language &&
            !isNetworkConnectionRequired &&
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features

        /**
         * The best installed voice for [locale]'s language that needs no network: the same
         * country first, then the better quality, then the quicker one.
         */
        internal fun offlineVoice(engine: TextToSpeech, locale: Locale): Voice? {
            val voices = try {
                engine.voices
            } catch (e: RuntimeException) {
                null
            } ?: return null
            return voices
                .filter { it.usable(locale) }
                .maxWithOrNull(
                    compareBy<Voice>({ it.locale.country == locale.country }, { it.quality }, { -it.latency }),
                )
        }
    }
}

/*
 * The spoken words (resources in English and Italian): every amount in words, since «20 min»
 * read aloud is at the mercy of the engine.
 */

/** «Brisk walk: off you go. 20 minutes at a brisk pace.» and the rest, for [session]. */
internal fun Context.spoken(announcement: SessionAnnouncement, session: Session, units: UnitPreference): String {
    val res = resources
    val format = measureFormatter(units)
    return when (announcement) {
        is SessionAnnouncement.Started -> announcement.intervals?.let { sets ->
            res.getQuantityString(
                R.plurals.spoken_intervals_started,
                sets.sets,
                res.sessionName(session),
                sets.sets,
                res.getQuantityString(R.plurals.spoken_minutes, sets.slowMinutes, sets.slowMinutes),
                res.getQuantityString(R.plurals.spoken_minutes, sets.fastMinutes, sets.fastMinutes),
            )
        } ?: res.getString(
            R.string.spoken_started,
            res.sessionName(session),
            res.intensityPhrase(announcement.intensity, res.spokenAmount(announcement.goal, format)),
        )

        is SessionAnnouncement.IntervalChanged -> res.spokenInterval(announcement)

        is SessionAnnouncement.Milestone -> res.spokenMilestone(announcement, format)

        is SessionAnnouncement.EndedStill -> res.getString(
            R.string.spoken_ended_still,
            res.spokenAmount(SessionAmount(SessionGoalKind.STEPS, announcement.steps.toDouble()), format),
        )

        is SessionAnnouncement.WalkStarted -> {
            val here = res.getString(placeNameRes(announcement.here.key))
            if (announcement.continued) {
                listOfNotNull(
                    res.getString(R.string.spoken_walk_continued, res.sessionName(session), here),
                    announcement.next?.let { res.spokenNext(it, format) },
                ).joinToString(" ")
            } else {
                listOfNotNull(
                    res.getString(R.string.spoken_walk_started, res.sessionName(session), here),
                    placeNoteRes(announcement.here.key)?.let(res::getString),
                ).joinToString(" ")
            }
        }

        is SessionAnnouncement.PlacesReached -> res.spokenPlaces(announcement, format)

        is SessionAnnouncement.GoalReached -> {
            val steps = res.spokenAmount(SessionAmount(SessionGoalKind.STEPS, announcement.steps.toDouble()), format)
            val goal = when {
                // A walk's goal is its last place, which was just said: what it took.
                session.walk != null -> res.getString(R.string.spoken_walk_end, steps)

                // The goal was the steps: said once.
                announcement.goal.kind in STEP_GOALS -> res.getString(R.string.spoken_goal_steps, steps)

                else -> res.getString(R.string.spoken_goal, res.spokenAmount(announcement.goal, format), steps)
            }
            val pace = when (val kept = announcement.pace) {
                PaceSummary.None -> null

                PaceSummary.Mostly -> res.getString(R.string.spoken_mostly_at_pace)

                is PaceSummary.Part -> res.getString(
                    R.string.spoken_zone,
                    kept.zoneMinutes,
                    res.getQuantityString(R.plurals.spoken_minutes, kept.movingMinutes, kept.movingMinutes),
                )

                is PaceSummary.Intervals -> res.getQuantityString(
                    R.plurals.spoken_intervals_at_pace,
                    kept.atPace,
                    kept.atPace,
                    kept.fast,
                )
            }
            val day = res.getString(R.string.spoken_day_goal).takeIf { announcement.dayGoalReached }
            listOfNotNull(goal, pace, day, res.getString(R.string.spoken_well_done)).joinToString(" ")
        }
    }
}

/** «Fast, 3 minutes.», «Slow.», «Last fast interval, 3 minutes.» */
private fun Resources.spokenInterval(announcement: SessionAnnouncement.IntervalChanged): String {
    if (!announcement.fast) return getString(R.string.spoken_interval_slow)
    val minutes = getQuantityString(R.plurals.spoken_minutes, announcement.minutes, announcement.minutes)
    return getString(
        if (announcement.lastSet) R.string.spoken_interval_last_fast else R.string.spoken_interval_fast,
        minutes,
    )
}

/** A sample of an interval outing's change, for the editor's "Try it": its first fast interval. */
fun Context.spokenIntervalSample(fastMinutes: Int): String =
    resources.spokenInterval(SessionAnnouncement.IntervalChanged(fast = true, minutes = fastMinutes, lastSet = false))

/** A sample of what an outing says on the way, for the editor's "Try it": its halfway. */
fun Context.spokenSample(announcement: SessionAnnouncement.Milestone, units: UnitPreference): String =
    resources.spokenMilestone(announcement, measureFormatter(units))

private fun Resources.spokenMilestone(announcement: SessionAnnouncement.Milestone, format: MeasureFormatter): String {
    val left = spokenAmount(announcement.left, format)
    val quantity = if (announcement.left.kind == SessionGoalKind.DISTANCE) 2 else announcement.left.value.roundToInt()
    val share = getQuantityString(
        when (announcement.milestone) {
            SessionMilestone.QUARTER -> R.plurals.spoken_quarter
            SessionMilestone.HALF, SessionMilestone.GOAL -> R.plurals.spoken_half
            SessionMilestone.THREE_QUARTERS -> R.plurals.spoken_three_quarters
        },
        quantity,
        left,
    )
    val cadence = announcement.cadence
    val pace = when {
        cadence == null -> null
        announcement.pace == PaceVerdict.ON_PACE -> getQuantityString(R.plurals.spoken_pace_on, cadence, cadence)
        announcement.pace == PaceVerdict.BELOW -> getQuantityString(R.plurals.spoken_pace_below, cadence, cadence)
        else -> null
    }
    return listOfNotNull(share, pace).joinToString(" ")
}

/**
 * A sample of what a city walk says on the way, for its page's "Hear it": the first place
 * ahead of [fromMeters] as it would be told on reaching it, with the one after.
 */
fun Context.spokenWalkSample(walk: Way, fromMeters: Int, units: UnitPreference): String {
    val stop = WalkPlaces.after(walk, fromMeters)?.stop ?: walk.stops.last()
    val sample = SessionAnnouncement.PlacesReached(listOf(stop), WalkPlaces.after(walk, stop.distanceMeters))
    return resources.spokenPlaces(sample, measureFormatter(units))
}

/** «Behind you: Brera. Here: La Scala. La Scala opened in 1778… Next: …, in 640 metres.» */
private fun Resources.spokenPlaces(announcement: SessionAnnouncement.PlacesReached, format: MeasureFormatter): String {
    val names = announcement.places.map { getString(placeNameRes(it.key)) }
    val reached = announcement.places.last()
    return listOfNotNull(
        names.dropLast(1).takeIf {
            it.isNotEmpty()
        }?.let { getString(R.string.spoken_walk_behind, it.joinToString(", ")) },
        getString(R.string.spoken_walk_here, names.last()),
        placeNoteRes(reached.key)?.let(::getString),
        announcement.next?.let { spokenNext(it, format) },
    ).joinToString(" ")
}

/** «Next: the Duomo, in 600 metres.» */
private fun Resources.spokenNext(next: NextPlace, format: MeasureFormatter): String {
    val measure = format.aheadDistance(next.inMeters.toDouble())
    val unit = when (measure.unit) {
        MeasureUnit.METER -> R.plurals.spoken_metres
        MeasureUnit.YARD -> R.plurals.spoken_yards
        MeasureUnit.MILE -> R.plurals.spoken_miles
        else -> R.plurals.spoken_kilometres
    }
    // Tens of metres or yards are always many, and so is a distance with decimals.
    val distance = getQuantityString(unit, MANY, measure.number)
    return getString(R.string.spoken_walk_next, getString(placeNameRes(next.stop.key)), distance)
}

/** An amount in words: «20 minutes», «2,400 steps», «1.50 kilometres». */
private fun Resources.spokenAmount(amount: SessionAmount, format: MeasureFormatter): String = when (amount.kind) {
    SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> {
        val steps = amount.value.roundToInt()
        getQuantityString(R.plurals.spoken_steps, steps, format.steps(steps))
    }

    SessionGoalKind.TIME, SessionGoalKind.INTERVALS -> {
        val minutes = amount.value.roundToInt()
        getQuantityString(R.plurals.spoken_minutes, minutes, minutes)
    }

    SessionGoalKind.DISTANCE -> {
        val measure = format.distance(amount.value)
        val unit = if (measure.unit == MeasureUnit.MILE) {
            R.plurals.spoken_miles
        } else {
            R.plurals.spoken_kilometres
        }
        getQuantityString(unit, 2, measure.number)
    }
}

private val STEP_GOALS = setOf(SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY)

/** A plural's quantity that reads as many in English and Italian alike. */
private const val MANY = 2
