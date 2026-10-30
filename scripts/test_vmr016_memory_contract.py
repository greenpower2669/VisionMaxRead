#!/usr/bin/env python3
"""VMR-016 structural regression: shared native weights, short FR/EN segments,
per-segment fresh KV and unload; no claim of Q4-native residency yet."""
from pathlib import Path
root=Path(__file__).resolve().parent.parent
rust=(root/"native/moe/src/lib.rs").read_text()
native=(root/"app/src/main/java/fr/visionmaxread/HobbyMoeNative.java").read_text()
review=(root/"app/src/main/java/fr/visionmaxread/HobbyMoeReview.java").read_text()
ui=(root/"app/src/main/java/fr/visionmaxread/MainActivity.java").read_text()
dialog=(root/"app/src/main/java/fr/visionmaxread/MoeStreamDialog.java").read_text()
assert "static SESSION:OnceLock<Mutex<Option<Loaded>>>" in rust
assert "Java_fr_visionmaxread_HobbyMoeNative_nativeOpen" in rust
assert "Java_fr_visionmaxread_HobbyMoeNative_nativeGenerateLoaded" in rust
assert "Java_fr_visionmaxread_HobbyMoeNative_nativeClose" in rust
assert "*lock=None;" in rust
assert "Engine::load(Path::new(&path),true)" in rust  # Q8 remains
assert "loaded.engine.generate(&prompt,&[],&opts" in rust
assert "loadMs=nativeOpen" in native
assert "pssAfterLoad=" in native
assert "try(HobbyMoeNative.Session session=new HobbyMoeNative.Session(context))" in review
assert "indexes.size()<3" in review and "source.length()>150" in review
assert "session.generate(buildPrompt(source,language),budget" in review
assert "trace.add(context,\"P4_MOE_SESSION\"" in review
assert '"P4_MOE_SEG_"+(n+1)' in review
assert "Conserve exactement les nombres" in review
assert "Keep numbers, names and dates unchanged." in review
assert "language.equals(\"fr\")" in review
assert 'setSingleChoiceItems(new String[]{"Français · contexte OCR","Français · sans contexte"' in ui
assert "void segment(int index,int total,String language)" in dialog
assert 'streamText.append(delta)' in dialog
assert "stream.token(delta,count)" in ui
assert 'approved?()->showComparisonText(' in ui
assert 'Copier logs techniques' in ui
print("VMR016 session reuse / segment streaming / language and privacy structural tests PASS")
