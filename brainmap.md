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


## VMR-006 — signature LAB persistante / installation Samsung (2026-10-14)
- Cause du message Android « Application non installée » : anciennes LAB et nouvelle LAB partageaient `fr.visionmaxread.lab` mais chaque runner GitHub générait sa propre clé debug.
- Correction validée sur `feature/vmr006-stable-lab-signing`.
- Nouvelle identité debug isolée : `fr.visionmaxread.labdebug`.
- `versionCode=5`, `versionName=0.4.1-debug`, label `VisionMaxRead LAB DEBUG`.
- Signature LAB persistante dédiée aux préversions/debug uniquement ; elle ne doit JAMAIS signer l'AAB release/Play.
- Empreinte certificat LAB SHA-256 de référence : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- La CI reconstruit la clé LAB test, vérifie son empreinte, puis vérifie après build package/version/certificat de l'APK.
- Code validé : `587cba2fcbb2157f364e48fcbc94a9be760832c7`.
- GitHub Actions : `38576497507` — SUCCESS ; 47 contrôles, Gradle/lint/APK/AAB, archive et verify_apk verts.
- APK : `VisionMaxRead-0.4.1-QWEN-PIPER-DEBUG.apk`, `1582759585` octets, SHA-256 `6edef1b2d4a62d415823023ee56b865fba6623eecc4fced633546d3d88c5053e`.
- AAB unsigned : `1333003863` octets, SHA-256 `afb71708069d31ca3c4962a7d3765827f09deb70c2e18c4cdbd7a8eef7cbe631`.
- Artefact principal : `11900497303`; métadonnées : `11900677088`.
- L'ancienne `fr.visionmaxread.lab` peut rester installée : la nouvelle LAB DEBUG s'installe à côté.
- Aucun merge de `main` et aucune nouvelle Release directe créée sans nouveau feu vert explicite.


### Pré-Release directe v0.4.1 DEBUG publiée
- Feu vert explicite de Fab reçu le 2026-10-14.
- Tag : `v0.4.1-qwen-piper-debug-test`.
- Source validée : `587cba2fcbb2157f364e48fcbc94a9be760832c7`.
- Build source : `38576497507` — SUCCESS.
- Workflow de publication : `38578433124` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK exact vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.1-QWEN-PIPER-DEBUG.apk`.
- Taille : `1582759585` octets.
- SHA-256 : `6edef1b2d4a62d415823023ee56b865fba6623eecc4fced633546d3d88c5053e`.
- Package : `fr.visionmaxread.labdebug` ; versionCode `5`.
- Certificat LAB SHA-256 : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- `main` inchangée ; AAB unsigned non publié.


## VMR-006 — Qwen cache LiteRT corrigé (2026-10-14)
- Retour Samsung SM-A576B Android 16 : modèle Qwen copié correctement (1 303 966 384 octets), puis échec moteur LiteRT-LM :
  `INVALID_ARGUMENT: Cache directory does not exist or is not writable: /data/user/0/fr.visionmaxread.labdebug/cache/qwen_litert_cache`.
- Cause confirmée : `QwenOcrService` passait le chemin `cacheDir` à LiteRT-LM sans créer le sous-répertoire.
- Correction : création explicite du dossier `qwen_litert_cache`, vérification `isDirectory/canWrite`, trace debug `QWEN cache ready`.
- Robustesse : si création/initialisation du moteur échoue, le moteur partiellement créé est fermé proprement et l'exception est journalisée.
- Version de test : `versionCode=6`, `versionName=0.4.2-debug`, package `fr.visionmaxread.labdebug`, même certificat LAB persistant.
- Code validé : `208032d92e682d3b9e43b621627bd7efc579cf78`.
- GitHub Actions : `38583098184` — SUCCESS.
- `PASS 47 contract checks`, Gradle/lint/APK/AAB, archive et `verify_apk.py` verts.
- APK : `VisionMaxRead-0.4.2-QWEN-PIPER-DEBUG.apk`, 1 582 759 977 octets, SHA-256 `74f6d2c71189d71214a750837034ed18c5ea7e1a081bcb3979ddf7e59f68d2a6`.
- Certificat LAB SHA-256 inchangé : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- Artefact principal : `11904645649`; métadonnées : `11903244873`.
- Valeurs caméra Samsung relevées pour la suite : back camera id=0, sensor=90°, display=0°, preview=640x480, still=4000x3000, TextureView=1012x759, transform actuel=90°, JPEG=90°.
- Correction du redressement d'aperçu caméra non incluse dans ce push ; conserver ces valeurs pour l'audit/correctif dédié.
- Aucun merge de `main` ; aucune nouvelle Release directe créée sans feu vert explicite.


### Pré-Release directe v0.4.2 DEBUG publiée
- Feu vert explicite de Fab reçu le 2026-10-14.
- Tag : `v0.4.2-qwen-piper-debug-test`.
- Source validée : `208032d92e682d3b9e43b621627bd7efc579cf78`.
- Build source : `38583098184` — SUCCESS.
- Workflow de publication : `38586381436` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK exact vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.2-QWEN-PIPER-DEBUG.apk`.
- Taille : `1582759977` octets.
- SHA-256 : `74f6d2c71189d71214a750837034ed18c5ea7e1a081bcb3979ddf7e59f68d2a6`.
- Package : `fr.visionmaxread.labdebug` ; versionCode `6`.
- Certificat LAB SHA-256 : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- Correctif principal : création/vérification explicite du cache LiteRT `qwen_litert_cache` avant initialisation du moteur.
- `main` inchangée ; AAB unsigned non publié.


## VMR-006 — Qwen cache racine + aperçu caméra Samsung (2026-10-14)
- Retour réel Samsung SM-A576B Android 16 :
  - portrait : sensor=90°, display=0°, preview 640x480, TextureView 1012x759 ;
  - paysage : display=90°, TextureView 2272x759 ;
  - ancien transform aperçu calculait 0° en paysage et l'image restait visuellement tournée ; JPEG figé restait correctement orienté.
  - Qwen continuait à lever `INVALID_ARGUMENT: Cache directory does not exist or is not writable: .../cache/qwen_litert_cache`.
- Audit de la source 0.4.2 : le correctif sous-dossier était bien présent dans le code, mais le log réel ne montrait pas la ligne `QWEN cache ready`, donc impossibilité de prouver que le binaire réellement lancé était la source attendue.
- 0.4.3-debug ajoute package/version/versionCode à la première ligne du journal pour lever toute ambiguïté de binaire.
- Qwen : abandon du sous-dossier dédié ; LiteRT-LM reçoit directement `context.cacheDir.absolutePath`, chemin Android existant/writable recommandé par l'API.
- Caméra : séparation stricte aperçu/JPEG. Le JPEG conserve la logique précédente déjà validée. La matrice TextureView suit le schéma Camera2 : portrait=0°, ROTATION_90=-90°, ROTATION_270=+90°, ROTATION_180=180°, avec scaling/cadrage indépendant de `SENSOR_ORIENTATION`.
- Branche : `feature/vmr006-qwen-camera-fix-043`.
- Version : package `fr.visionmaxread.labdebug`, versionCode `7`, versionName `0.4.3-debug`, certificat LAB persistant inchangé.
- Code validé : `79e905ab69723716eef22a14738f4df7d3e91dd1`.
- GitHub Actions : `38592842511` — SUCCESS.
- `PASS 47 contract checks`, Gradle/lint/APK/AAB, archive et `verify_apk.py` verts.
- APK : `VisionMaxRead-0.4.3-QWEN-PIPER-DEBUG.apk`, 1 582 760 261 octets, SHA-256 `7e285969c3b0fd33c2e1bb0f2acb21d92af8154727278b1c4b85c017733186f2`.
- Certificat LAB SHA-256 : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- Artefact principal : `11908775821`; métadonnées : `11908402484`.
- Validation fonctionnelle Samsung requise : la première ligne du prochain log doit annoncer `version=0.4.3-debug code=7`; Qwen doit ensuite montrer `QWEN cache ready path=/data/user/0/fr.visionmaxread.labdebug/cache ...`.
- Aucun merge de `main`; aucune Release directe 0.4.3 sans feu vert explicite.


### Pré-Release directe v0.4.3 DEBUG publiée
- Demande explicite de Fab : nouvel APK/push.
- Tag : `v0.4.3-qwen-piper-debug-test`.
- Source validée : `79e905ab69723716eef22a14738f4df7d3e91dd1`.
- Build source : `38592842511` — SUCCESS.
- Workflow de publication : `38601765196` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.3-QWEN-PIPER-DEBUG.apk`.
- Taille : `1582760261` octets.
- SHA-256 : `7e285969c3b0fd33c2e1bb0f2acb21d92af8154727278b1c4b85c017733186f2`.
- Package : `fr.visionmaxread.labdebug` ; versionCode `7`.
- Certificat LAB SHA-256 inchangé : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- `main` inchangée ; AAB unsigned non publié.

## VMR-007 — index actif (14/10/2026)
- Branche `feature/vmr007-specialist-ocr-audio-hq-v1`, référence documentaire `docs/VMR-007-ocr-specialise-voix-hd.md`.
- `SpecializedOcrService.kt` : import local SHA256 de PaddleOCR-VL-1.6, session `OCR:`, engine temporaire ; `QwenOcrService.unload()` garantit une seule grosse instance LiteRT sur `qwenWorker`.
- `PiperSpeechEngine.kt` : sélection entre Siwis et Jessica, moteur TTS unique, AudioTrack persistant. `MainActivity.java` : boutons OCR spécialisé, import SAF, réglage voix.
- `.github/workflows/android.yml` : modèle Siwis officiel SHA256 verrouillé, LAB versionCode 8. Aucune preuve Samsung ou CI verte ne doit être inventée.

### VMR-007 — preuve CI complète (14/10/2026)
- HEAD compilé et validé : `fb59d7b1a16438f1f6d264657fc865044b687fc8`.
- GitHub Actions : https://github.com/greenpower2669/VisionMaxRead/actions/runs/38632764998 — SUCCESS.
- 47 contrôles contractuels PASS ; Gradle/lint/APK/AAB, archive, verify_apk verts.
- APK test : `VisionMaxRead-0.4.4-QWEN-SIWIS-DEBUG.apk`, 1 651 093 817 octets, SHA-256 `644e77ff4a162bb980793c55191904772f7333ef7349104846f53ae5ce422dce`.
- Package `fr.visionmaxread.labdebug`, versionName `0.4.4-debug`, versionCode `8`, ARM64, aucune permission INTERNET.
- Certificat LAB SHA256 inchangé : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- AAB unsigned non publié : 1 400 474 764 octets, SHA-256 `d14062ef0ef64952588d8608f1dd7d677b92b0e771f738258369a39bbd0ac1c5`.
- Artefact GitHub Actions binaire : `11926398102` ; métadonnées : `11926628152`.
- Modèle additionnel PaddleOCR-VL-1.6 non intégré à l'APK : import manuellement via SAF + SHA256, format .litertlm (1 390 305 472 octets, `314df8b34d6911990d488e825602550a5bbcc5d6700cb1a8914e853e9043f4f1`).
- CI ≠ validation Samsung : test matériel Qwen/cache/caméra, import OCR, comparatif précision, Siwis/Jessica, RAM/température toujours nécessaires.
- Ne pas merger `main` ni créer de Release sans autorisation expresse.

### VMR-007 — livraison APK direct, sans ZIP (14/10/2026)
- Changement GitHub Actions : `actions/upload-artifact@v7`, `archive: false` pour l'APK unique et pour l'AAB unsigned dans deux artefacts distincts ; upload de métadonnées séparé.
- CI source du packaging : `7ff23a04dc69e6327bac611485bdb2d511417fa4`, run https://github.com/greenpower2669/VisionMaxRead/actions/runs/38635132541 — SUCCESS. Mêmes sources Android version 0.4.4-debug code 8, même clé LAB, mêmes tests verts.
- **APK Android seul (non zippé, 1 651 093 817 octets)** : https://github.com/greenpower2669/VisionMaxRead/actions/runs/38635132541/artifacts/11927379292 ; artefact nommé `VisionMaxRead-0.4.4-QWEN-SIWIS-DEBUG.apk`.
- AAB unsigned non installable (distinct) : artefact `11927274270`. Métadonnées : `11926934378`.
- Cette voie de livraison GitHub n'est ni une Release ni un merge `main`.
- La branche ajoute une correction de notices : `THIRD_PARTY_NOTICES.md` mentionne désormais Siwis CC-BY 4.0 et la version Sherpa-ONNX réelle 1.13.4.
- Reste à valider matériellement sur Samsung la qualité vocale et la lecture PaddleOCR-VL importé ; la CI ne démontre pas les performances réelles.


## VMR-008 — Qwen inchangé, audio naturel, caméra et mémoire (15/10/2026)
Instruction de Fab : « Ok on garde qwen et on optimisé le reste, l audio est pas mal juste mal découpé à mon gout ».
- Branche dédiée \`feature/vmr008-qwen-audio-flow-camera-memory\`, dérivée de VMR-007, \`main\` intact.
- **Qwen reste OCR principal** : même Qwen3.5-0.8B-VL, modèle LiteRT, backend et prompt inchangés. PP-OCRv6-Small conserve son worker indépendant ; spécialiste PaddleOCR-VL toujours facultatif/import séparé.
- Audio Siwis/Jessica : ancien \`TextChunks.split(text,240)\` coupait au dernier espace et le prochain segment n'était synthétisé qu'après la lecture du précédent. Nouveau \`TextChunks.speech(text,340)\` privilégiant les fins de phrase (abbreviations françaises protégées), ponctuation puis espaces; retours simples OCR recollés, paragraphes distincts (180 ms). Nouveau \`VoicePCM\` avec anticipation bornée d'un segment (une seule synthèse native à la fois), AudioTrack persistant, garde initiale 250 ms seulement, arrêt sans verrou audio long.
- Caméra : \`CameraController.start\` ignore un deuxième appel identique pour le même document (évite l'ouverture en double observée le 15 octobre 00:14:41). Capture JPEG et matrices d'orientation non modifiées.
- Mémoire : \`MainActivity.onTrimMemory\` transfère la libération des moteurs LiteRT sur \`qwenWorker\`, après les inférences éventuellement en cours, sans fermer Qwen à chaque lecture OCR/PP-OCR. Le moteur se recharge au prochain besoin après alerte mémoire.
- LAB \`fr.visionmaxread.labdebug\` v0.4.5-debug, versionCode 9, signature LAB persistante inchangée. Pas de permission INTERNET. APK/AAB livrés séparément et directement, sans ZIP.
- Tests unitaires du cœur étendus : paragraphes, retours de ligne OCR, ponctuation, abréviations, Unicode/emoji, bornes max en points de code; ne pas prétendre une CI verte sans preuve du run HEAD.
- Vérifications matérielles nécessaires : 1 seul CAM start/open par session, bonne orientation réelle, 2+ phrases audio à débit continu, arrêt immédiat, changement Siwis/Jessica, Qwen 1ère et 2e image, PP-OCR, PSS/pression mémoire, mode avion.
- **Interdictions** : pas de merge sur \`main\`, pas de Release ni publication Play sans nouvel accord explicite de Fab. La compilation CI n'est pas une validation Samsung.

## VMR-009 — Indra OCR expérimental
- app/src/main/java/fr/visionmaxread/IndraOcrService.java : portage Android du détecteur ONNX et du reconnaisseur CTC latin, modèle et vocab dans assets/indra ; aucune persistance du texte.
- scripts/prepare_indra_models.py : acquiert les modèles au build uniquement depuis une révision HF fixe et génère le manifeste avec SHA ; scripts/verify_apk.py valide les assets du binaire.
- MainActivity : bouton Indra OCR, worker distinct, même bitmap, durée et PSS processus, dialogue texte sélectionnable, sans modifier Qwen/Paddle/synthèse.
- docs/VMR-009-indra-ocr-latin.md : périmètre, écarts au pipeline officiel et protocole de test. Test CI et Samsung à confirmer.

## VMR-010 — Parcours nouveau lecteur et analyses par passes
- `app/src/main/java/fr/visionmaxread/MainActivity.java` : automatique Indra → ordre de lecture → dossier de passes ; comparateur PP-OCR et lecture vocale conservés ; bouton « Passes Indra · rapport ».
- `IndraOcrService.java` + `IndraReadingOrder.java` : det/rec puis OrderNet officiel, fallback XY-cut si modèle incompatible ; `order.onnx` verrouillé et téléchargé uniquement en CI.
- `OcrTrace.java` : journal métrique par étape, texte brut seulement sur copie confirmée ; Liquid/MobileMoE explicitement « unavailable » dans LAB actuelle.
- `LegacyModelCleanup.java` : suppression ciblée des anciens poids Qwen du cache applicatif ; garder historique utilisateur.
- `scripts/mobilemoe_benchmark.py` : banc desktop opt-in LiquidAI GGUF et Meta MobileMoE-S-QAT, données synthétiques dans `tests/vmr010-synthetic.jsonl` ; `scripts/test_vmr010_contract.py` : garde-fous build.
- `docs/VMR-010-indra-liquid-moe-architecture.md` + `todo.md` : choix architecture, limitations et prochaines intégrations ; pas d'APK neuronal Android annoncé.

## VMR-011 — Index des améliorations suite aux journaux de Fab
- `app/src/main/java/fr/visionmaxread/IndraNgramLM.java` : parseur pur Java de `lm.npz` et backoff original Indra.
- `app/src/main/java/fr/visionmaxread/IndraCtcBeam.java` : décodage CTC avec 8 candidats, seulement sur lignes incertaines.
- `app/src/main/java/fr/visionmaxread/IndraReadingOrder.java` : `rank` fidèle au decode_order officiel, regroupement des blocs, comparateur XY-cut.
- `app/src/main/java/fr/visionmaxread/IndraOcrService.java` : P1 brut, P2 OrderNet, P2A XY-cut, P2B LM, P2C XY-cut LM, géométrie normalisée par ligne.
- `MainActivity.java`/`OcrTrace.java` : choix de lecture et rapport avec textes uniquement sur copie explicite, debug limité aux métriques.
- `scripts/test_indra_lm_java.py` / `tests/java/fr/visionmaxread/IndraLmOracle.java` : contrat Java de backoff et CTC ; documentation `docs/VMR-011-layout-language-model.md`.

## VMR-012 — carte du runtime sémantique natif
- `native/liquid/vmr_liquid.cpp` et `CMakeLists.txt` : JNI vers vrai llama.cpp LFM2.5 GGUF, CPU deux threads, taille de contexte bornée, génération, stop/unload et UTF-8 en octets.
- `scripts/build_liquid_android.sh` : compile sous NDK Android 27.2 depuis commit llama.cpp SHA fixe et copie `libvmr_liquid.so` ARM64 + licence MIT.
- `scripts/prepare_liquid.py` : télécharge au build uniquement GGUF QAD Q4_0 avec empreinte fixe, licence officielle du modèle ; pas de permission Internet.
- `app/src/main/java/fr/visionmaxread/LiquidNative.java` : vérification locale SHA, copie vers dossier privé, JNI/fermeture.
- `LiquidOcrReview.java` : P3 proposition, P4A linguistique, P4B faits, P5 vote A/B ; taille segment bornée et contrôle chiffres. Ceci est multi-experts **par prompts**, pas Meta MobileMoE.
- `MainActivity.java` : bouton d'essai optionnel, rapport détaillé et choix utilisateur, base Indra inchangée en cas d'échec. `scripts/verify_apk.py` impose modèle+licences+JNI.

## VMR-013/014 — composants et chemins
- `OcrTrace.java`: snapshot, metricsReport, passReport, fullReport ; `MainActivity.java` boutons Copier logs techniques, Copier résultats toutes les passes, Copier une passe ; traces Liquid et erreurs sans écran.
- `scripts/test_vmr013_copy_contract.py` : test présence export et absence de texte privé dans diagnostics ; VMR-013 APK 0.6.1 code14 CI success.
- `scripts/mobilemoe_cpu_probe.py` + `.github/workflows/mobilemoe-cpu-probe.yml` : tentative vrai Meta QAT (seulement sur PC) **bloquée 401 gated** ; accès HF autorisé et `HF_TOKEN` uniquement via GitHub Secrets si acceptation de licence, jamais dans les logs. Ne pas confondre avec HobbyLM.
- `native/moe/src/lib.rs`, `Cargo.toml`: véritable MoE HobbyLM via Rust Engine::load/GenOpts, JNI annulation, ARM64. `scripts/build_hobby_moe_android.sh`: clone du SHA upstream, compilation cargo aarch64 Android.
- `scripts/prepare_hobby_moe.py`: GGUF HobbyLM-1B-Instruct Q4_K_M SHA officiel + LICENSE/NOTICE, assets embarqués. `HobbyMoeNative.java`: copie privée vérifiée et JNI. `HobbyMoeReview.java`: 460 caractères Indra LM, Liquid optionnel, P4 MoE et P5 chiffres, résultat à choisir explicitement.
- `docs/VMR-014-hobbylm-real-moe.md` : conditions, licence, limites, protocole Samsung. VMR-014 version 0.6.2 code15 prévue, ne pas annoncer CI verte avant résultat.

## VMR-015 — lecture en temps réel des jetons HobbyLM
- `native/moe/src/lib.rs` : `engine.generate` passe chaque delta UTF-8 à `JNIEnv.call_method(listener,"onToken",...)` ; cancellation native conservée via reset explicite.
- `HobbyMoeNative.java` : interface `TokenListener`, `infer(...listener)`, annulation avant/durant première copie GGUF.
- `HobbyMoeReview.java` : `buildPrompt` officiel `<|user|>...<|assistant|>`, sans Liquid, sortie 260 caractères, limite 110 tokens, P4 trace tokens et `interrupted`.
- `MoeStreamDialog.java` : affichage progressif auto-scroll, temps et nombre de jetons, Arrêter, Copier flux sensible après accord, Fermer.
- `MainActivity.java` : bouton MoE ouvre immédiatement fenêtre, envoie callback, sauvegarde diagnostic, n'autorise comparaison/usage que si garde-fou validé, ferme/arrête à la prochaine image.
- `scripts/test_vmr015_stream_contract.py`, CI Rust/Java, `docs/VMR-015-moe-live-token-stream.md`. LAB code16, branche VMR-015. Ancienne VMR-014 reste sans modification.

## VMR-016 — index de la session MoE réutilisée
- `native/moe/src/lib.rs`: ajout session globale Rust `OnceLock<Mutex<Option<Loaded>>>` avec `nativeOpen`, `nativeGenerateLoaded`, `nativeWeightBytes`, `nativeClose` ; chargement poids une fois par lot, contexte neuf à chaque phrase et libération à la fin.
- `HobbyMoeNative.java`: `Session AutoCloseable`, SHA modèle une seule fois à l'ouverture, métriques load_ms/PS S et poids Q8.
- `HobbyMoeReview.java`: sélection fr/en, trois lignes max ≤150 chars, prompts distincts, arbitrage strict chiffres, traces P4 par ligne + P5.
- `MoeStreamDialog.java`/`MainActivity.java`: marqueurs de ligne dans le flux, sélecteur Français/English et boutons stop/copie conservés.
- `scripts/test_vmr016_memory_contract.py`, `docs/VMR-016-moe-memory-fr-en.md`. VMR-016 LAB 0.6.4/code17 visée ; CI et test Samsung nécessaires.

## VMR-017 — index léger de reprise
- Source de référence VMR-016 `feature/vmr016-moe-memory-fr-en`; travail `feature/vmr017-adaptive-document-context` ; VMR-016 Q4 divergente **documentation seulement** selon comparaison de fichiers.
- `AdaptiveTokenBudget.java`, `OcrProposalGuard.java` : estimation prudentielle jetons et rejets hors sujet ; `DocumentContextMemory.java` : état document volatile avec observations/hypothèses/confirmations distinctes.
- `HobbyMoeReview.java`/`HobbyMoeNative.java`/`native/moe/src/lib.rs` : sortie jusqu'à 512 jetons avec motif de fin honnêtement inconnu et A/B précédent OCR ; `MoeStreamDialog.java`/`MainActivity.java` : budget affiché et modes FR/EN × mémoire.
- Tests `scripts/test_vmr017_contract.py` ; audit, limites et ordre de mission dans `docs/VMR-017-adaptive-model-audit.md` et `todo.md`. Ne pas annoncer Liquid/Qwen intégrés ; pas de merge/Release.

### Index VMR-017 — modèles textuels facultatifs
- `OfflineCorrectionModels.java` : deux références GGUF SHA256 fixes, import SAF sans réseau, copie locale avec contrôle taille/digest et revalidation au chargement ; OCR seul par défaut.
- `LiquidNative.java` + `native/liquid/vmr_liquid.cpp` : charge les poids authentifiés dans un runtime llama.cpp commun, cause EOS/limite observable, streaming JNI cumulatif en UTF-8, un seul modèle séquentiellement.
- `DocumentSentenceChunks.java` : découpage conservateur à intervalles lossless ; `TextualDocumentReview.java` : comparaison bilingue ≤40 phrases/lot, contexte précédent OCR étiqueté incertain, filtre non-hallucination ; `TextModelStreamDialog.java` : flux, arrêt, copie sensible volontaire.
- `MainActivity.java` : réglage « OCR seul/Liquid 1.2/Qwen3 1.7 », import GGUF et bouton de comparaison après Indra. Pas de preuve de qualité/compatibilité Samsung ni d'optimisation mémoire : voir `todo.md`.
