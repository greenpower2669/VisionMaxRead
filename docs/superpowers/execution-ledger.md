# Journal d’exécution

- BASE : 3b0b10a9dc16ef0d2e8ff3e1f9bf4b8e1e34d05e, dépôt initial LICENSE uniquement.
- Décision : l’ordre explicite de coder autorise l’implémentation de la première version décrite ; pas de nouvelle demande de permission pour les choix réversibles.
- Décision : garder le texte original en mémoire pour lecture fidèle, et ne persister que sa version masquée ; interprétation annoncée à Fab.
- Décision : caméra native Camera2, capture JPEG séparée de l’aperçu basse résolution.
- En cours : outillage et références locales ; aucun test téléphone effectué.

Task 1: core tests RED (entity not masked), then GREEN: 34 checks. Gradle/Java/SDK installed in scratch; apt unavailable, standalone JDK used. Shared interfaces: SDK consumes asset paths, engines consume immutable model manifest; UI uses generation tickets to drop stale results.

Task 1: tokenizer RED (no tokens), GREEN 38 checks. Model lock completed: 18 assets, 609,645,027 bytes, pinned HF revisions and SHA256. Official model weights explain the real distribution size; OCR-only size does not include speech/NER/runtime.
Task 2: Camera2 separates ~640×480 preview from <=12MP JPEG; EXIF import decode bound 2560; OCR SDK releases sessions after each operation. Image and original text remain in memory.
Task 3: Java Android Supertonic port (8 steps, seed 2026, F1/M1), quantized multilingual BERT NER with 254-token windows/30-token overlap, French regex complement, masked-only AtomicFile history. Tests on device still pending.
Build diagnosis: Java tools ignore the environment HTTP proxy; Python downloads succeed. SDK binaries obtained from the official package index; retry Gradle with explicit JVM proxy properties.

Ruling: test APK signed with Android debug key, release AAB unsigned until an upload key is supplied — allows immediate installation without creating/publishing a production identity — cost if wrong: reinstall needed when changing signing key. CI only builds artifacts, never releases or merges.

Model smoke tests PASS on desktop CPU: exact synthetic French printed line (accents included); detector activation; French PER/LOC classification; Supertonic French waveform 2.19s RMS 0.0436. This does not validate Android audio/camera nor cursive handwriting.
CI run 1 failed before compiling: setup-android v3 requests removed package `tools`. Use the runner’s existing sdkmanager and request only pinned platform/build-tools. Local AGP defaults build-tools 35 even with compileSdk 36: explicitly pin 36.0.0 in both Android modules.
Camera lifecycle adjusted: document ticket accompanies capture to ignore old frames after retake; camera can restart after permission dialog/resume. API 28 heading guarded for Android 26.

Final review: fresh context gpt-6-astra. Two Important findings, no Critical. Final fixed audio-focus operation cancellation — cancellation test RED→GREEN. Final fixed history/source ticket interference — ReaderSessions clear/edit/capture tests RED→GREEN. Suite 47 checks. UI cancellation now returns to Lire; independent document/source/history scopes preserve current text and camera while cancelling old history writes.
Final: minor (deferred): individual history entry deletion (whole history deletion works).
Final: minor (deferred): natural-landscape tablets/display rotation support; first version targets portrait phones.
Final: Ruling: camera HAL, handwriting/French voice quality, memory/performance on Samsung and empirical NER recall require device trials — do not claim them from desktop tests — cost if wrong: phone-specific corrections after first trial.

Tasks 1–3: complete, core 47 checks and desktop real-model smoke green. Task 4 build verification complete: final Gradle build exit 0, 47 checks, lint 0 errors/37 warnings, signed APK contains all immutable model hashes, no INTERNET, ARM64 only, ZIP and ELF 16 KiB verified; AAB unsigned. Full fingerprints in docs/build-evidence.json. User-device trials remain human work. Final branch/file delivery in progress.
