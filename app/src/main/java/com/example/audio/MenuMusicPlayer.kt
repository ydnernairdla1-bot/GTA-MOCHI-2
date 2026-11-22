package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.R

class MenuMusicPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var shouldPlay: Boolean = false
    private var isForeground: Boolean = true
    private var isMuted: Boolean = false

    @Synchronized
    fun setShouldPlay(active: Boolean) {
        shouldPlay = active
        if (!active) {
            mediaPlayer?.let { player ->
                runCatching {
                    if (player.isPlaying) {
                        player.pause()
                    }
                    player.seekTo(0)
                }
            }
        }
        syncPlaybackState()
    }

    @Synchronized
    fun setForeground(foreground: Boolean) {
        isForeground = foreground
        syncPlaybackState()
    }

    @Synchronized
    fun setMuted(muted: Boolean) {
        isMuted = muted
        mediaPlayer?.let { player ->
            val vol = if (muted) 0f else 0.72f
            runCatching { player.setVolume(vol, vol) }
        }
        syncPlaybackState()
    }

    @Synchronized
    private fun syncPlaybackState() {
        val wantAudio = shouldPlay && isForeground && !isMuted
        if (wantAudio) {
            val player = ensurePlayer() ?: return
            runCatching {
                player.setVolume(0.72f, 0.72f)
                if (!player.isPlaying) {
                    player.start()
                }
            }
        } else {
            mediaPlayer?.let { player ->
                runCatching {
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            }
        }
    }

    private fun ensurePlayer(): MediaPlayer? {
        mediaPlayer?.let { return it }
        return runCatching {
            MediaPlayer.create(appContext, R.raw.menu_theme)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                isLooping = true
                setVolume(if (isMuted) 0f else 0.72f, if (isMuted) 0f else 0.72f)
            }
        }.getOrNull()?.also { mediaPlayer = it }
    }

    @Synchronized
    fun release() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        }
        mediaPlayer = null
    }
}
