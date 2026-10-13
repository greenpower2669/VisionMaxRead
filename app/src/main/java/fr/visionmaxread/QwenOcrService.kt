package fr.visionmaxread

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.function.Consumer

/** Primary OCR: Qwen3.5-0.8B-VL through LiteRT-LM, fully on-device. */
class QwenOcrService {
    companion object {
        private const val ASSET = "qwen/Qwen3.5-0.8B-VL_int8.litertlm"
        private const val EXPECTED_SIZE = 1_303_966_384L
        private const val LOCAL = "Qwen3.5-0.8B-VL_int8.litertlm"
    }

    @Volatile private var engine: Engine? = null
    @Volatile private var released = false

    @Synchronized private fun ensureEngine(context: Context, status: Consumer<String>): Engine {
        engine?.let { return it }
        check(!released) { "Qwen engine released" }
        val model = ensureModelFile(context.applicationContext, status)
        status.accept("Initialisation de Qwen3.5 Vision sur CPU…")
        val created = Engine(
            EngineConfig(
                modelPath = model.absolutePath,
                backend = Backend.CPU(),
                visionBackend = Backend.CPU(),
                maxNumTokens = 2048,
                maxNumImages = 1,
                cacheDir = File(context.cacheDir, "qwen_litert_cache").absolutePath
            )
        )
        runBlocking { created.initialize() }
        engine = created
        status.accept("Qwen prêt.")
        return created
    }

    fun recognize(context: Context, bitmap: Bitmap, status: Consumer<String>): String {
        val jpeg = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, jpeg)) { "Image encoding failed" }
        val current = ensureEngine(context, status)
        val prompt = "Transcris exactement tout le texte visible dans cette image. " +
            "Réponds uniquement avec le texte transcrit, sans introduction, commentaire ni balises Markdown. " +
            "Respecte autant que possible les retours à la ligne, accents, nombres et ponctuation. " +
            "Ne décris pas l'image. Si aucun texte n'est lisible, réponds par une chaîne vide."
        status.accept("Qwen lit l’image…")
        val result = runBlocking {
            current.createConversation().use { conversation ->
                conversation.sendMessage(
                    Contents.of(
                        Content.ImageBytes(jpeg.toByteArray()),
                        Content.Text(prompt)
                    )
                ).toString()
            }
        }
        return clean(result)
    }

    @Synchronized fun release() {
        if (released) return
        released = true
        engine?.close()
        engine = null
    }

    fun isReady(): Boolean = engine != null && !released

    private fun ensureModelFile(context: Context, status: Consumer<String>): File {
        val dir = File(context.filesDir, "qwen")
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, LOCAL)
        if (target.isFile && target.length() == EXPECTED_SIZE) return target
        target.delete()
        status.accept("Première utilisation : préparation locale de Qwen (≈ 1,3 Go)…")
        val temp = File(dir, "$LOCAL.part")
        temp.delete()
        context.assets.open(ASSET).use { input ->
            FileOutputStream(temp).use { output ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) output.write(buffer, 0, read)
                }
                output.fd.sync()
            }
        }
        check(temp.length() == EXPECTED_SIZE) { "Qwen model size mismatch: " + temp.length() }
        check(temp.renameTo(target)) { "Unable to install Qwen model locally" }
        return target
    }

    private fun clean(value: String): String {
        var text = value.trim()
        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.removePrefix("```").removeSuffix("```").trim()
            if (text.startsWith("text\n")) text = text.removePrefix("text\n")
        }
        return text.trim()
    }
}
