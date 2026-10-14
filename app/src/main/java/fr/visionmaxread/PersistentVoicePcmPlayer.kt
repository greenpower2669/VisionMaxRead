package fr.visionmaxread

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.CancellationException
import java.util.function.BooleanSupplier
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/** Persistent streaming AudioTrack; warm-up crosses the real audio route at zero track gain. */
class PersistentVoicePcmPlayer {
    @Volatile private var track: AudioTrack? = null
    private val playbackGeneration = AtomicLong(0L)
    private var sampleRate = 0

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
        return created
    }

    @Synchronized fun warm(samples: FloatArray, rate: Int, cancelled: BooleanSupplier) {
        if (samples.isEmpty()) return
        val current = ensureTrack(rate)
        current.setVolume(0f)
        if (current.playState != AudioTrack.PLAYSTATE_PLAYING) current.play()
        val startHead = current.playbackHeadPosition.toLong() and 0xffffffffL
        val written = writeAll(current, samples, cancelled)
        waitUntilConsumed(current, startHead, written, cancelled)
        current.setVolume(1f)
    }

    /** Playback is serialized on VoicePCM; stop() must never wait for this track. */
    fun play(samples: FloatArray, rate: Int, guardMs: Int, tailPauseMs: Int, cancelled: BooleanSupplier) {
        if (samples.isEmpty()) return
        val generation = playbackGeneration.get()
        val current = ensureTrack(rate)
        val active = BooleanSupplier {
            cancelled.asBoolean || generation != playbackGeneration.get()
        }
        checkCancelled(active)
        current.setVolume(1f)
        if (current.playState != AudioTrack.PLAYSTATE_PLAYING) current.play()
        val startHead = current.playbackHeadPosition.toLong() and 0xffffffffL
        var written = 0L
        val guardFrames = (rate * guardMs / 1000).coerceAtLeast(0)
        if (guardFrames > 0) written += writeAll(current, FloatArray(guardFrames), active)
        written += writeAll(current, samples, active)
        // The only explicit additional pause is a genuine blank-line paragraph.
        val tailFrames = (rate * tailPauseMs / 1000).coerceAtLeast(0)
        if (tailFrames > 0) written += writeAll(current, FloatArray(tailFrames), active)
        waitUntilConsumed(current, startHead, written, active)
        checkCancelled(active)
    }

    private fun writeAll(current: AudioTrack, samples: FloatArray, cancelled: BooleanSupplier): Long {
        var offset = 0
        while (offset < samples.size) {
            checkCancelled(cancelled)
            val n = current.write(samples, offset, samples.size - offset, AudioTrack.WRITE_BLOCKING)
            check(n > 0) { "AudioTrack write failed: $n" }
            offset += n
        }
        return offset.toLong()
    }

    /** Wait on the real playback head; the 5 ms sleep only avoids busy-spinning. */
    private fun waitUntilConsumed(current: AudioTrack, startHead: Long, frames: Long, cancelled: BooleanSupplier) {
        while ((((current.playbackHeadPosition.toLong() and 0xffffffffL) - startHead) and 0xffffffffL) < frames) {
            checkCancelled(cancelled)
            Thread.sleep(5)
        }
    }

    /** Stop current speech but retain the initialized AudioTrack for the next warm-up. */
    fun stop() {
        // Invalidate queued and active playback immediately. Unlike the old
        // synchronized stop, this cannot wait for a 10-second audio chunk.
        playbackGeneration.incrementAndGet()
        track?.let {
            try { it.pause() } catch (_: Throwable) {}
            try { it.flush() } catch (_: Throwable) {}
            try { it.setVolume(1f) } catch (_: Throwable) {}
        }
    }
    @Synchronized fun release() {
        playbackGeneration.incrementAndGet()
        releaseLocked()
    }
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
    }

    private fun checkCancelled(cancelled: BooleanSupplier) {
        if (cancelled.asBoolean) throw CancellationException()
    }
}
