package fr.visionmaxread
import android.content.Context
import android.graphics.Bitmap
import com.paddle.ocr.EngineConfig
import com.paddle.ocr.PaddleOCRConfig
import com.paddle.ocr.engine.OCREngine
import com.paddle.ocr.util.OpenCVUtils

/** Synchronous adapter, called exclusively on the single inference worker. */
class OcrService {
    fun recognize(context: Context, bitmap: Bitmap): String {
        check(OpenCVUtils.init(context)) { "OpenCV indisponible" }
        val engine = OCREngine(context, PaddleOCRConfig(detLimitSideLen=1280, detLimitType="max", detMaxSideLimit=2560), EngineConfig(numThreads=2))
        return try { engine.run(bitmap).results.joinToString("\n") { it.text } }
        finally { engine.release() }
    }
}
