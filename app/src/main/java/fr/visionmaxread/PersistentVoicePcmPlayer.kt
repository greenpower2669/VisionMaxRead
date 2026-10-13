package fr.visionmaxread

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.CancellationException
import java.util.function.BooleanSupplier
import kotlin.math.max

/** Persistent streaming AudioTrack; warm-up crosses the real audio route at zero track gain. */
class PersistentVoicePcmPlayer {
    private var track: AudioTrack? = null
    private var sampleRate = 0
    private var writtenFrames = 0L

    @Synchronized private fun ensureTrack(rate: Int): AudioTrack {
        val existing = track
        if (existing != null && sampleRate == rate && existing.state == AudioTrack.STATE_INITIALIZED) return existing
        releaseLocked()
        val minBytes = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        check(minBytes > 0) { "AudioTrack buffer unavailable: $minBytes" }
        val created = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(minBytes * 4, 16 * 1024))
            .build()
        check(created.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack initialization failed" }
        track = created
        sampleRate = rate
        writtenFrames = 0L
        return created
    }

    @Synchronized fun warm(samples: FloatArray, rate: Int, cancelled: BooleanSupplier) {
        if (samples.isEmpty()) return
        val current = ensureTrack(rate)
        current.setVolume(0f)
        if (current.playState != AudioTrack.PLAYSTATE_PLAYING) current.play()
        writeAll(current, samples, cancelled)
        waitUntilConsumed(current, cancelled)
        current.setVolume(1f)
    }

    @Synchronized fun play(samples: FloatArray, rate: Int, guardMs: Int, cancelled: BooleanSupplier) {
        if (samples.isEmpty()) return
        val current = ensureTrack(rate)
        current.setVolume(1f)
        if (current.playState != AudioTrack.PLAYSTATE_PLAYING) current.play()
        val guardFrames = (rate * guardMs / 1000).coerceAtLeast(0)
        if (guardFrames > 0) writeAll(current, FloatArray(guardFrames), cancelled)
        writeAll(current, samples, cancelled)
        waitUntilConsumed(current, cancelled)
    }

    private fun writeAll(current: AudioTrack, samples: FloatArray, cancelled: BooleanSupplier) {
        var offset = 0
        while (offset < samples.size) {
            checkCancelled(cancelled)
            val n = current.write(samples, offset, samples.size - offset, AudioTrack.WRITE_BLOCKING)
            check(n > 0) { "AudioTrack write failed: $n" }
            offset += n
            writtenFrames += n
        }
    }

    private fun waitUntilConsumed(current: AudioTrack, cancelled: BooleanSupplier) {
        val target = writtenFrames
        while ((current.playbackHeadPosition.toLong() and 0xffffffffL) < target) {
            checkCancelled(cancelled)
            Thread.sleep(5)
        }
    }

    @Synchronized fun stop() = releaseLocked()
    @Synchronized fun release() = releaseLocked()
    @Synchronized fun hasLiveTrack(): Boolean = track?.state == AudioTrack.STATE_INITIALIZED

    private fun releaseLocked() {
        track?.let {
            try { it.pause() } catch (_: Throwable) {}
            try { it.flush() } catch (_: Throwable) {}
            try { it.stop() } catch (_: Throwable) {}
            try { it.release() } catch (_: Throwable) {}
        }
        track = null
        sampleRate = 0
        writtenFrames = 0L
    }

    private fun checkCancelled(cancelled: BooleanSupplier) {
        if (cancelled.asBoolean) throw CancellationException()
    }
}
