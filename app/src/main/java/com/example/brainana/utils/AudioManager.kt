package com.example.brainana.utils

import android.content.Context
import android.media.SoundPool
import androidx.compose.runtime.mutableStateOf
import com.example.brainana.R

class AudioManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var bgMusicPlayer: android.media.MediaPlayer? = null

    var isMuted = mutableStateOf(false)
    var volume = mutableStateOf(1f)
    var isLoading = mutableStateOf(false)

    // Sound IDs loaded from raw resources
    private var correctSoundId = 0
    private var wrongSoundId = 0
    private var timeoutSoundId = 0
    private var levelupSoundId = 0
    private var rankupSoundId = 0

    init {
        initializeSoundPool()
        loadSounds()
        println("🎵 AudioManager initialized with local sounds")
    }

    // ========== INITIALIZE SOUNDPOOL ==========
    private fun initializeSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .build()
            .apply {
                setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) {
                        println("✅ Sound loaded successfully")
                    } else {
                        println("❌ Failed to load sound")
                    }
                }
            }
    }

    // ========== LOAD ALL SOUNDS ==========
    private fun loadSounds() {
        try {
            correctSoundId = soundPool?.load(context, R.raw.correct_answer, 1) ?: 0
            wrongSoundId = soundPool?.load(context, R.raw.wrong_answer, 1) ?: 0
            timeoutSoundId = soundPool?.load(context, R.raw.timeout_alert, 1) ?: 0
            levelupSoundId = soundPool?.load(context, R.raw.level_up, 1) ?: 0
            rankupSoundId = soundPool?.load(context, R.raw.rank_promotion, 1) ?: 0

            println("✅ All sounds loaded")
        } catch (e: Exception) {
            println("❌ Error loading sounds: ${e.message}")
        }
    }

    // ========== BACKGROUND MUSIC ==========
    fun playBackgroundMusic(loop: Boolean = true) {
        try {
            if (isMuted.value) return

            bgMusicPlayer?.release()
            bgMusicPlayer = android.media.MediaPlayer.create(context, R.raw.dashboard_music).apply {
                isLooping = loop
                setVolume(volume.value, volume.value)
                start()
                println("🎵 Background music playing")
            }
        } catch (e: Exception) {
            println("❌ Error playing background music: ${e.message}")
        }
    }

    fun stopBackgroundMusic() {
        try {
            bgMusicPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            bgMusicPlayer = null
        } catch (e: Exception) {
            println("❌ Error stopping music: ${e.message}")
        }
    }

    fun pauseBackgroundMusic() {
        try {
            bgMusicPlayer?.apply {
                if (isPlaying) pause()
            }
            println("⏸️ Music paused")
        } catch (e: Exception) {
            println("❌ Error pausing music: ${e.message}")
        }
    }

    fun resumeBackgroundMusic() {
        try {
            bgMusicPlayer?.apply {
                if (!isPlaying) start()
            }
            println("▶️ Music resumed")
        } catch (e: Exception) {
            println("❌ Error resuming music: ${e.message}")
        }
    }

    // ========== SOUND EFFECTS ==========
    fun playSoundEffect(soundType: String) {
        try {
            if (isMuted.value) return

            val soundId = when (soundType) {
                "correct" -> correctSoundId
                "wrong" -> wrongSoundId
                "timeout" -> timeoutSoundId
                "levelup" -> levelupSoundId
                "rankup" -> rankupSoundId
                else -> 0
            }

            if (soundId > 0) {
                soundPool?.play(
                    soundId,
                    volume.value,
                    volume.value,
                    1,
                    0,
                    1f
                )
                println("🔊 Playing sound: $soundType")
            }
        } catch (e: Exception) {
            println("❌ Error playing sound: ${e.message}")
        }
    }

    // ========== VOLUME CONTROL ==========
    fun setVolume(vol: Float) {
        val clampedVol = vol.coerceIn(0f, 1f)
        volume.value = clampedVol

        bgMusicPlayer?.setVolume(clampedVol, clampedVol)
        soundPool?.setVolume(clampedVol, clampedVol)
        println("🔊 Volume: ${(clampedVol * 100).toInt()}%")
    }

    // ========== MUTE TOGGLE ==========
    fun toggleMute() {
        isMuted.value = !isMuted.value

        if (isMuted.value) {
            pauseBackgroundMusic()
            println("🔇 Muted")
        } else {
            resumeBackgroundMusic()
            println("🔊 Unmuted")
        }
    }

    // ========== CLEANUP ==========
    fun release() {
        try {
            bgMusicPlayer?.release()
            soundPool?.release()
            bgMusicPlayer = null
            soundPool = null
            println("🧹 AudioManager released")
        } catch (e: Exception) {
            println("❌ Error releasing: ${e.message}")
        }
    }
}

private fun SoundPool?.setVolume(streamID: Float, leftVolume: Float) {}
