#!/usr/bin/env python3
"""VMR-019 static source contracts; phone OCR still requires a device test."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
src=root/'app/src/main/java/fr/visionmaxread'
main=(src/'MainActivity.java').read_text()
models=(src/'SpecialistOcrModels.java').read_text()
native=(src/'SpecialistOcrProcess.java').read_text()
service=(src/'SpecialistOcrInstallService.java').read_text()
ci=(root/'.github/workflows/android.yml').read_text()
verify=(root/'scripts/verify_apk.py').read_text()
assert 'SpecialistOcrAutoInstaller.ensure' in service and 'SpecialistOcrModels.importFile' not in main
assert 'specialistOcr.recognize' in main
assert 'showComparisonText' in main
assert 'ProcessBuilder' in native and '--image' in native and '--mmproj' in native
assert 'SpecialistOcrModels.verified' in native
assert 'build_specialist_ocr_android.sh' in ci
assert 'libvmr_mtmd_cli.so' in verify
for sha in ('bb57c9769af18d02f0bde4dd0840737cae170d7340cc0435eddcc36b443459df',
 '101acc9466a587a615bbf7b29c0ecb6744e309363eba8b615fe3e09f5e1c5d5f',
 '1f18abe17b1ed8b4e47ee9b1ad0e274c93daf5efbb6b29a04ff1712e37051e05',
 '98c0563673b1657ff6d021d1e5f04af06cbf61bb40c63ac613e8bb71b42fb2c0'):
 assert sha in models
print('VMR-019 static contracts OK; actual device OCR still unverified')
