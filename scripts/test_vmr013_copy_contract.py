#!/usr/bin/env python3
"""VMR-013 UI/privacy guardrails; run before Android compilation."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
p=root/"app/src/main/java/fr/visionmaxread"
ui=(p/"MainActivity.java").read_text(encoding="utf-8")
trace=(p/"OcrTrace.java").read_text(encoding="utf-8")
for button in ("Copier logs techniques","Copier résultats · toutes les passes","Copier une passe"):
 assert button in ui,button
assert "trace.metricsReport()" in ui
assert "trace.passReport(which)" in ui
assert "trace.fullReport()" in ui
assert "confirmSensitiveCopy" in ui
assert "setPrimaryClip" in ui
assert "sessions.documentCurrent(ticket)" in ui
assert "lastTrace=trace" in ui
assert "private void copyTechnicalLogs()" in ui
assert "private void confirmCopyAllPasses()" in ui
assert "private void choosePassToCopy" in ui
assert "snapshot()" in trace
assert "metricsReport()" in trace
assert "passReport(int index)" in trace
technical=trace.split('synchronized String metricsReport(){')[1].split('private static String render')[0]
assert "p.text.length()" in technical
assert ".append(p.text)" not in technical,"technical logs must not contain document text"
assert "p.status.equals(\"executed\")" in trace
assert "p.status.equals(\"interrupted\")" in trace
assert "MobileMoE" in ui and "not_run" in ui
print("VMR-013: direct copy buttons, per-pass and full logs, privacy guardrails PASS")
