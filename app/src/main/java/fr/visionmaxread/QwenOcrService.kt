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

        const val PROFILE_AUTO_TURBO = "auto_gpu_gpu"
        const val PROFILE_GPU_GPU = "gpu_gpu"
        const val PROFILE_GPU_CPU = "gpu_cpu"
        const val PROFILE_CPU_GPU = "cpu_gpu"
        const val PROFILE_CPU4 = "cpu4"
        const val PROFILE_CPU2 = "cpu2"
        const val PROFILE_CPU_DEFAULT = "cpu_default"
    }

    @Volatile private var engine: Engine? = null
    @Volatile private var released = false
    @Volatile private var activeRequestedProfile = ""
    @Volatile private var activeRuntimeProfile = ""

    private data class ProfileSpec(
        val key: String,
        val label: String,
        val backend: Backend,
        val visionBackend: Backend
    )

    private fun normalizeProfile(value: String?): String = when (value) {
        PROFILE_GPU_GPU, PROFILE_GPU_CPU, PROFILE_CPU_GPU,
        PROFILE_CPU4, PROFILE_CPU2, PROFILE_CPU_DEFAULT -> value
        else -> PROFILE_AUTO_TURBO
    }

    private fun specFor(key: String): ProfileSpec = when (key) {
        PROFILE_GPU_GPU -> ProfileSpec(key, "GPU langage + GPU vision", Backend.GPU(), Backend.GPU())
        PROFILE_GPU_CPU -> ProfileSpec(key, "GPU langage + CPU vision", Backend.GPU(), Backend.CPU())
        PROFILE_CPU_GPU -> ProfileSpec(key, "CPU 4 threads + GPU vision", Backend.CPU(threadCount = 4), Backend.GPU())
        PROFILE_CPU2 -> ProfileSpec(key, "CPU 2 threads + CPU vision", Backend.CPU(threadCount = 2), Backend.CPU())
        PROFILE_CPU_DEFAULT -> ProfileSpec(key, "CPU auto + CPU vision", Backend.CPU(), Backend.CPU())
        else -> ProfileSpec(PROFILE_CPU4, "CPU 4 threads + CPU vision", Backend.CPU(threadCount = 4), Backend.CPU())
    }

    @Synchronized private fun ensureEngine(
        context: Context,
        status: Consumer<String>,
        progress: java.util.function.IntConsumer,
        requestedProfileRaw: String?
    ): Engine {
        val requestedProfile = normalizeProfile(requestedProfileRaw)
        engine?.let {
            if (activeRequestedProfile == requestedProfile) return it
            DebugLog.append(context, "QWEN profile changed requested=" + requestedProfile + " previous=" + activeRequestedProfile + " -> recreate engine")
            try { it.close() } catch (_: Throwable) {}
            engine = null
            activeRequestedProfile = ""
            activeRuntimeProfile = ""
        }
        check(!released) { "Qwen engine released" }

        progress.accept(5)
        val model = ensureModelFile(context.applicationContext, status, progress)
        progress.accept(50)
        val cacheDir = context.cacheDir
        check(cacheDir.isDirectory) { "Android cache directory is missing: " + cacheDir.absolutePath }
        check(cacheDir.canWrite()) { "Android cache directory is not writable: " + cacheDir.absolutePath }
        DebugLog.append(context, "QWEN cache ready path=" + cacheDir.absolutePath + " exists=" + cacheDir.exists() + " writable=" + cacheDir.canWrite())

        val candidates = if (requestedProfile == PROFILE_AUTO_TURBO) {
            listOf(
                ProfileSpec(PROFILE_GPU_GPU, "AUTO Turbo GPU/GPU", Backend.GPU(), Backend.GPU()),
                specFor(PROFILE_CPU4)
            )
        } else if (requestedProfile == PROFILE_GPU_GPU || requestedProfile == PROFILE_GPU_CPU || requestedProfile == PROFILE_CPU_GPU) {
            listOf(specFor(requestedProfile), specFor(PROFILE_CPU4))
        } else {
            listOf(specFor(requestedProfile))
        }

        var lastError: Throwable? = null
        for ((index, spec) in candidates.withIndex()) {
            status.accept("Initialisation Qwen : " + spec.label + "…")
            DebugLog.thermal(context, "QWEN init before requested=" + requestedProfile + " attempt=" + spec.key)
            DebugLog.append(context, "QWEN engine initialize start requested=" + requestedProfile + " attempt=" + spec.key + " label=" + spec.label)
            var created: Engine? = null
            try {
                created = Engine(
                    EngineConfig(
                        modelPath = model.absolutePath,
                        backend = spec.backend,
                        visionBackend = spec.visionBackend,
                        maxNumTokens = 2048,
                        maxNumImages = 1,
                        cacheDir = cacheDir.absolutePath
                    )
                )
                runBlocking { created.initialize() }
                engine = created
                activeRequestedProfile = requestedProfile
                activeRuntimeProfile = spec.key
                progress.accept(60)
                status.accept("Qwen prêt : " + spec.label + ".")
                DebugLog.append(context, "QWEN engine initialize success requested=" + requestedProfile + " active=" + spec.key)
                DebugLog.thermal(context, "QWEN init after active=" + spec.key)
                return created
            } catch (t: Throwable) {
                lastError = t
                try { created?.close() } catch (_: Throwable) {}
                DebugLog.exception(context, "QWEN engine initialize profile=" + spec.key, t)
                DebugLog.thermal(context, "QWEN init failed profile=" + spec.key)
                if (index + 1 < candidates.size) {
                    DebugLog.append(context, "QWEN fallback from=" + spec.key + " to=" + candidates[index + 1].key)
                    status.accept("Mode " + spec.label + " indisponible ; repli CPU 4 threads…")
                }
            }
        }
        throw checkNotNull(lastError)
    }

    fun recognize(
        context: Context,
        bitmap: Bitmap,
        status: Consumer<String>,
        progress: java.util.function.IntConsumer,
        requestedProfile: String?
    ): String {
        val jpeg = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, jpeg)) { "Image encoding failed" }
        DebugLog.append(context, "QWEN recognize start image=" + bitmap.width + "x" + bitmap.height + " requestedProfile=" + normalizeProfile(requestedProfile))
        DebugLog.thermal(context, "QWEN inference before")
        val current = ensureEngine(context, status, progress, requestedProfile)
        val prompt = "Transcris exactement tout le texte visible dans cette image. " +
            "Réponds uniquement avec le texte transcrit, sans introduction, commentaire ni balises Markdown. " +
            "Respecte autant que possible les retours à la ligne, accents, nombres et ponctuation. " +
            "Ne décris pas l'image. Si aucun texte n'est lisible, réponds par une chaîne vide."
        status.accept("Qwen lit l’image…")
        progress.accept(-1)
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
        val cleaned = clean(result)
        progress.accept(100)
        DebugLog.append(context, "QWEN recognize success chars=" + cleaned.length + " activeProfile=" + activeRuntimeProfile)
        DebugLog.thermal(context, "QWEN inference after active=" + activeRuntimeProfile)
        return cleaned
    }

    @Synchronized fun release() {
        if (released) return
        released = true
        try { engine?.close() } catch (_: Throwable) {}
        engine = null
        activeRequestedProfile = ""
        activeRuntimeProfile = ""
    }

    fun isReady(): Boolean = engine != null && !released

    private fun ensureModelFile(context: Context, status: Consumer<String>, progress: java.util.function.IntConsumer): File {
        val dir = File(context.filesDir, "qwen")
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, LOCAL)
        if (target.isFile && target.length() == EXPECTED_SIZE) {
            progress.accept(45)
            DebugLog.append(context, "QWEN model already installed bytes=" + target.length())
            return target
        }
        target.delete()
        status.accept("Première utilisation : préparation locale de Qwen (≈ 1,3 Go)…")
        DebugLog.append(context, "QWEN model copy start expectedBytes=" + EXPECTED_SIZE)
        val temp = File(dir, "$LOCAL.part")
        temp.delete()
        context.assets.open(ASSET).use { input ->
            FileOutputStream(temp).use { output ->
                val buffer = ByteArray(1024 * 1024)
                var copied = 0L
                var lastPercent = -1
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) {
                        output.write(buffer, 0, read)
                        copied += read
                        val percent = ((copied * 100L) / EXPECTED_SIZE).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            status.accept("Préparation locale de Qwen : " + percent + " %")
                            progress.accept(5 + (percent * 40 / 100))
                        }
                    }
                }
                output.fd.sync()
            }
        }
        check(temp.length() == EXPECTED_SIZE) { "Qwen model size mismatch: " + temp.length() }
        check(temp.renameTo(target)) { "Unable to install Qwen model locally" }
        progress.accept(45)
        DebugLog.append(context, "QWEN model copy success bytes=" + target.length())
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
