#!/usr/bin/env python3
"""Source regression test; not a proof of Android OCR model execution."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
main=(root/'app/src/main/java/fr/visionmaxread/MainActivity.java').read_text()
catalog=(root/'app/src/main/java/fr/visionmaxread/OcrEngineCatalog.java').read_text()
ci=(root/'.github/workflows/android.yml').read_text()
verify=(root/'scripts/verify_apk.py').read_text()
gradle=(root/'app/build.gradle').read_text()
assert all(x not in main for x in ('LiquidNative','HobbyMoe','OfflineCorrectionModels','TextualDocumentReview','MoeStreamDialog'))
assert all(x in catalog for x in ('Surya OCR 2','LightOnOCR-3 0.8B','Nemotron 3.5 Parse'))
assert 'Non installé, non exécutable' in catalog
assert all(x not in ci for x in ('prepare_liquid.py','prepare_hobby_moe.py','build_liquid_android.sh','build_hobby_moe_android.sh'))
assert 'assets/liquid/' in verify and 'libvmr_moe.so' in verify
assert any('versionCode '+str(v) in gradle for v in (21,22,23,24))
print('VMR-018 static contract OK. OCR image backends still unavailable.')
