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
   if(OcrProposalGuard.verify("La lettre de Paris est intacte.","<think>La lettre de Paris est intacte.</think>").accepted)throw new AssertionError("Qwen thinking leak");
   if(OcrProposalGuard.verify("Madame la Presidente","This article has no relevance").accepted)throw new AssertionError("off-topic");
   if(OcrProposalGuard.verify("Paris 1942, reference 123","Paris 1943, reference 123").accepted)throw new AssertionError("numbers");
   if(OcrProposalGuard.verify("Bonjour Paris, je pars demain.","Bonjour Lyon, je pars demain.").accepted)throw new AssertionError("proper name");
   if(!OcrProposalGuard.verify("Ths is a simpie document.","This is a simple document.").accepted)throw new AssertionError("conservative OCR correction");
   DocumentContextMemory m=new DocumentContextMemory();
   m.observeOriginal("Paris, 22 janvier 1942. Document privé.");
   if(m.previousSummary().length()>150||m.confirmedOverview().length()!=0)throw new AssertionError("unverified not fact");
   m.proposeObservation("date","22 janvier 1942");
   if(m.observations().get("date").kind!=DocumentContextMemory.EvidenceKind.MODEL_HYPOTHESIS)
       throw new AssertionError("hypothesis must remain hypothesis");
   m.reset();
   if(!m.previousSummary().isEmpty()||m.position()!=0)throw new AssertionError("reset");
   String doc="Mme. Dupont habite Paris. 22 janvier 1942. Le Dr. Robert écrit.";
   StringBuilder back=new StringBuilder();
   for(DocumentSentenceChunks.Segment seg:DocumentSentenceChunks.split(doc))back.append(seg.raw);
   if(!doc.equals(back.toString()))throw new AssertionError("sentence lossless");
   if(DocumentSentenceChunks.split("Le 22.01.1942 est une date. Une autre phrase.").size()!=2)
       throw new AssertionError("numeric date split");
   OcrProposalGuard.Verdict french=OcrProposalGuard.verify(
       "Croix rouge Prangaise", "Croix rouge Française");
   if(french.accepted||!french.reviewRecommended)
       throw new AssertionError("OCR French adjective must be reviewable, not auto-accepted");
   if(OcrProposalGuard.verify("Croix rouge Prangaise","Croix rouge UNICEF").reviewRecommended)
       throw new AssertionError("invented proper name not reviewable");
   StringBuilder sample=new StringBuilder();
   for(int i=0;i<9;i++)sample.append("Ligne numero ").append(i).append(" en français.\\n");
   java.util.List<MoeSegmentPlanner.Segment> segs=MoeSegmentPlanner.plan(sample.toString(),12);
   if(segs.size()!=9)throw new AssertionError("Must process more than three lines");
   StringBuilder recreated=new StringBuilder();int cursor=0;
   for(MoeSegmentPlanner.Segment seg:segs){
       if(seg.end-seg.start>150||seg.start<cursor)
           throw new AssertionError("Invalid segment offsets or overlap");
       recreated.append(sample,cursor,seg.start).append(seg.source(sample.toString()));
       cursor=seg.end;
   }
   recreated.append(sample,cursor,sample.length());
   if(!sample.toString().equals(recreated.toString()))
       throw new AssertionError("Source lost during segmented rendering");
   if(MoeSegmentPlanner.plan(sample.toString(),3).size()!=3)
       throw new AssertionError("Maximum must be configurable");
   System.out.println("VMR017 regression PASS");
 }
}
""")
    subprocess.run(["javac","-encoding","UTF-8","-d",tmp,str(base/"AdaptiveTokenBudget.java"),
                    str(base/"OcrProposalGuard.java"),str(base/"DocumentContextMemory.java"),str(base/"DocumentSentenceChunks.java"),str(base/"MoeSegmentPlanner.java"),str(harness)],check=True)
    subprocess.run(["java","-cp",tmp,"fr.visionmaxread.GuardSmoke"],check=True)
review=(base/"HobbyMoeReview.java").read_text()
native=(base/"HobbyMoeNative.java").read_text()
rust=(root/"native/moe/src/lib.rs").read_text()
dialog=(base/"MoeStreamDialog.java").read_text()
assert "72,(delta,count)" not in review
assert "AdaptiveTokenBudget.forOcrCorrection" in review
assert "OcrProposalGuard.verify" in review
assert "first_token_ms=" in review and " ctx_sent=" in review and " finish=" in review
assert "maxTokens>512" in native and "limit>512" in rust
assert "/ 72" not in dialog
print("VMR017 structural checks PASS")

model=(base/"OfflineCorrectionModels.java").read_text()
liq=(base/"LiquidNative.java").read_text()
cpp=(root/"native/liquid/vmr_liquid.cpp").read_text()
review_text=(base/"TextualDocumentReview.java").read_text()
dialog_text=(base/"TextModelStreamDialog.java").read_text()
screen=(base/"MainActivity.java").read_text()
assert "bb741ebb106d543e9de114b843a3d3d73d51c74b5801e69da2abde821a0cb3e1" in model
assert "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5" in model
assert "MessageDigest.getInstance" in model and "importFromDocument" in model
assert "inferStreaming" in liq and "Java_fr_visionmaxread_LiquidNative_inferStreaming" in cpp
assert "output.append(buf,n);g_lastTokens++" in cpp
assert "CallVoidMethod(listener,method" in cpp
assert '"eos"' in cpp and '"token_limit"' in cpp
assert "progress.onToken(" in review_text and "first_token_ms=" in review_text
assert "void token(int index,int total,String cumulative,int count)" in dialog_text
assert "new TextModelStreamDialog" in screen and "OCR uniquement" in screen
assert "OfflineCorrectionModels.OCR_ONLY" in screen
print("VMR017 streamed offline Q4 model contract PASS")

planner=(base/"MoeSegmentPlanner.java").read_text()
assert "DEFAULT_SEGMENTS=12" in planner and "HARD_SEGMENTS=40" in planner
assert "MoeSegmentPlanner.plan(original,requestedSegments)" in review
assert "candidates.append(original,cursor,segment.start)" in review
assert "reviewOnlySegments" in review
print("VMR017 bounded OCR segments and manual review contract PASS")

review=(base/"HobbyMoeReview.java").read_text()
screen=(base/"MainActivity.java").read_text()
assert "final List<SegmentProposal> comparisons" in review
assert "comparisons.add(new SegmentProposal(" in review
assert "for(HobbyMoeReview.SegmentProposal item:result.comparisons)" in screen
print("VMR017 exact original/proposal pairing checks PASS")
