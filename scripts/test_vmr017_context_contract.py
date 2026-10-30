#!/usr/bin/env python3
"""VMR017: run the exact shipped prompt construction, not a simulated flag."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parent.parent
app=root/"app/src/main/java/fr/visionmaxread"
with tempfile.TemporaryDirectory() as d:
    harness=Path(d)/"ContextProbe.java"
    harness.write_text('''package fr.visionmaxread;
final class ContextProbe {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args){
  String first="Paris, 22 Janvier 1942.";
  String second="Croix rouge Frangaise.";
  String third="Mon mari est ancien combattant.";
  DocumentContextMemory m=new DocumentContextMemory();
  String a=MoePromptBuilder.build(first,"fr",m.previousSummary(),m.openingExcerpt());
  check(!a.contains("Phrase OCR précédente"),"first sentence must have no prior context");
  m.observeOriginal(first);
  String b=MoePromptBuilder.build(second,"fr",m.previousSummary(),m.openingExcerpt());
  check(b.contains(first),"previous OCR missing from actual prompt");
  check(b.indexOf(first)<b.indexOf(second),"context must precede current OCR");
  check(b.endsWith(second+"\\n<|assistant|>\\n"),"chat marker must follow current OCR");
  check(!b.contains("\\\\n<|assistant|>"),"literal slash-n is not a delimiter");
  m.observeOriginal(second);
  String c=MoePromptBuilder.build(third,"fr",m.previousSummary(),m.openingExcerpt());
  check(c.contains(first)&&c.contains(second),"opening and preceding context needed");
  check(c.indexOf(second)<c.indexOf(third),"third sentence must follow context");
  String baseline=MoePromptBuilder.build(third,"fr","","");
  check(!baseline.contains(first)&&!baseline.contains(second),"A/B context leaked");
  check(MoePromptBuilder.build("An English line.","en","Previous text.").contains("Previous OCR phrase"),"English mode");
  m.reset();
  check(m.previousSummary().isEmpty()&&m.openingExcerpt().isEmpty(),"document reset leaked OCR");
  System.out.println("VMR017 context prompt regression PASS");
 }
}''',encoding="utf-8")
    subprocess.run(["javac","-encoding","UTF-8","-d",d,
        str(app/"MoePromptBuilder.java"),str(app/"DocumentContextMemory.java"),str(harness)],check=True)
    subprocess.run(["java","-cp",d,"fr.visionmaxread.ContextProbe"],check=True)
review=(app/"HobbyMoeReview.java").read_text()
assert 'buildPrompt(source,language,previous,opening)' in review
assert 'memory.observeOriginal(source)' in review
assert 'ctx_sent="+contextAttached' in review
print("VMR017 context wiring regression PASS")
