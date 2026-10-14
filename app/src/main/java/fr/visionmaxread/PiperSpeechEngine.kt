package fr.visionmaxread

import android.content.Context
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import fr.visionmaxread.core.TextChunks
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import kotlin.math.abs

/** Offline Sherpa-ONNX/Piper: Siwis articulation mode or original Jessica fallback. */
class PiperSpeechEngine : SpeechEngine {
    companion object {
        private const val ASSET_DIR = "tts/piper/jessica"
        private const val MODEL = "fr_FR-upmc-medium.onnx"
        private const val HQ_ASSET_DIR = "tts/piper/siwis"
        private const val HQ_MODEL = "fr_FR-siwis-medium.onnx"
        private const val TOKENS = "tokens.txt"
        private const val DATA = "espeak-ng-data"
        private const val VOICE_SID = 0
        private const val GUARD_MS = 250
        private const val MARKER = ".visionmaxread-sherpa-1.13.4-upmc"
        private const val WARM_TEXT = "Ah, bon."
    }

    private val player = PersistentVoicePcmPlayer()
    @Volatile private var highQuality = true

    fun voiceName(): String = if (highQuality) "Siwis" else "Jessica"
    private fun currentAssetDir(): String = if (highQuality) HQ_ASSET_DIR else ASSET_DIR
    private fun currentModel(): String = if (highQuality) HQ_MODEL else MODEL

    /** Called between utterances, on the serial speech worker. Never keep two TTS engines. */
    @Synchronized fun selectVoice(hiFi: Boolean) {
        if (highQuality == hiFi) return
        tts?.release()
        tts = null
        player.release()
        warmed = false
        firstPassDiagnostic = null
        highQuality = hiFi
    }
    @Volatile private var tts: OfflineTts? = null
    @Volatile private var warmed = false
    @Volatile private var released = false
    @Volatile private var lastInitializeMs = -1L
    @Volatile private var firstWarmUpMs = -1L
    @Volatile private var lastWarmUpMs = -1L
    @Volatile private var lastSpeechMs = -1L
    @Volatile private var firstPassDiagnostic: String? = null

    @Synchronized override fun initialize(context: Context, status: Consumer<String>?) {
        check(!released) { "Piper engine released" }
        if (tts != null) return
        status?.accept("Chargement de Sherpa-ONNX / Piper " + voiceName() + "…")
        val started = SystemClock.elapsedRealtime()
        val dataDir = prepareEspeakData(context.applicationContext)
        val assetDir = currentAssetDir()
        val vits = OfflineTtsVitsModelConfig(
            model = "$assetDir/${currentModel()}",
            tokens = "$assetDir/$TOKENS",
            dataDir = dataDir.absolutePath
        )
        tts = OfflineTts(
            assetManager = context.assets,
            config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(vits = vits, numThreads = 2, debug = false, provider = "cpu"),
                maxNumSentences = 1,
                silenceScale = 0.2f
            )
        )
        lastInitializeMs = SystemClock.elapsedRealtime() - started
        status?.accept("Piper " + voiceName() + " chargé.")
    }

    @Synchronized override fun warmUp(context: Context, cancelled: BooleanSupplier) {
        initialize(context, null)
        if (warmed && player.hasLiveTrack()) return
        checkCancelled(cancelled)
        val started = SystemClock.elapsedRealtime()
        val engine = requireNotNull(tts)
        val audio = if (firstPassDiagnostic == null) {
            val firstStart = SystemClock.elapsedRealtime()
            val first = engine.generate(WARM_TEXT, VOICE_SID, 1.0f)
            val firstMs = SystemClock.elapsedRealtime() - firstStart
            checkCancelled(cancelled)
            val secondStart = SystemClock.elapsedRealtime()
            val second = engine.generate(WARM_TEXT, VOICE_SID, 1.0f)
            val secondMs = SystemClock.elapsedRealtime() - secondStart
            val firstHash = hash(first.samples)
            val secondHash = hash(second.samples)
            val verdict = if (firstHash == secondHash && first.samples.size == second.samples.size)
                "PCM identiques : si le début audible manque, piste Android/AudioTrack à examiner."
            else "PCM différents : génération Piper à examiner."
            firstPassDiagnostic =
                "PCM1=" + first.samples.size + ", gen=" + firstMs + " ms, signal=" + leadingSignalMs(first.samples, first.sampleRate) + " ms, " + firstHash +
                "; PCM2=" + second.samples.size + ", gen=" + secondMs + " ms, signal=" + leadingSignalMs(second.samples, second.sampleRate) + " ms, " + secondHash +
                "; fréquence=" + second.sampleRate + " Hz. " + verdict
            second
        } else {
            engine.generate(WARM_TEXT, VOICE_SID, 1.0f)
        }
        checkCancelled(cancelled)
        player.warm(audio.samples, audio.sampleRate, cancelled)
        lastWarmUpMs = SystemClock.elapsedRealtime() - started
        if (firstWarmUpMs < 0) firstWarmUpMs = lastWarmUpMs
        warmed = true
    }

    override fun speak(context: Context, text: String, speed: Float, cancelled: BooleanSupplier, status: Consumer<String>) {
        if (text.isBlank()) return
        warmUp(context, cancelled)
        val engine = requireNotNull(tts)
        val speechStarted = SystemClock.elapsedRealtime()
        val chunks = TextChunks.split(text, 240)
        for ((index, segment) in chunks.withIndex()) {
            checkCancelled(cancelled)
            if (segment.isBlank()) continue
            status.accept(voiceName() + " · passage " + (index + 1) + " / " + chunks.size)
            val audio = engine.generate(segment, VOICE_SID, speed)
            checkCancelled(cancelled)
            player.play(audio.samples, audio.sampleRate, if (index == 0) GUARD_MS else 0, cancelled)
        }
        lastSpeechMs = SystemClock.elapsedRealtime() - speechStarted
    }

    @Synchronized fun diagnoseFirstPass(context: Context): String {
        warmUp(context, BooleanSupplier { false })
        return "init=" + lastInitializeMs + " ms, première amorce=" + firstWarmUpMs + " ms, dernière amorce=" + lastWarmUpMs +
            " ms, dernière lecture=" + lastSpeechMs + " ms; " + (firstPassDiagnostic ?: "PCM initial non mesuré")
    }

    @Synchronized override fun stop() {
        player.stop()
        warmed = false
    }

    @Synchronized override fun release() {
        if (released) return
        released = true
        player.release()
        tts?.release()
        tts = null
        warmed = false
    }

    override fun isReady(): Boolean = tts != null && !released

    private fun prepareEspeakData(context: Context): File {
        val root = File(context.filesDir, if (highQuality) "piper_siwis" else "piper_jessica")
        val assetDir = currentAssetDir()
        val destination = File(root, DATA)
        val marker = File(destination, MARKER + if (highQuality) "-siwis" else "-upmc")
        if (!marker.exists()) {
            if (destination.exists()) destination.deleteRecursively()
            destination.mkdirs()
            copyAssetTree(context, "$assetDir/$DATA", destination)
            marker.writeText("sherpa-onnx-1.13.4;" + voiceName() + ";sid=0")
        }
        context.assets.open("$assetDir/${currentModel()}").use { }
        context.assets.open("$assetDir/$TOKENS").use { }
        return destination
    }

    private fun copyAssetTree(context: Context, assetPath: String, destination: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output, 64 * 1024) }
            }
            return
        }
        destination.mkdirs()
        for (name in children) copyAssetTree(context, "$assetPath/$name", File(destination, name))
    }

    private fun leadingSignalMs(samples: FloatArray, sampleRate: Int): Int {
        if (sampleRate <= 0) return -1
        val index = samples.indexOfFirst { abs(it) > 0.001f }
        return if (index < 0) -1 else (index * 1000L / sampleRate).toInt()
    }

    private fun hash(samples: FloatArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            buffer.clear()
            buffer.putFloat(sample)
            digest.update(buffer.array())
        }
        return digest.digest().take(6).joinToString("") { "%02x".format(it) }
    }

    private fun checkCancelled(cancelled: BooleanSupplier) {
        if (cancelled.asBoolean) throw CancellationException()
    }
}
