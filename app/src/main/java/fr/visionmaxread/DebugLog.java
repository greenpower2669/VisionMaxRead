package fr.visionmaxread;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Small private diagnostic journal. Never log OCR text or image bytes here.
 * The file lives in app-private storage and is capped/rotated.
 */
final class DebugLog {
    private static final String TAG = "VisionMaxRead";
    private static final String FILE = "vmr-debug.log";
    private static final long MAX_BYTES = 512L * 1024L;
    private static final Object LOCK = new Object();

    private DebugLog() {}

    static void append(Context context, String message) {
        String line = timestamp() + " | " + Thread.currentThread().getName() + " | " + message;
        Log.d(TAG, line);
        synchronized (LOCK) {
            try {
                File file = new File(context.getFilesDir(), FILE);
                if (file.isFile() && file.length() > MAX_BYTES) {
                    File old = new File(context.getFilesDir(), FILE + ".old");
                    old.delete();
                    file.renameTo(old);
                }
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(line);
                    writer.write('\n');
                }
            } catch (IOException ignored) {
                // Diagnostics must never break the app.
            }
        }
    }

    static void exception(Context context, String stage, Throwable error) {
        String type = error == null ? "null" : error.getClass().getSimpleName();
        String message = error == null || error.getMessage() == null ? "" : error.getMessage().replace('\n', ' ');
        append(context, stage + " ERROR " + type + (message.isEmpty() ? "" : ": " + message));
    }

    static String read(Context context) {
        synchronized (LOCK) {
            File file = new File(context.getFilesDir(), FILE);
            if (!file.isFile()) return "Journal debug vide.";
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) out.append(line).append('\n');
                return out.toString();
            } catch (IOException e) {
                return "Journal debug illisible : " + e.getClass().getSimpleName();
            }
        }
    }

    static void clear(Context context) {
        synchronized (LOCK) {
            new File(context.getFilesDir(), FILE).delete();
            new File(context.getFilesDir(), FILE + ".old").delete();
        }
        append(context, "DEBUG journal cleared");
    }

    static void session(Context context) {
        append(context, "SESSION start sdk=" + Build.VERSION.SDK_INT +
            " device=" + Build.MANUFACTURER + "/" + Build.MODEL +
            " abi=" + (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown"));
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }
}
