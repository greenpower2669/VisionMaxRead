# Carte technique — VisionMaxRead

- `app/` : application Android, Java, interface native accessible.
  - `MainActivity` : navigation caméra/photo/texte, commandes, statut, historique et réglages.
  - `CameraController` : Camera2, aperçu léger, capture JPEG distincte et orientation.
  - `ImageLoader` : import borné, orientation EXIF, mémoire.
  - `OcrService` : adaptation du SDK PaddleOCR, ordre de lecture, résultat texte.
  - `MlKitOcrService` : OCR latin ML Kit modèle bundled ; uniquement dans VMR-002 sur branche d'essai.
  - `SupertonicEngine` : quatre réseaux ONNX, styles, génération/audio, interruption.
  - `LocalAnonymizer` : NER ONNX multilingue + règles ; ne persiste rien.
  - `HistoryStore` : stockage privé des seuls textes masqués, suppression.
  - `core/` : règles et segmentation testables sans Android ; offsets et protection des callbacks périmés.
- `ppocr-sdk/` : source Android officielle PaddleOCR adaptée ; ONNX Runtime et OpenCV ; licences/source identifiées.
- `scripts/` : préparation et validation des modèles, tests de contrats, construction reproductible.
- `.github/workflows/` : vérification et APK/AAB. Sur la seule branche expérimentale VMR-003 : workflow isolé `publish-ocr-test.yml` produisant une GitHub **prerelease** après validation du run préexistant ; aucune release du stable ni fusion automatique.
- `docs/superpowers/` : conception, plan et journal d’exécution.
- `app/src/main/assets/` : modèles générés localement/CI, exclus de Git ; manifest d’intégrité.

Le thread UI ne réalise aucune inférence. Les commandes annulent la génération précédente. Le texte brut ne traverse pas la frontière de persistance. Les erreurs de modèle sont visibles et empêchent le faux succès.

Validation : docs/build-evidence.json. ReaderSessions sépare source/document/historique ; Cancellation propage la perte de focus audio. APK/AAB dans app/build/deliverables (ignorés Git). Sources sur branche/PR, modèles régénérés depuis models.lock.json.

ModelArchive + scripts/model_pack.py : emballage VMR1/XZ du grand modèle vocal, restitution exacte avant ONNX. scripts/test-archive.sh contrôle décodeur Java et erreurs de troncature ; verify_apk.py exige liste exacte des assets et hashes des octets originaux.

## Branche d'essai VMR-002
`feature/ocr-ab-benchmark-v1` : `MainActivity.compareOcr()` compare le même bitmap, chronomètre les 2 moteurs et propose lecture/choix du texte, sans stockage nouveau ; voir `docs/VMR-002-essai-ocr.md`. Application debug distincte `fr.visionmaxread.ocrtest`. Aucun VLM intégré sur cette branche.

## VMR-002 — Garanties de manifeste de l'essai
Les dépendances Android de ML Kit peuvent contribuer des déclarations de permissions pendant la fusion du manifeste. `app/src/main/AndroidManifest.xml` retire explicitement `android.permission.INTERNET` via `tools:node="remove"`. `scripts/verify_apk.py` inspecte le manifeste de l'APK effectivement compilé ; la CI échoue si INTERNET apparaît ou si CAMERA est absente. Une compilation réussie avant ce contrôle ne suffit pas à qualifier l'APK comme livrable.

## VMR-003 — Circuit d'APK direct autorisé
`release du test` : push du seul workflow `.github/workflows/publish-ocr-test.yml` sur `feature/ocr-ab-benchmark-v1` → vérifier run `38316437083` (conclusion et HEAD source) → `actions/download-artifact@v4` **sur runner GitHub** → sélectionner `dist/VisionMaxRead-0.2.0-OCR-TEST.apk` (jamais AAB) → GitHub Release `v0.2.0-ocr-test` taguée sur le commit **ayant construit** l'APK → re-téléchargement Release sur runner et validation SHA256. Les permissions `contents:write` sont limitées au workflow de publication dédié. Code Android, `main` et signature du stable sont inchangés.

### Preuve de livraison VMR-003
Run de publication `38318000959` success ; Release https://github.com/greenpower2669/VisionMaxRead/releases/tag/v0.2.0-ocr-test non brouillon / préversion, asset direct `VisionMaxRead-0.2.0-OCR-TEST.apk` 539 252 854 octets, SHA-256 `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438`. Les fichiers de mémoire/documentation de preuve sont actualisés sur branche expérimentale après le tag de build figé `53356ee7`, sans modifier ce dernier. La voie GitHub/CI a décompressé l'archive 1,07 Go sans faire passer le ZIP sur le Samsung.


## VMR-004 — Laboratoire audio et préparation Qwen
`feature/offline-tts-qwen-prep-v1` dérive du test OCR sans changer `main` : `OfflineAndroidTts.java` (voix française déclarée hors ligne), sélecteur dans `MainActivity`, variante `fr.visionmaxread.lab`, protocole `docs/VMR-004-audio-hors-ligne-preparation-qwen.md`. Qwen Vision et Qwen Omni **non intégrés**.
