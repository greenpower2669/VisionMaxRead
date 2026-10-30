#!/usr/bin/env python3
"""Build-time VMR-010 invariants. Does not claim on-device model execution."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
app=root/"app/src/main/java/fr/visionmaxread"
main=(app/"MainActivity.java").read_text()
indra=(app/"IndraOcrService.java").read_text()
order=(app/"IndraReadingOrder.java").read_text()
trace=(app/"OcrTrace.java").read_text()
gradle=(root/"app/build.gradle").read_text()
workflow=(root/".github/workflows/android.yml").read_text()
prepare=(root/"scripts/prepare_indra_models.py").read_text()
assert "Qwen" not in main and "qwen" not in main
assert "qwen" not in workflow.lower() and "qwen" not in gradle.lower().replace('// qwen and its litert-lm model/runtime deliberately removed from vmr-010.','')
assert not (app/"QwenOcrService.kt").exists()
assert not (app/"SpecializedOcrService.kt").exists()
assert "litertlm-android" not in gradle
assert "order.onnx" in prepare and "a42db2e8b536ad3d84045a4b7f9b0a72667cf8be4aa02d63f7b38004719fb180" in prepare
assert "order.onnx" in indra and 'Map.of("feats",inp,"mask",m)' in order
for stage in ("P1_INDRA_RAW","P2_ORDERNET","P2B_INDRA_LM","P3_LIQUID","P4_MOBILE_MOE","P5_ARBITRAGE"):
 assert stage in main,stage
assert 'p.status.equals("executed")' in trace
assert 'p.status.equals("interrupted")' in trace, 'partial output must be explicitly labeled'
assert "p.text" not in trace.split("DebugLog.append(context")[1].split(";")[0], "text leaked to debug log"
assert "sans réseau" not in main or "INTERNET" not in (root/"app/src/main/AndroidManifest.xml").read_text()
print("VMR-010 contract: no Qwen, offline Indra+OrderNet, honest semantic statuses, private logs")
