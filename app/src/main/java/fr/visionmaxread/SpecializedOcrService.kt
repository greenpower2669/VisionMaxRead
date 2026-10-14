package fr.visionmaxread

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.StatFs
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.function.Consumer
import java.util.function.IntConsumer

/**
 * Optional local-only specialist OCR. The user imports the independently pinned
 * .litertlm file through Android's document picker; no app network permission.
 * Engine orchestration is serialized on the same worker as Qwen.
 */
class SpecializedOcrService {
    companion object {
        const val MODEL_NAME = "PaddleOCR-VL-1.6.litertlm"
        const val EXPECTED_SIZE = 1_390_305_472L
        const val EXPECTED_SHA256 =
            "314df8b34d6911990d488e825602550a5bbcc5d6700cb1a8914e853e9043f4f1"
    }

    @Volatile private var engine: Engine? = null
    @Volatile private var released = false

    private fun modelFile(context: Context) =
        File(File(context.applicationContext.filesDir, "ocr_specialist"), MODEL_NAME)

    fun isInstalled(context: Context): Boolean {
        val file = modelFile(context)
        return file.isFile && file.length() == EXPECTED_SIZE
    }

    /** Atomic import. The old verified model stays intact if a new import fails. */
    @Synchronized fun importModel(context: Context, uri: Uri, progress: IntConsumer) {
        check(!released) { "Specialist engine released" }
        if (isInstalled(context)) { progress.accept(100); return }
        val destination = modelFile(context)
        val parent = requireNotNull(destination.parentFile)
        check(parent.exists() || parent.mkdirs()) { "Cannot create private model directory" }
        val free = StatFs(parent.absolutePath).availableBytes
        check(free > EXPECTED_SIZE + 96L * 1024 * 1024) {
            "Not enough space for specialist OCR model"
        }
        val temp = File(parent, "$MODEL_NAME.part")
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                checkNotNull(input) { "Cannot open selected model" }
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var previousPercent = -1
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (n == 0) continue
                        size += n
                        check(size <= EXPECTED_SIZE) { "Selected model is too large" }
                        digest.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                        val percent = (size * 100 / EXPECTED_SIZE).toInt()
                        if (percent != previousPercent) {
                            progress.accept(percent)
                            previousPercent = percent
                        }
                    }
                    output.fd.sync()
                }
            }
            check(size == EXPECTED_SIZE) { "Unexpected specialist model size: $size" }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(actual == EXPECTED_SHA256) { "Specialist model SHA-256 mismatch" }
            check(temp.renameTo(destination)) { "Could not install specialist OCR model" }
            DebugLog.append(context, "SPECIALIST model import verified bytes=$size")
            progress.accept(100)
        } finally {
            temp.delete()
        }
    }

    @Synchronized private fun ensureEngine(context: Context, status: Consumer<String>): Engine {
        engine?.let { return it }
        check(!released) { "Specialist engine released" }
        val model = modelFile(context)
        check(isInstalled(context)) { "Specialist model is not installed" }
        val cache = context.cacheDir
        check(cache.isDirectory && cache.canWrite()) { "LiteRT cache not writable" }
        status.accept("Initialisation de PaddleOCR-VL-1.6 sur CPU…")
        DebugLog.append(context, "SPECIALIST engine initialize start")
        var created: Engine? = null
        try {
            created = Engine(EngineConfig(
                modelPath = model.absolutePath,
                backend = Backend.CPU(),
                visionBackend = Backend.CPU(),
                maxNumTokens = 4096,
                maxNumImages = 1,
                cacheDir = cache.absolutePath
            ))
            runBlocking { created.initialize() }
        } catch (error: Throwable) {
            try { created?.close() } catch (_: Throwable) {}
            DebugLog.exception(context, "SPECIALIST engine initialize", error)
            throw error
        }
        val ready = checkNotNull(created)
        engine = ready
        DebugLog.append(context, "SPECIALIST engine initialize success")
        return ready
    }

    fun recognize(context: Context, image: Bitmap, status: Consumer<String>): String {
        val jpeg = ByteArrayOutputStream()
        check(image.compress(Bitmap.CompressFormat.JPEG, 95, jpeg)) {
            "Specialist image encoding failed"
        }
        val current = ensureEngine(context, status)
        status.accept("PaddleOCR-VL-1.6 reconnaît le texte…")
        // This model is task-prompted; a generic Qwen instruction may return nothing.
        val result = runBlocking {
            current.createConversation().use { conversation ->
                conversation.sendMessage(
                    Contents.of(Content.ImageBytes(jpeg.toByteArray()), Content.Text("OCR:"))
                ).toString()
            }
        }
        DebugLog.append(context, "SPECIALIST done chars=" + result.length)
        return result.trim().removePrefix("&#x200B;").trim()
    }

    @Synchronized fun unload() {
        val previous = engine
        engine = null
        if (previous != null) try { previous.close() } catch (_: Throwable) {}
    }

    @Synchronized fun release() {
        released = true
        unload()
    }
}
