package fr.visionmaxread

import android.content.Context
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

/** Sherpa-ONNX/Piper UPMC medium. Jessica is speaker 0; Pierre is speaker 1. */
class PiperSpeechEngine : SpeechEngine {
    companion object {
        private const val ASSET_DIR = "tts/piper/jessica"
        private const val MODEL = "fr_FR-upmc-medium.onnx"
        private const val TOKENS = "tokens.txt"
        private const val DATA = "espeak-ng-data"
        private const val JESSICA_SID = 0
        private const val GUARD_MS = 250
        private const val MARKER = ".visionmaxread-sherpa-1.13.8-upmc"
        private const val WARM_TEXT = "Ah, bon."
    }

    private val player = PersistentVoicePcmPlayer()
    @Volatile private var tts: OfflineTts? = null
    @Volatile private var warmed = false
    @Volatile private var released = false

    @Synchronized override fun initialize(context: Context, status: Consumer<String>?) {
        check(!released) { "Piper engine released" }
        if (tts != null) return
        status?.accept("Chargement de Sherpa-ONNX / Piper Jessica…")
        val dataDir = prepareEspeakData(context.applicationContext)
        val vits = OfflineTtsVitsModelConfig(
            model = "$ASSET_DIR/$MODEL",
            tokens = "$ASSET_DIR/$TOKENS",
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
        status?.accept("Piper Jessica chargé.")
    }

    @Synchronized override fun warmUp(context: Context, cancelled: BooleanSupplier) {
        initialize(context, null)
        if (warmed && player.hasLiveTrack()) return
        checkCancelled(cancelled)
        val audio = requireNotNull(tts).generate(WARM_TEXT, JESSICA_SID, 1.0f)
        checkCancelled(cancelled)
        player.warm(audio.samples, audio.sampleRate, cancelled)
        warmed = true
    }

    override fun speak(context: Context, text: String, speed: Float, cancelled: BooleanSupplier, status: Consumer<String>) {
        if (text.isBlank()) return
        warmUp(context, cancelled)
        val engine = requireNotNull(tts)
        val chunks = TextChunks.split(text, 240)
        for ((index, segment) in chunks.withIndex()) {
            checkCancelled(cancelled)
            if (segment.isBlank()) continue
            status.accept("Jessica · passage " + (index + 1) + " / " + chunks.size)
            val audio = engine.generate(segment, JESSICA_SID, speed)
            checkCancelled(cancelled)
            player.play(audio.samples, audio.sampleRate, GUARD_MS, cancelled)
        }
    }

    @Synchronized fun diagnoseFirstPass(context: Context): String {
        initialize(context, null)
        val engine = requireNotNull(tts)
        val phrase = "Bonjour, ceci est un test."
        val a = engine.generate(phrase, JESSICA_SID, 1.0f)
        val b = engine.generate(phrase, JESSICA_SID, 1.0f)
        return "A=" + a.samples.size + "/" + hash(a.samples) + " B=" + b.samples.size + "/" + hash(b.samples)
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
        val root = File(context.filesDir, "piper_jessica")
        val destination = File(root, DATA)
        val marker = File(destination, MARKER)
        if (!marker.exists()) {
            if (destination.exists()) destination.deleteRecursively()
            destination.mkdirs()
            copyAssetTree(context, "$ASSET_DIR/$DATA", destination)
            marker.writeText("sherpa-onnx-1.13.8;upmc-medium;sid=0")
        }
        context.assets.open("$ASSET_DIR/$MODEL").use { }
        context.assets.open("$ASSET_DIR/$TOKENS").use { }
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
