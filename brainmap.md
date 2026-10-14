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

## VMR-005 — Temporaire GitHub pour APK LAB
Workflow `.github/workflows/extract-lab-apk.yml` : vérifier run source `38365535174` et SHA `644d5177` → dézip automatique `actions/download-artifact@v4` dans `${RUNNER_TEMP}/VisionMaxRead-LAB` → sélectionner APK signé `VisionMaxRead-0.3.0-LAB.apk` (ignorer AAB unsigned) → contrôler ZIP/SHA256 → publier une préversion `v0.3.0-lab-test` avec APK seul → re-télécharger et comparer SHA256. Le dossier temp du runner sera nettoyé par GitHub après exécution ; seuls les assets de Release sont permanents. Ne jamais committer le binaire dans Git.

### Livraison effectivement vérifiée VMR-005
Le workflow `38394243158` est vert : téléchargement depuis Actions vers le dossier éphémère `RUNNER_TEMP/VisionMaxRead-LAB`, contrôle ZIP de l'APK, préversion `v0.3.0-lab-test` créée, comparaison du SHA256 après re-téléchargement réussie. URL https://github.com/greenpower2669/VisionMaxRead/releases/download/v0.3.0-lab-test/VisionMaxRead-0.3.0-LAB.apk ; 539 256 754 octets ; SHA256 `dfebd355d7aba7db182a1d9ccc7d88616265f94249a562ab5ed1055144e23042`. Le `main` initial (`35a1eb4c`) reste inchangé.


## VMR-006 — carte active de la branche Qwen/Piper
- `QwenOcrService.kt` : OCR principal Qwen3.5-0.8B-VL via LiteRT-LM ; moteur conservé en mémoire, conversation neuve par document.
- `OcrService.kt` : PP-OCRv6-Small conservé comme moteur de référence/bouton de comparaison sur le même bitmap.
- `SpeechEngine.kt` + `PiperSpeechEngine.kt` : Sherpa-ONNX/Piper UPMC, Jessica sid 0 ; moteur persistant.
- `PersistentVoicePcmPlayer.kt` : AudioTrack streaming, amorce silencieuse réelle, attente par playback head et garde 250 ms.
- `CameraController.java` : orientation relative capteur/Display, recalcul de transformation, JPEG cohérent.
- ML Kit, Supertonic et le prototype Android TTS ne font plus partie de l'exécution VMR-006.
- La CI prépare les gros binaires/modèles sans les committer : Sherpa AAR, UPMC Piper et Qwen LiteRT-LM ; les SHA sont contrôlés.
- Identité debug conservée : `fr.visionmaxread.lab`. Aucun merge `main`, aucune Release VMR-006 autorisée à ce stade.


## VMR-006 — Orientation, audio persistant et compatibilité ORT (14/10/2026)
- `CameraController` : rotation relative = capteur - affichage pour la caméra arrière, capteur + affichage pour l'avant puis miroir ; `OrientationEventListener` force un nouveau transform si la rotation d'affichage change.
- `AndroidManifest.xml` : aucun verrou `screenOrientation=portrait` sur la LAB VMR-006.
- `PersistentVoicePcmPlayer` : `stop()` fait pause+flush mais conserve l'AudioTrack initialisé ; `release()` seul libère la route. L'attente de fin se base sur la progression réelle du playback head relative aux frames écrites.
- Sherpa-ONNX 1.13.4 et Paddle partagent ORT Android 1.27.0 ; la copie native ORT de l'AAR Sherpa est retirée après vérification SHA.
- `MainActivity.compareOcr()` affiche temps et PSS processus approximative après Qwen / après Paddle ; ce n'est pas une attribution mémoire isolée à chaque moteur.


### VMR-006 — Preuve instrumentée finale
- `MainActivity` conserve sa session lors de `orientation|screenSize`; la rotation est gérée par `CameraController` plutôt que par une recréation de l'écran.
- `PiperSpeechEngine.warmUp()` capture la vraie paire génération 1 / génération 2 de `WARM_TEXT` au premier démarrage, conserve hashes/onsets/temps/fréquence, puis amorce silencieusement l'AudioTrack avec la seconde génération.
- `.github/workflows/android.yml` produit `build-manifest.txt` avec RUN_ID, HEAD_SHA, noms, tailles exactes et SHA-256 APK/AAB, et l'upload aussi dans un petit artifact metadata séparé.


### VMR-006 — Ancrage de preuve final
La référence binaire canonique de la LAB VMR-006 est le run `38505688283` sur le code SHA `cf7083b5d204626617aa17bc1515c8c910f82dc6`. Le petit artifact metadata `11867068707` contient `build-manifest.txt` ; le gros artifact binaire `11867298469` contient APK, AAB, lint et manifeste. Toute validation terrain doit rester distincte de cette preuve de build.


## VMR-006 — pré-Release test directe publiée (2026-10-14)
- Feu vert explicite de Fab reçu pour la pré-Release de test VMR-006.
- Publication effectuée sans merge de `main`.
- Tag : `v0.4.0-qwen-piper-lab-test`.
- Source validée : `cf7083b5d204626617aa17bc1515c8c910f82dc6`.
- Build source vert : GitHub Actions `38505688283`.
- Workflow de publication : `38537482245` — succès.
- Artefact source extrait dans `RUNNER_TEMP` côté GitHub Actions.
- APK publié seul : `VisionMaxRead-0.4.0-QWEN-PIPER-LAB.apk`.
- Taille : `1582753113` octets.
- SHA-256 vérifié avant et après publication : `be97ff8f4b90f3f5aa6cccb9564e1d9a13f0cefab8b341a47fea05da4c3686af`.
- AAB unsigned non publié.
- Validation réelle Samsung encore à effectuer.


## VMR-006 — instrumentation OCR/caméra (2026-10-14)
- Branche : `feature/vmr006-ocr-camera-debug` ; `main` inchangée.
- Retour Samsung : Qwen/PP-OCR perçus comme inactifs ; aperçu caméra encore mal orienté alors que l'Activity tourne.
- PP-OCRv6-Small est désormais réellement indépendant du worker Qwen et reste utilisable même si Qwen bloque ou échoue.
- Bouton PP-OCR grisé visiblement lorsqu'il est indisponible/en cours.
- Barre de progression OCR ajoutée : progression réelle pendant la copie locale du modèle Qwen ; mode indéterminé pendant les phases sans télémétrie fiable (initialisation/inférence), pas de faux pourcentage.
- Journal debug privé ajouté : dimensions image, étapes OCR, temps, PSS processus, erreurs, paramètres caméra (sensor/display/rotation/buffer), sans texte OCR brut ni image.
- Le journal est consultable/copier/effaçable depuis Réglages > Journal debug.
- Orientation caméra : instrumentation seulement dans ce push ; aucune nouvelle formule de transform appliquée avant retour des valeurs réelles Samsung.
- Validation CI Android attendue avant APK de test.


### Validation CI instrumentation OCR/caméra
- Code validé : `331b0207009477857dd9c2bfa42034558bad2937`.
- GitHub Actions : `38558165918` — SUCCESS.
- `PASS 47 contract checks`, Gradle/lint/APK/AAB verts, contrat d’archive vert.
- `verify_apk.py` : APK signé, ARM64, hashes modèles conformes, aucune permission INTERNET.
- APK : `1582759577` octets ; SHA-256 `f2d1a7604900413507f524f27bbaccdada7c398ef28eb3d2feab0ac2dde135ba`.
- AAB unsigned : `1333003862` octets ; SHA-256 `eb570dd4bc5bf64de9e057681aa19ad17b4cc5cda8b160e47be76f5e3274d70f`.
- Artefact principal : `11892045941` ; métadonnées : `11891441218`.
- Validation fonctionnelle Samsung encore requise pour observer le journal caméra/OCR et corriger ensuite la formule d’orientation avec les valeurs réelles.


### Pré-Release directe VMR-006 DEBUG publiée
- Feu vert explicite de Fab reçu le 2026-10-14.
- Tag : `v0.4.0-qwen-piper-debug-test`.
- Source : `331b0207009477857dd9c2bfa42034558bad2937`.
- Build source : `38558165918` — SUCCESS.
- Workflow publication : `38570794104` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK exact vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.0-QWEN-PIPER-LAB.apk`.
- Taille : `1582759577` octets.
- SHA-256 : `f2d1a7604900413507f524f27bbaccdada7c398ef28eb3d2feab0ac2dde135ba`.
- `main` inchangée ; AAB unsigned non publié.
