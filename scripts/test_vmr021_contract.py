#!/usr/bin/env python3
"""VMR-021 static contracts: these never claim phone installation or OCR quality."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
java=root/'app/src/main/java/fr/visionmaxread'
service=(java/'SpecialistOcrDownloadService.java').read_text()
ui=(java/'MainActivity.java').read_text()
installer=(java/'SpecialistOcrAutoInstaller.java').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
for token in (
    'extends Service', 'startForegroundService', 'startForeground(',
    'FOREGROUND_SERVICE_TYPE_DATA_SYNC','START_REDELIVER_INTENT',
    'NotificationChannel','PendingIntent.getService','cancelled::get',
    'getSharedPreferences("ocr_model_install"', 'SpecialistOcrAutoInstaller.ensure',
    '.putBoolean("complete",success)', '.putBoolean("active",false)',
    'newSingleThreadExecutor', 'Thread.sleep(1000)',
):
    assert token in service, 'missing foreground installer contract: '+token
for token in (
    'SpecialistOcrDownloadService.start', 'SpecialistOcrDownloadService.cancel',
    'installStatusWatcher','syncInstallStatus', 'sessions.documentCurrent(installTicket)',
):
    assert token in ui, 'missing UI contract: '+token
assert 'modelInstallWorker' not in ui, 'downloads must not depend on Activity worker'
assert 'modelInstallCancel.set(true)' not in ui, 'Activity destruction must not cancel weights'
assert 'Bitmap' not in service and 'document.jpg' not in service, 'no OCR pixels in service'
for token in (
    'wifi.openConnection(at)', 'requireUnmeteredNetwork(context)',
    'Content-Range', 'SHA-256', 'partial.renameTo(target)',
    'folder.getUsableSpace()', 'if(status==416&&resume>0)',
):
    assert token in installer, 'installer network or integrity contract: '+token
for token in (
    'android.permission.FOREGROUND_SERVICE',
    'android.permission.FOREGROUND_SERVICE_DATA_SYNC',
    'android:foregroundServiceType="dataSync"',
    'android:exported="false"',
):
    assert token in manifest, 'manifest contract: '+token
assert 'Nemotron' not in service, 'unported Nemotron must not be downloaded'
print('VMR-021 static FGS + Wi-Fi + privacy contracts OK; real phone tests remain pending')
