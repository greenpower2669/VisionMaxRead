package fr.visionmaxread;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
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


    static void thermal(Context context, String stage) {
        String thermal = "unsupported";
        int thermalCode = -1;
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (power != null) {
                    thermalCode = power.getCurrentThermalStatus();
                    thermal = thermalName(thermalCode);
                }
            } catch (Exception ignored) {}
        }

        float batteryTempC = Float.NaN;
        int batteryPercent = -1;
        int plugged = -1;
        try {
            Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                if (temp != Integer.MIN_VALUE) batteryTempC = temp / 10f;
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) batteryPercent = Math.round(level * 100f / scale);
                plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            }
        } catch (Exception ignored) {}

        append(context, "THERMAL " + stage +
            " status=" + thermal + "(" + thermalCode + ")" +
            " batteryTempC=" + (Float.isNaN(batteryTempC) ? "n/d" : String.format(Locale.US, "%.1f", batteryTempC)) +
            " battery=" + (batteryPercent < 0 ? "n/d" : batteryPercent + "%") +
            " plugged=" + plugged);
    }

    private static String thermalName(int value) {
        if (Build.VERSION.SDK_INT < 29) return "unsupported";
        switch (value) {
            case PowerManager.THERMAL_STATUS_NONE: return "NONE";
            case PowerManager.THERMAL_STATUS_LIGHT: return "LIGHT";
            case PowerManager.THERMAL_STATUS_MODERATE: return "MODERATE";
            case PowerManager.THERMAL_STATUS_SEVERE: return "SEVERE";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "CRITICAL";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "EMERGENCY";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "SHUTDOWN";
            default: return "UNKNOWN";
        }
    }

    static void session(Context context) {
        String versionName = "?";
        long versionCode = -1;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName == null ? "?" : info.versionName;
            versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        } catch (Exception ignored) {}
        append(context, "SESSION start package=" + context.getPackageName() +
            " version=" + versionName + " code=" + versionCode +
            " sdk=" + Build.VERSION.SDK_INT +
            " device=" + Build.MANUFACTURER + "/" + Build.MODEL +
            " abi=" + (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown"));
        thermal(context, "SESSION start");
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }
}
