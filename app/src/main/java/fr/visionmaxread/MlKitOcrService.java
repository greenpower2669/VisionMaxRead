package fr.visionmaxread;

import android.graphics.Bitmap;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.TimeUnit;

/**
 * Bundled Latin OCR. No download, INTERNET permission, telemetry or original
 * document persistence. Never call from the Android UI thread.
 */
final class MlKitOcrService implements AutoCloseable {
    private final TextRecognizer recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    String recognize(Bitmap image) throws Exception {
        Text result = Tasks.await(
                recognizer.process(InputImage.fromBitmap(image, 0)),
                120, TimeUnit.SECONDS);
        return result.getText();
    }

    @Override public void close() {
        recognizer.close();
    }
}
