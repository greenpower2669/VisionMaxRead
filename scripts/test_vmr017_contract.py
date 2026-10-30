#!/usr/bin/env python3
"""VMR-017: compile and execute the pure Java guard/budget; check wiring."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parent.parent
base=root/"app/src/main/java/fr/visionmaxread"
with tempfile.TemporaryDirectory() as tmp:
    harness=Path(tmp)/"GuardSmoke.java"
    harness.write_text("""package fr.visionmaxread;
final class GuardSmoke {
 public static void main(String[] args) {
   if(AdaptiveTokenBudget.forOcrCorrection("Une phrase française sans chiffres.",512)<=72)throw new AssertionError("72 token cap");
   if(AdaptiveTokenBudget.forOcrCorrection("abc",128)>128)throw new AssertionError("safety cap");
   if(!AdaptiveTokenBudget.outputMayBeCapped(128,128))throw new AssertionError("end unknown");
   if(OcrProposalGuard.verify("Ma lettre de Paris","Cite this article as").accepted)throw new AssertionError("citation hallucinated");
   if(OcrProposalGuard.verify("Madame la Presidente","This article has no relevance").accepted)throw new AssertionError("off-topic");
   if(OcrProposalGuard.verify("Paris 1942, reference 123","Paris 1943, reference 123").accepted)throw new AssertionError("numbers");
   if(OcrProposalGuard.verify("Bonjour Paris, je pars demain.","Bonjour Lyon, je pars demain.").accepted)throw new AssertionError("proper name");
   if(!OcrProposalGuard.verify("Ths is a simpie document.","This is a simple document.").accepted)throw new AssertionError("conservative OCR correction");
   System.out.println("VMR017 regression PASS");
 }
}
""")
    subprocess.run(["javac","-encoding","UTF-8","-d",tmp,str(base/"AdaptiveTokenBudget.java"),
                    str(base/"OcrProposalGuard.java"),str(harness)],check=True)
    subprocess.run(["java","-cp",tmp,"fr.visionmaxread.GuardSmoke"],check=True)
review=(base/"HobbyMoeReview.java").read_text()
native=(base/"HobbyMoeNative.java").read_text()
rust=(root/"native/moe/src/lib.rs").read_text()
dialog=(base/"MoeStreamDialog.java").read_text()
assert "72,(delta,count)" not in review
assert "AdaptiveTokenBudget.forOcrCorrection" in review
assert "OcrProposalGuard.verify" in review
assert "first_token_ms=" in review and "finish_reason=" in review
assert "maxTokens>512" in native and "limit>512" in rust
assert "/ 72" not in dialog
print("VMR017 structural checks PASS")
