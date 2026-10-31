#!/usr/bin/env python3
"""Static VMR-020 safety and usability contract. Actual Android downloads require device tests."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
src=root/'app/src/main/java/fr/visionmaxread'
installer=(src/'SpecialistOcrAutoInstaller.java').read_text()
main=(src/'MainActivity.java').read_text()
models=(src/'SpecialistOcrModels.java').read_text()
native=(src/'SpecialistOcrProcess.java').read_text()
service=(src/'SpecialistOcrInstallService.java').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
verify=(root/'scripts/verify_apk.py').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()
gradle=(root/'app/build.gradle').read_text()
for required in ('HttpURLConnection','SHA-256','getUsableSpace','Content-Range',
                 'Range','partial.renameTo(target)','CancellationException',
                 'NET_CAPABILITY_NOT_METERED','TRANSPORT_WIFI','trustedHost','https','cloudfront.net'):
    assert required in installer, required
for required in ('SpecialistOcrInstallService.install','requestAutoInstall',
                 'runSpecialistOcr','beginAutoInstall','syncInstallState'):
    assert required in main, required
for required in ('SpecialistOcrAutoInstaller.ensure','startForeground',
                 'START_REDELIVER_INTENT','awaitNetwork','NotificationChannel',
                 'onTimeout','ACTION_CANCEL'):
    assert required in service, required
assert 'modelInstallWorker' not in main
assert 'modelInstallCancel' not in main
assert 'chooseSpecialistFile' not in main
assert 'IMPORT_SURYA_MODEL' not in main
assert 'SpecialistOcrModels.importFile' not in main
assert 'SpecialistOcrModels.verified' in native
assert 'android.permission.INTERNET' in manifest
assert 'android.permission.ACCESS_NETWORK_STATE' in manifest
assert 'usesCleartextTraffic="false"' in manifest
assert 'android:foregroundServiceType="dataSync"' in manifest
assert 'android.permission.FOREGROUND_SERVICE_DATA_SYNC' in manifest
assert 'selectedUnmeteredNetwork(context).openConnection(at)' in installer
assert "android.permission.INTERNET' not in result" in verify
assert 'versionCode 23' in gradle
assert 'test_vmr020_contract.py' in workflow
assert 'model-Q4_K_M.gguf' in models and 'surya-2.gguf' in models
print("VMR-021 static provisioning/foreground-service contract OK; Android network and image OCR unmeasured")
