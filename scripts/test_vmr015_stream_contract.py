#!/usr/bin/env python3
"""VMR015 contracts: actual per-token JNI, published chat markers and safe UI."""
from pathlib import Path
app=Path("app/src/main/java/fr/visionmaxread")
rust=Path("native/moe/src/lib.rs").read_text()
native=(app/"HobbyMoeNative.java").read_text()
review=(app/"HobbyMoeReview.java").read_text()
ui=(app/"MainActivity.java").read_text()
dialog=(app/"MoeStreamDialog.java").read_text()
trace=(app/"OcrTrace.java").read_text()
assert 'call_method(listener,"onToken"' in rust, "JNI must emit every token"
assert '"(Ljava/lang/String;I)V"' in rust, "Callback descriptor mismatch"
assert 'out.push_str(delta)' in rust
assert "engine.generate(&prompt,&[],&opts,|delta|" in rust
assert "STOP.load(Ordering::Acquire)" in rust
assert "Java_fr_visionmaxread_HobbyMoeNative_reset" in rust
assert "Java_fr_visionmaxread_HobbyMoeNative_nativeStop" in rust
assert 'void onToken(String delta,int tokenCount)' in native
assert 'infer(String model,String prompt,int maxTokens,TokenListener listener)' in native
assert "new String(tokens,StandardCharsets.UTF_8)" in native
assert "if(cancelling.get())throw new IllegalStateException" in native
assert '<|user|>' in review and '<|assistant|>' in review, "Instruct role markers mandatory"
assert "USER: Tu es" not in review and "ASSISTANT:" not in review
assert "indexes.size()<3" in review and "source.length()>150" in review
assert 'session.generate(buildPrompt(source,language),budget,(delta,count)->' in review
assert "partial.append(delta)" in review
assert '" generated_tokens="+tokenCount[0]' in review
assert 'stream.finish("Terminé' in ui
assert "stream.token(delta,count)" in ui
assert "activeMoeStream.cancelAndClose()" in ui
assert "new MoeStreamDialog(this)" in ui
assert 'approved?()->showComparisonText(' in ui, "Rejected proposals must not show Use text"
assert "showComparisonText(\"HobbyLM MoE — proposition\"" not in ui
assert 'streamText.append(delta)' in dialog
assert "void segment(int index,int total,String language)" in dialog
assert 'scroller.fullScroll(View.FOCUS_DOWN)' in dialog
assert 'setNeutralButton("Arrêter"' in dialog
assert 'setPositiveButton("Copier flux"' in dialog
assert 'getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener' in dialog
assert 'setTextIsSelectable(true)' in dialog
assert 'confirmCopy()' in dialog
assert 'if(p.status.equals("executed")||p.status.equals("interrupted"))' in trace
print("VMR015: real token callback + cancellation + 1B instruct chat template + privacy guard PASS")
