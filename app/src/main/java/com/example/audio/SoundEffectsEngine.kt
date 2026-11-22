package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.GameWorldState
import com.example.game.SoundEvent
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class SoundEffectsEngine {

    private class Voice(
        val kind: SoundEvent,
        val durationSec: Float,
        var elapsedSec: Float = 0f,
        var phase: Float = 0f,
    )

    private val pendingEvents = ConcurrentLinkedQueue<SoundEvent>()

    @Volatile
    private var isRunning = false

    @Volatile
    private var isActiveSession = false

    @Volatile
    private var isMuted = false

    @Volatile
    private var targetEngineNorm = 0f // 0 = off, >0 = driving RPM [0.15..1.35]

    @Volatile
    private var targetNitroBoost = 0f

    @Volatile
    private var targetDrift = 0f

    @Volatile
    private var targetSirenLevel = 0f

    private var mixerThread: Thread? = null

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun setSessionActive(active: Boolean) {
        isActiveSession = active
        if (!active) {
            targetEngineNorm = 0f
            targetNitroBoost = 0f
            targetDrift = 0f
            targetSirenLevel = 0f
            pendingEvents.clear()
        } else {
            ensureStarted()
        }
    }

    fun playUiClick() {
        if (!isMuted) {
            ensureStarted()
            pendingEvents.add(SoundEvent.PICKUP_CASH)
        }
    }

    fun triggerEvent(event: SoundEvent) {
        if (!isMuted && isActiveSession) {
            pendingEvents.add(event)
        }
    }

    fun processWorldState(world: GameWorldState, isNitroActive: Boolean) {
        if (isMuted || !isActiveSession) return

        for (ev in world.soundEvents) {
            pendingEvents.add(ev)
        }

        if (world.isDeadOrBusted) {
            targetEngineNorm = 0f
            targetNitroBoost = 0f
            targetDrift = 0f
            targetSirenLevel = 0f
            return
        }

        val activeVeh = world.inVehicleId?.let { id -> world.vehicles.firstOrNull { it.id == id } }
        if (activeVeh != null) {
            val speedFrac = (abs(activeVeh.speed) / activeVeh.kind.maxSpeed).coerceIn(0f, 1.35f)
            targetEngineNorm = 0.20f + speedFrac * 0.95f
            targetNitroBoost = if (isNitroActive && world.nitroFuel > 2f && abs(activeVeh.speed) > 40f) 1f else 0f
            targetDrift = if (world.isDrifting) 1f else 0f
        } else {
            targetEngineNorm = 0f
            targetNitroBoost = 0f
            targetDrift = 0f
        }

        if (world.wantedLevel > 0) {
            val minCopDist = world.vehicles.filter { it.isPolice }
                .minOfOrNull { hypot(it.x - world.playerX, it.y - world.playerY) } ?: 750f
            targetSirenLevel = (1f - (minCopDist / 900f)).coerceIn(0.22f, 1f)
        } else {
            targetSirenLevel = 0f
        }
    }

    @Synchronized
    private fun ensureStarted() {
        if (isRunning) return
        isRunning = true
        mixerThread = Thread({ runAudioLoop() }, "NeonViceAudioMixer").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun release() {
        isRunning = false
        isActiveSession = false
        pendingEvents.clear()
        mixerThread?.interrupt()
        mixerThread = null
    }

    private fun runAudioLoop() {
        val sampleRate = 22050
        val minBytes = try {
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(1024)
        } catch (_: Throwable) {
            isRunning = false
            return
        }

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBytes * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            isRunning = false
            return
        }

        try {
            track.play()
        } catch (_: Throwable) {
            track.release()
            isRunning = false
            return
        }

        val chunkSamples = 512
        val pcm = ShortArray(chunkSamples)
        val activeVoices = ArrayList<Voice>(24)

        val dt = 1f / sampleRate
        val twoPi = (2.0 * PI).toFloat()

        var smoothEngine = 0f
        var smoothNitro = 0f
        var smoothDrift = 0f
        var smoothSiren = 0f

        var enginePhase1 = 0f
        var enginePhase2 = 0f
        var driftPhase = 0f
        var sirenPhase = 0f
        var sirenLfo = 0f
        var lpFilter = 0f

        try {
            while (isRunning) {
                while (true) {
                    val ev = pendingEvents.poll() ?: break
                    if (activeVoices.size < 18) {
                        activeVoices += createVoice(ev)
                    }
                }

                if (!isActiveSession || isMuted) {
                    activeVoices.clear()
                    pcm.fill(0)
                    track.write(pcm, 0, chunkSamples)
                    Thread.sleep(16)
                    continue
                }

                for (i in 0 until chunkSamples) {
                    smoothEngine += (targetEngineNorm - smoothEngine) * 0.0018f
                    smoothNitro += (targetNitroBoost - smoothNitro) * 0.004f
                    smoothDrift += (targetDrift - smoothDrift) * 0.008f
                    smoothSiren += (targetSirenLevel - smoothSiren) * 0.0015f

                    var mix = 0f

                    // 1. Continuous Realistic Engine RPM & Turbo Nitro
                    if (smoothEngine > 0.02f) {
                        val baseFreq = 38f + smoothEngine * 92f + smoothNitro * 35f
                        enginePhase1 = (enginePhase1 + twoPi * baseFreq * dt) % twoPi
                        enginePhase2 = (enginePhase2 + twoPi * baseFreq * 2.0f * dt) % twoPi
                        val saw1 = (enginePhase1 / PI.toFloat()) - 1f
                        val pulse2 = sin(enginePhase2)
                        val rawEngine = (saw1 * 0.55f + pulse2 * 0.45f) * 0.17f * smoothEngine.coerceAtMost(1f)
                        lpFilter += (rawEngine - lpFilter) * 0.22f
                        mix += lpFilter
                    }
                    if (smoothNitro > 0.02f) {
                        val turbine = sin(enginePhase2 * 3.5f) * 0.06f + (Random.nextFloat() * 2f - 1f) * 0.14f
                        mix += turbine * smoothNitro
                    }

                    // 2. Tire Drift Screech
                    if (smoothDrift > 0.03f) {
                        val modFreq = 980f + sin(sirenLfo * 18f) * 85f
                        driftPhase = (driftPhase + twoPi * modFreq * dt) % twoPi
                        val screech = (sin(driftPhase) * 0.65f + (Random.nextFloat() * 2f - 1f) * 0.35f) * 0.15f
                        mix += screech * smoothDrift
                    }

                    // 3. Police Siren (Wail sweep)
                    if (smoothSiren > 0.03f) {
                        sirenLfo = (sirenLfo + twoPi * 1.15f * dt) % twoPi
                        val sirenFreq = 690f + (sin(sirenLfo) * 0.5f + 0.5f) * 430f
                        sirenPhase = (sirenPhase + twoPi * sirenFreq * dt) % twoPi
                        mix += sin(sirenPhase) * 0.13f * smoothSiren
                    }

                    // 4. Polyphonic One-Shot Effects
                    var idx = activeVoices.size - 1
                    while (idx >= 0) {
                        val v = activeVoices[idx]
                        v.elapsedSec += dt
                        if (v.elapsedSec >= v.durationSec) {
                            activeVoices.removeAt(idx)
                        } else {
                            mix += sampleVoice(v, dt, twoPi)
                        }
                        idx--
                    }

                    // Soft-clipping master bus -> zero harsh clipping
                    val scaled = mix * 0.85f
                    val softClipped = scaled / (1f + abs(scaled) * 0.65f)
                    pcm[i] = (softClipped.coerceIn(-0.98f, 0.98f) * 32767f).toInt().toShort()
                }

                track.write(pcm, 0, chunkSamples)
            }
        } catch (_: InterruptedException) {
            // Normal shutdown
        } finally {
            try {
                track.stop()
            } catch (_: Throwable) {
            }
            track.release()
        }
    }

    private fun createVoice(ev: SoundEvent): Voice {
        val dur = when (ev) {
            SoundEvent.HIJACK_ENTER -> 0.34f
            SoundEvent.VEHICLE_EXIT -> 0.14f
            SoundEvent.WEAPON_MELEE -> 0.16f
            SoundEvent.WEAPON_PISTOL -> 0.22f
            SoundEvent.WEAPON_SMG -> 0.13f
            SoundEvent.WEAPON_SHOTGUN -> 0.36f
            SoundEvent.WEAPON_PLASMA -> 0.26f
            SoundEvent.WEAPON_SWITCH -> 0.12f
            SoundEvent.HIT_IMPACT -> 0.11f
            SoundEvent.VEHICLE_CRASH -> 0.32f
            SoundEvent.EXPLOSION -> 0.85f
            SoundEvent.PICKUP_CASH -> 0.22f
            SoundEvent.PICKUP_HEALTH -> 0.35f
            SoundEvent.PAY_N_SPRAY -> 0.68f
            SoundEvent.MISSION_START -> 0.45f
            SoundEvent.MISSION_PASSED -> 1.05f
            SoundEvent.PLAYER_WASTED -> 0.95f
            SoundEvent.PLAYER_BUSTED -> 0.78f
            SoundEvent.CAT_MEOW -> 0.42f
            SoundEvent.CAT_HISS -> 0.30f
        }
        return Voice(ev, dur)
    }

    private fun sampleVoice(v: Voice, dt: Float, twoPi: Float): Float {
        val t = v.elapsedSec
        val p = (t / v.durationSec).coerceIn(0f, 1f)
        val env = 1f - p
        val noise = Random.nextFloat() * 2f - 1f

        return when (v.kind) {
            SoundEvent.WEAPON_PISTOL -> {
                val freq = 260f * (1f - p * 0.75f) + 60f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                val punch = sin(v.phase) * (1f - p).let { it * it } * 0.55f
                val crack = noise * (1f - p).let { it * it * it } * 0.55f
                punch + crack
            }
            SoundEvent.WEAPON_SMG -> {
                val freq = 320f * (1f - p * 0.7f) + 85f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) * 0.42f + noise * 0.38f) * env * env
            }
            SoundEvent.WEAPON_SHOTGUN -> {
                val freq = 145f * (1f - p * 0.78f) + 42f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) * 0.65f + noise * 0.72f) * env * env
            }
            SoundEvent.WEAPON_PLASMA -> {
                val freq = 1350f * (1f - p * 0.78f) + 190f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                val zap = sin(v.phase) * 0.48f + sin(v.phase * 2.0f) * 0.22f
                zap * env
            }
            SoundEvent.WEAPON_MELEE -> {
                val freq = 180f * (1f - p * 0.65f)
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) * 0.38f + noise * 0.24f) * env * env
            }
            SoundEvent.WEAPON_SWITCH -> {
                val freq = if (t < 0.05f) 520f else 880f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) * 0.26f + noise * 0.18f) * env * env
            }
            SoundEvent.HIT_IMPACT -> {
                val freq = 160f * (1f - p * 0.5f)
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) * 0.28f + noise * 0.22f) * env * env
            }
            SoundEvent.HIJACK_ENTER -> {
                // Door thud followed by starter rev
                if (t < 0.09f) {
                    v.phase = (v.phase + twoPi * 115f * dt) % twoPi
                    (sin(v.phase) * 0.42f + noise * 0.20f) * (1f - t / 0.09f)
                } else {
                    val revFreq = 65f + (p * 110f)
                    v.phase = (v.phase + twoPi * revFreq * dt) % twoPi
                    sin(v.phase) * 0.26f * env
                }
            }
            SoundEvent.VEHICLE_EXIT -> {
                v.phase = (v.phase + twoPi * 140f * dt) % twoPi
                (sin(v.phase) * 0.32f + noise * 0.16f) * env * env
            }
            SoundEvent.VEHICLE_CRASH -> {
                val freq = 105f * (1f - p * 0.55f)
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) * 0.45f + noise * 0.62f) * env * env
            }
            SoundEvent.EXPLOSION -> {
                val freq = 82f * (1f - p * 0.72f) + 26f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                val boom = sin(v.phase) * 0.75f + noise * 0.85f
                boom * (1f - p) * (1f - p * 0.5f)
            }
            SoundEvent.PICKUP_CASH -> {
                val freq = if (t < 0.09f) 987.77f else 1318.51f // B5 -> E6
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                sin(v.phase) * 0.30f * env
            }
            SoundEvent.PICKUP_HEALTH -> {
                val freq = 440f + p * 440f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                sin(v.phase) * 0.30f * env
            }
            SoundEvent.PAY_N_SPRAY -> {
                if (t < 0.42f) {
                    noise * 0.28f * (1f - t / 0.45f)
                } else {
                    val freq = 659.25f // E5 chime
                    v.phase = (v.phase + twoPi * freq * dt) % twoPi
                    (sin(v.phase) + sin(v.phase * 1.5f) * 0.5f) * 0.25f * env
                }
            }
            SoundEvent.MISSION_START -> {
                val freq = when {
                    t < 0.14f -> 392f
                    t < 0.28f -> 493.88f
                    else -> 587.33f
                }
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                sin(v.phase) * 0.32f * env
            }
            SoundEvent.MISSION_PASSED -> {
                val freq = when {
                    t < 0.20f -> 523.25f // C5
                    t < 0.40f -> 659.25f // E5
                    t < 0.65f -> 783.99f // G5
                    else -> 1046.50f // C6
                }
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) * 0.34f + sin(v.phase * 0.5f) * 0.16f) * env
            }
            SoundEvent.PLAYER_WASTED -> {
                val freq = 220f * (1f - p * 0.65f)
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                (sin(v.phase) + sin(v.phase * 1.5f) * 0.4f) * 0.36f * env
            }
            SoundEvent.PLAYER_BUSTED -> {
                val freq = if ((t * 8f).toInt() % 2 == 0) 640f else 480f
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                sin(v.phase) * 0.32f * env
            }
            SoundEvent.CAT_MEOW -> {
                // Expressive feline meow: pitch sweeps up 560 -> 880 Hz then falls to 470 Hz
                val freq = if (p < 0.35f) {
                    560f + (p / 0.35f) * 320f
                } else {
                    880f - ((p - 0.35f) / 0.65f) * 410f
                }
                v.phase = (v.phase + twoPi * freq * dt) % twoPi
                val envelope = kotlin.math.sin(p * PI.toFloat()).coerceAtLeast(0f)
                (sin(v.phase) * 0.34f + sin(v.phase * 2.0f) * 0.14f + sin(v.phase * 3.0f) * 0.06f) * envelope
            }
            SoundEvent.CAT_HISS -> {
                // Sharp feline hiss burst
                val envelope = (1f - p).let { it * it }
                noise * 0.32f * envelope
            }
        }
    }
}
