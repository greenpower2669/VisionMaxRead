# Exécution — VisionMaxRead

- [x] Dépôt et référence initiale vérifiés ; mémoires créées ; mission séparée.
- [x] Modèles OCR, Supertonic et NER épinglés ; intégrité et licences.
- [x] Interface caméra/photo/texte, capture nette, import orienté.
- [x] OCR local ; lecture Supertonic ; annulation ; réglages.
- [x] Masquage local et historique sans texte brut ; tests de confidentialité.
- [x] Compilation APK/AAB, inspection manifest, tests et analyse statique.
- [x] Commit/branche/PR vérifiés ; preuve de build ; APK et AAB construits, vérifiés et disponibles.
- [x] PR #1 fusionnée sur main le 11/10/2026 (merge 61831472d7b1).
- [x] Notes de préversion v0.1.0 ajoutées : docs/release-notes-v0.1.0.md.
- [ ] [AGENT/HUMAIN] Publier une GitHub Release v0.1.0 avec les APK/AAB issus d’une CI main réussie ; aucune Release GitHub publiée au 11/10/2026 08:44 Europe/Paris.
- [ ] [HUMAIN] Tester caméra, voix, écriture manuscrite et masquage en mode avion sur Samsung.
- [ ] [AGENT] Import d’un fichier vidéo : possibilité évoquée avant mission, à confirmer après première version.

Prochains gestes : vérifier le build main après fusion, publier la préversion GitHub en joignant APK debug et AAB unsigned, puis tester sur téléphone en mode avion. Ne pas annoncer une Release publiée tant que l’URL /releases/tag/v0.1.0 n’existe pas.

## VMR-002 — Essai OCR comparatif (13 octobre 2026)
- [x] Créer branche `feature/ocr-ab-benchmark-v1` sans modifier main.
- [x] Ajouter le moteur ML Kit latin **bundled**, hors ligne ; même image et temps comparatifs.
- [x] APK de test identifié séparément (`fr.visionmaxread.ocrtest`), résultats lisibles, sans stockage de la comparaison.
- [x] Protocole d'essai et sources : `docs/VMR-002-essai-ocr.md`.
- [x] [CI] Compilation APK debug + AAB unsigned, contrôles Java/lint, absence de permission INTERNET, hashes des modèles : run 38316437083 vert, SHA 53356ee7.
- [ ] [HUMAIN] Télécharger l'APK direct de la préversion puis comparer sur Samsung en mode avion trois documents non sensibles : imprimé, colonnes, manuscrit.
- [ ] [HUMAIN] Tester séparément une vraie IA multimodale dans Google AI Edge Gallery avec modèle vision compatible ; mesurer RAM et compréhension.
- [ ] [AGENT] Après mesures, choisir une architecture VLM réellement exécutable sur ce Samsung et la tester dans une **autre branche**, sans poids énormes ni publication automatique.
- [ ] [HUMAIN] Une préversion de test a été autorisée ; **aucun merge main ni release stable** sans validation des mesures.

## VMR-002 — Dernier état CI (13 octobre 2026)
- [x] Le build du commit 5a43248b réussit Gradle, lint, APK/AAB et archive ; **échec de la vérification des permissions**, donc aucun APK déclaré prêt (run 38315196736).
- [x] Contrer la permission INTERNET ajoutée potentiellement par une dépendance avec `tools:node="remove"` et diagnostic explicite dans `verify_apk.py`.
- [x] [CI] `verify_apk.py`, signature debug, absence INTERNET et artefact APK/AAB confirmés : run 38316437083.
- [x] [AGENT] APK direct publié avec accord explicite de Fab dans la préversion v0.2.0-ocr-test, sans merge main.

## Preuve CI VMR-002 — 13 octobre 2026
- [x] Build vert sur commit `53356ee746dcb856cde0e37f851d12010dde74c7`, run [38316437083](https://github.com/greenpower2669/VisionMaxRead/actions/runs/38316437083) : Java/Gradle, lint, APK/AAB, archive modèle, `verify_apk.py` (signature, ARM64, 18 hashes, CAMERA et **aucune permission INTERNET**).
- [x] Artifact `VisionMaxRead-0.2.0-OCR-TEST` présent (ID 11777960102, ZIP d'environ 1,07 Go contenant APK de test et AAB non signé), expiration 26/10/2026 ; **aucun APK direct ni GitHub Release publiés**.
- [ ] [AGENT] Conformément à FAB-APK-TEMP-001, prévoir si autorisé un canal de distribution directe de l'APK sans ZIP ; GitHub Actions empaquette son téléchargement et le dépôt Git standard ne convient pas à ce gros fichier. Ne pas créer de Release sans accord de Fab.
- [ ] [HUMAIN] Installation Samsung et tests qualitatifs en mode avion non effectués.

## VMR-003 — Préversion APK directe, accord Fab du 13/10/2026
- [x] Autorisation explicite de publier une **prerelease test uniquement**, sans merge main et sans AAB unsigned : « Feu vert ».
- [x] Run Android source `38316437083`, SHA `53356ee7`, vert complet ; artifact `11777960102` présent.
- [x] Préparer `.github/workflows/publish-ocr-test.yml` avec permissions minimales, vérification de SHA/CI, téléchargement **dans GitHub Actions**, publication de l'APK seul, et re-téléchargement SHA256.
- [x] [CI] Release `v0.2.0-ocr-test` vérifiée : seul asset APK direct, SHA256 identique, run de publication 38318000959 success.
- [ ] [HUMAIN] Installer et valider sur Samsung en mode avion. Ne pas fusionner `main` avant autorisation distincte.

## VMR-003 — Correctif de publication du 13/10/2026
- [x] Run `38317719077` : téléchargement côté runner et validation ZIP/APK réussis, SHA256 APK `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438`.
- [x] Correction du faux positif « tag existant » sur réponse API 404 JSON. 
- [x] [CI] Publication, re-téléchargement SHA256 et statut non-brouillon de la préversion : run 38318000959 success.

- [x] Second échec de publication (run `38317821483`) : le wrapper `gh api` expose son JSON 404 brut malgré l'expression jq ; remplacement par `git ls-remote` pour détecter un tag réellement existant.
- [x] Publication v0.2.0-ocr-test et hachage vérifiés ; lien direct communiqué après confirmation.

## Preuve de livraison directe — VMR-003, 13 octobre 2026
- [x] Workflow `Publish VisionMaxRead OCR TEST APK`, run `38318000959` terminé **success** ; téléchargement de l'artefact sur runner, intégrité ZIP, préversion publiée, APK re-téléchargé et SHA256 comparé.
- [x] Release GitHub : https://github.com/greenpower2669/VisionMaxRead/releases/tag/v0.2.0-ocr-test (tag `v0.2.0-ocr-test`, draft=false, prerelease=true).
- [x] Asset direct **unique** : https://github.com/greenpower2669/VisionMaxRead/releases/download/v0.2.0-ocr-test/VisionMaxRead-0.2.0-OCR-TEST.apk ; 539 252 854 octets, SHA256 `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438`.
- [x] `main` préservée au SHA `35a1eb4ce5c9a5286ebbe1be39207cf01b969051` ; branche de test séparée.
- [ ] [HUMAIN] Test sur Samsung : installation côte à côte, écran, moteur comparatif, mode avion, qualité du manuscrit et performances.


## VMR-004 — Audio Android hors ligne et Qwen compact (13/10/2026)
- [x] Créer `feature/offline-tts-qwen-prep-v1` depuis VMR-002 ; ne pas modifier `main`.
- [x] Ajouter un moteur TTS Android sélectionnable explicitement, filtré aux voix françaises **non réseau** ; conserver Supertonic par défaut.
- [x] Préserver annulation, mode avion, aucun texte sauvegardé, réglages défilants et identité APK LAB `fr.visionmaxread.lab`.
- [x] Documenter le protocole de mesures dans `docs/VMR-004-audio-hors-ligne-preparation-qwen.md`.
- [x] [CI] Gradle, lint, ZIP/APK signé, intégrité modèles et absence INTERNET : run `38365535174` success ; identité LAB prévue par `app/build.gradle`.
- [ ] [HUMAIN] Mesurer sur Samsung voix Supertonic et voix Android hors ligne : délai, intelligibilité, RAM, chauffe, annulation.
- [ ] [AGENT] Intégrer ultérieurement le petit Qwen3.5-0.8B **avec projecteur image et vrai runtime multimodal**, puis benchmark français manuscrit ; **pas encore codé**.
- [ ] [AGENT/HUMAIN] Évaluer plus tard Qwen Omni (vision/audio natif), séparément.
- [ ] Aucun merge main ni nouvelle Release sans accord distinct.

## VMR-005 — APK LAB dans temp GitHub
- [x] Run Android LAB `38365535174` terminé `success` au SHA `644d5177`, artefact `VisionMaxRead-0.3.0-LAB` présent (ID `11796812007`).
- [x] Workflow `extract-lab-apk.yml` préparé : `actions/download-artifact` décompresse dans `RUNNER_TEMP/VisionMaxRead-LAB`, vérifie fichier APK, ZIP et SHA256, puis livre APK direct en préversion expérimentale uniquement et re-télécharge pour vérifier l'intégrité.
- [x] [CI] `v0.3.0-lab-test` publiée, unique APK direct `VisionMaxRead-0.3.0-LAB.apk`, SHA256 vérifié après re-téléchargement : run `38394243158` success.
- [ ] [HUMAIN] Installer sur Samsung et tester deux TTS en mode avion, arrêter/reprendre, latence et qualité.
- [ ] Aucune fusion main ni publication stable autorisées.

### Preuve finale VMR-005
- [x] https://github.com/greenpower2669/VisionMaxRead/releases/tag/v0.3.0-lab-test — préversion non brouillon avec uniquement l'APK de LAB.
- [x] https://github.com/greenpower2669/VisionMaxRead/releases/download/v0.3.0-lab-test/VisionMaxRead-0.3.0-LAB.apk — 539 256 754 octets ; SHA-256 `dfebd355d7aba7db182a1d9ccc7d88616265f94249a562ab5ed1055144e23042`.
- [x] Run `38394243158` `success`, source build `38365535174` `success`.
- [ ] Retour d'essai Samsung pour voix française Android installée, Supertonic et mode avion.


## VMR-006 — Qwen/Piper/caméra
- [x] Créer `feature/qwen-piper-camera-v1` sans modifier `main`.
- [x] Retirer ML Kit de la branche et conserver PP-OCRv6-Small comme référence.
- [x] Coder Qwen3.5-0.8B-VL comme OCR automatique principal.
- [x] Coder la comparaison Qwen ↔ PP-OCRv6-Small sur le même bitmap avec chronos et sélection du texte.
- [x] Remplacer l'exécution Supertonic/Android TTS par Sherpa-ONNX/Piper UPMC Jessica sid 0.
- [x] Ajouter amorce audio silencieuse réelle, AudioTrack streaming, garde 250 ms et diagnostic PCM première/deuxième génération.
- [x] Corriger le calcul d'orientation Camera2 avec rotation Display + recalcul TextureView/JPEG.
- [x] Retirer les poids Supertonic du manifeste de modèles de cette branche.
- [x] Capturer et verrouiller le SHA Qwen complet : `5e6d69e55f92d21799fe0a2552da979c95a2533b64e4f0ed73fe87dd9d759ab9`.
- [x] Diagnostiquer le premier échec CI `38435343498` : incompatibilité Kotlin 2.1/2.4.
- [ ] [CI] Valider Kotlin 2.4 + compilation, lint, APK/AAB, signature, ARM64 et absence INTERNET.
- [ ] [CI] Relever taille finale APK/AAB et artifact LAB.
- [ ] [HUMAIN] Samsung : orientation aperçu/photo, Qwen imprimé/colonnes/manuscrit, comparaison Paddle, RAM/chauffe/espace, mode avion.
- [ ] [HUMAIN] Samsung : première phrase Jessica, répétition identique, textes longs, arrêt/reprise et après inactivité.
- [ ] Aucun merge `main` ni Release VMR-006 sans feu vert distinct.

- [x] [VMR-006] CI `38436112539` : Kotlin 2.4 passe ; blocage suivant identifié = doublon natif ONNX Runtime.
- [x] [VMR-006] Auditer Sherpa v1.13.8 : ORT Android ARM64 upstream = 1.28.2 ; aligner Paddle/NER/app sur 1.28.2 et retirer la copie ORT du seul AAR Sherpa authentifié.
- [x] [VMR-006] Surcharger R8 9.4.14 pour métadonnées Kotlin 2.4.
- [ ] [VMR-006/CI] Revalider merge native, D8, APK/AAB et contrôles finaux.


### VMR-006 — Correctif du 14/10/2026
- [x] Diagnostiquer run `38436956702` : ORT Android 1.28.2 absent des dépôts Maven utilisés.
- [x] Revenir à Sherpa 1.13.4 + ORT Android 1.27.0 commun, AAR Sherpa vérifié par SHA avant déduplication native.
- [x] Retirer le verrou portrait, corriger le signe rotation caméra arrière/avant et écouter les changements de rotation d'affichage.
- [x] Conserver l'AudioTrack initialisé entre Arrêter/Reprendre ; attendre la consommation PCM via playback head.
- [x] Enrichir le diagnostic première phrase (temps init/amorce, temps et début de signal des 2 PCM) et la comparaison OCR (PSS processus approximative).
- [ ] [CI] Obtenir compilation/lint/APK/AAB/verify_apk verts sur ce correctif.
- [ ] [HUMAIN] Valider sur Samsung orientation, audio première phrase/arrêt-reprise/inactivité, OCR et mesures PSS en mode avion.

- [x] [VMR-006/CI] Run `38504519405` : dépendances + 47 tests passent ; échec Kotlin limité aux deux références `head(current)` manquantes.
- [ ] [VMR-006/CI] Relancer après correction des deux références AudioTrack.


### VMR-006 — Raffinement final avant clôture
- [x] Préserver l'Activity/session caméra lors de rotation avec `configChanges=orientation|screenSize`.
- [x] Capturer les deux premières générations Piper réelles du même texte dans l'amorce et exposer leur diagnostic.
- [x] Ajouter temps init/première amorce/dernière amorce/dernière lecture et fréquence au diagnostic.
- [x] Ajouter un `build-manifest.txt` CI avec tailles et SHA-256 APK/AAB + artifact metadata compact.
- [ ] [CI] Revalider le SHA de ce raffinement final et relever les tailles exactes APK/AAB.
- [ ] [HUMAIN] Tests Samsung toujours nécessaires : orientation en cours de caméra, rendu figé identique, Qwen/Paddle, Jessica, arrêt/reprise, inactivité et mode avion.


### VMR-006 — Clôture CI du 14/10/2026
- [x] [CI] Run final `38505688283` vert sur code SHA `cf7083b5d204626617aa17bc1515c8c910f82dc6`.
- [x] [CI] 47 contrôles, lint, APK/AAB, archive, signature, ARM64, hashes modèles et absence INTERNET validés.
- [x] [CI] APK : 1 582 753 113 octets, SHA-256 `be97ff8f4b90f3f5aa6cccb9564e1d9a13f0cefab8b341a47fea05da4c3686af`.
- [x] [CI] AAB unsigned : 1 333 001 356 octets, SHA-256 `977679dce7a4726f99a5f1219db3db99438002b8d694757369d768de22b680e2`.
- [x] [CI] Artifact binaire `11867298469` + metadata `11867068707`.
- [ ] [HUMAIN] Installer l'APK sur Samsung SM-A576B et exécuter les 12 validations terrain VMR-006, dont mode avion.
- [ ] Aucun merge `main` ni Release sans feu vert explicite de Fab.


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

## VMR-007 — OCR spécialisé / audio HD (à valider)
- [x] Créer branche expérimentale `feature/vmr007-specialist-ocr-audio-hq-v1` depuis VMR-006 0.4.3, sans merge main.
- [x] Ajouter import SHA256 local PaddleOCR-VL-1.6 et comparaison même bitmap sur worker LiteRT Qwen séquentiel.
- [x] Conserver PP-OCR worker séparé et Qwen par défaut, fournir unload/reload plutôt que deux moteurs simultanés.
- [x] Ajouter choix Siwis/Jessica français hors ligne avec paquet Siwis verrouillé SHA256 dans la CI.
- [x] Incrémenter versionCode 8/versionName 0.4.4-debug, maintenir signature LAB persistante et noms APK/AAB.
- [x] Prouver CI verte pour le commit source VMR-007 `fb59d7b1` (run 38632764998), relire logs et valider APK signé sans modifier main.
- [ ] Vérifier les changements ultérieurs de documentation avant de qualifier leur HEAD comme source de l'APK.
- [ ] Confirmer sur Samsung que Qwen 0.4.3 démarre réellement et que la caméra respecte l'aperçu validé.
- [ ] Tester import vrai modèle 1,39 Go, mauvaise empreinte, peu de stockage et performance; aucun téléchargement depuis l'application.
- [ ] Comparer résultats Qwen vs PP-OCRv6 vs PaddleOCR-VL-1.6 sur les mêmes photos françaises.
- [ ] Tester audio Siwis vs Jessica (qualité subjective, chiffres, première syllabe, phonèmes, arrêt/reprise, longs textes).
- [ ] Auditer OvisOCR2 en branche séparée (GGUF + mmproj + runtime Android natif) ; option non intégrée.
- [ ] Consigner RAM/PSS/chauffe sur Samsung, puis demander feu vert distinct avant toute Release/merge.
- [x] Fournir un APK seul non zippé sur GitHub Actions v7 (run 38635132541, artefact 11927379292).

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

### À faire sur VMR-008
- [ ] Valider CI du dernier HEAD : tests, lint, APK 0.4.5 code 9, SHA256, certificat, absence INTERNET.
- [ ] Écouter sur Samsung Siwis puis Jessica en continu sur un texte de 2 pages, avec phrases longues, abréviations et paragraphes.
- [ ] Vérifier stop immédiat / reprise sans première syllabe manquante / pas de silence prolongé entre morceaux.
- [ ] Vérifier une seule ouverture caméra et aperçu/JPEG en portrait puis paysage.
- [ ] Mesurer PSS après OCR Qwen → PP-OCR et sous Android onTrimMemory.
- [ ] Consigner améliorations et arbitrer avec Fab avant Release ou merge.

## VMR-009 — Essai Indra OCR (29/10/2026)
- [x] Identifier la branche VMR-008 comme référence fonctionnelle ; préserver Qwen et PP-OCR.
- [x] Créer feature/vmr009-indra-ocr-latin-test sans fusion main.
- [x] Ajouter portage natif Android minimal Indra det/rec Latin ONNX, bouton comparatif sur image identique, métriques et échec isolé.
- [x] Préparer téléchargement build-only depuis révision Indra fixe, contrôle SHA embarqué, notices de licence et test de contenu APK.
- [ ] [CI] Confirmer compilation/Android lint/test APK signé avec 0 permission INTERNET, dépendances, assets Indra et SHA vérifiés.
- [ ] [AGENT] Épingler les empreintes upstream des poids indépendamment de leur téléchargement ; vérifier exactitude du prétraitement/CTC à partir de cas oracles.
- [ ] [HUMAIN] Installer APK LAB et comparer documents imprimés français, colonnes et manuscrit en mode avion. Vérifier latence, PSS, fiabilité caméra/audio et source Indra réelle.
- [ ] [AGENT] Suivant les mesures : ajouter orientation, ordre de lecture, LM et langues, ou éliminer le prototype si bénéfice insuffisant.
- [ ] [AGENT/HUMAIN] Publication éventuelle APK direct après CI verte et accord explicite ; aucun merge main automatique.

### VMR-009 — preuve de construction Android (29/10/2026)
- [x] Révision de Qwen figée au commit HF \`efb93fca20bc4b65df078b2b0a365d3350b3dcfb\` du 12/10 : bundle Qwen préexistant taille/SHA identiques (aucun changement de modèle), correctif du téléchargement CI.
- [x] Modèles Indra \`det.onnx\` et \`rec.onnx\` épinglés aux SHA256 officiels (licence fournie).
- [x] GitHub Actions run \`40941725735\` (code \`de2369940182019b52c8e2cdc8bc49b3b1ed4355\`) : SUCCESS. Tests core/Gradle, Android lint, APK/AAB, archive, modèle et signature vérifiés, ARM64, aucune permission INTERNET.
- [x] APK direct sans ZIP : \`VisionMaxRead-0.4.6-QWEN-SIWIS-INDRA-DEBUG.apk\`, artefact \`12927727403\`, 1 656 190 797 octets, SHA256 \`62a4e2fbd24cff3cdc6f61fc10061f847cf365cc86a3b3650b2d236880075cf2\`.
- [x] Signature LAB persistante : \`2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182\`, package \`fr.visionmaxread.labdebug\`, code 10.
- [ ] Essais sur téléphone Samsung à réaliser : fiabilité Indra et comparatif Qwen/PP-OCR, qualité et ordre de lecture, temps, PSS, consommation, température, mode avion.
- [ ] Étalonner le portage Android sur des oracles Python Indra, notamment interpolation, rotation, ordre de lecture et textes en colonnes ; ne pas présenter les performances desktop comme des mesures Android.

## VMR-010 — Architecture Indra / LiquidAI / MoE (décision de Fab, 29/10/2026)
Branche de conception : `feature/vmr010-indra-liquid-moe-review` ; document canonique : `docs/VMR-010-indra-liquid-moe-architecture.md`. Aucune fusion main.
- [x] [ARCHITECTURE] Choisir Indra OCR + OrderNet/LM officiel + LiquidAI en première reconstruction + experts MoE en seconde lecture + revalidation conditionnelle croisée, avec conservation du brut.
- [x] [DOCUMENTATION] Séparer l'ordre des colonnes de la reconstruction linguistique ; définir preuves, abstention, garde-fous chiffres/noms et tests comparatifs.
- [ ] [AGENT] Porter `order.onnx` et `lm.npz` avec sorties blocs/lignes conservées et oracles Python/Android ; tester 2–3 colonnes, titres transversaux, tableaux et pages inclinées.
- [ ] [AGENT] Retirer Qwen des assets, dépendances et chemin OCR **dans VMR-010 uniquement** ; laisser VMR-009 intact ; vérifier réduction de taille et d'empreinte mémoire.
- [ ] [AGENT] Tester LFM2.5-230M quantifié sur Android hors ligne (chargement, inférence, latence et PSS) ; correction ciblée avec provenance et possibilité de refuser.
- [ ] [AGENT] Définir/entraîner puis évaluer les experts de correction indépendamment du premier résultat Liquid ; ne pas baptiser « MoE neuronal » un simple routeur de règles.
- [ ] [AGENT] Arbitrage MoE sur original + hypothèses + preuves OCR, en passe supplémentaire **seulement** si ambigu ; calibrer sur jeu étiqueté ; aucune probabilité fictive.
- [ ] [AGENT] Tests d'ablation CER/WER/ordre/hallucinations/chiffres, mode avion, mémoire, annulation et fallback OCR ; CI Gradle/lint/ARM64/signature LAB, aucune permission INTERNET ; APK/AAB au nom distinct.
- [ ] [HUMAIN] Essais sur Samsung : pages multicolonnes, texte difficile, lecture vocale, chauffe/latence/mémoire ; valider qualité avant choix par défaut.
- [ ] [AGENT/HUMAIN] Ne publier aucune Release ni fusionner main sans accord distinct. Aucune nouvelle APK annoncée pour VMR-010 à ce stade.

### VMR-010 — suivi de l'exécution one-shot
- [x] Retirer le Qwen principal et les poids Qwen du build CI, ses classes et LiteRT-LM ; nettoyer uniquement l'ancien modèle Qwen du dossier privé après mise à niveau.
- [x] Connecter Indra réel automatiquement et le `order.onnx` officiel à 16 features ; repli XY-cut multi-colonnes si ordre échoue.
- [x] Conserver PP-OCR comme secours et la voix hors ligne, identité et signature LAB.
- [x] Journaliser les passages P1 brut, P2 OrderNet, P2B LM, P3 Liquid, P4 MobileMoE, P5 arbitrage, avec status exécuté/indisponible, temps, caractères, PSS ; copie détaillée volontaire avec mise en garde confidentialité.
- [x] Ajouter script PC opt-in d'inférence Liquid 230M GGUF + MobileMoE-S-QAT Meta, et corpus synthétique français pour réemploi possible dans Bonze sans modifier Bonze.
- [ ] **BLOQUANT** Faire fonctionner Liquid 230M GGUF QAD avec runtime natif Android (llama.cpp JNI) et tests de mémoire/CPU ; ne pas inventer les résultats en attendant.
- [ ] **BLOQUANT** Porter MobileMoE-S-QAT INT4 avec vrai runtime Android compatible avec architecture Meta ; la version Transformers/PyTorch PC n'est pas un runtime Android. Vérifier la licence FAIR-NC avant diffusion.
- [ ] Porter la correction officielle `lm.npz` via CTC beam sur Android et comparer à l'original ; l'étape P2B reste actuellement indisponible.
- [ ] Valider sur téléphone OrderNet : multi-colonnes, tableaux, titres, orientation et recadrage avec référence visuelle ; tester sortie du modèle ONNX et qualité du texte.
- [ ] GitHub Actions Gradle/lint/contrats, vérification APK signé, zéro permission INTERNET, artefact direct et taille après retrait Qwen ; tests Samsung ensuite.
- [ ] Optimiser la latence, PSS et tests d'ablation; ne pas merge main/release sans accord.

### VMR-010 — preuve de compilation du 29/10/2026
- [x] GitHub Actions `40952014104`, source `dca13014e7e1223157a4d1d26cb3d03b29494bdf`, build **success**, Gradle check, lintDebug, APK/AAB, vérification modèles et confidentialité.
- [x] APK installable `VisionMaxRead-0.5.0-INDRA-ORDER-SIWIS-LAB.apk` direct GitHub artefact `12932545953`, taille `343172170` octets, SHA256 `9b6cc5d2401bccea217851801e38f26f5cedb01613570ca76b273bb0d3a39189`.
- [x] Package LAB `fr.visionmaxread.labdebug`, code 11, certificat SHA256 `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`, ARM64, aucune permission INTERNET.
- [x] AAB unsigned séparé (`335166923` octets) uniquement technique, pas une livraison Play.
- [ ] **Épreuve Samsung encore requise** : Ordre des colonnes, résultat OCR Indra et PP-OCR, TTS, mémoire, journal par passe et export volontaire des textes. VMR-010 n'exécute PAS encore les deux modèles neuronaux Liquid et MobileMoE ni la correction statistique n-gramme.
- [ ] Ne pas fusionner `main`, ni créer de Release sans feu vert distinct.

### VMR-011 — Indra LM + ordre des colonnes, préparation
- [x] Brancher `lm.npz` officiel SHA256 6c0fb618a363a0acdd05d4f5b0edd9e8146d2d990541b87f6130bbfe17cea58d en build-only.
- [x] Portage NPY/NPZ ngramme et CTC beam Java pur, conserver texte brut, repli par ligne et stats LM ; fixture JVM.
- [x] Remplacer classement des blocs OrderNet simplifié par log-sigmoid + 4 passes de déplacement conformément au décodeur upstream.
- [x] Générer variantes OrderNet / XY-cut brut et + LM, choix manuel accessible, notes de géométrie normalisée uniquement dans rapport détaillé volontaire.
- [ ] Validation CI version LAB 0.5.1 code12, modèle hashé, APK ARM64 signé, sans INTERNET, fichier APK direct sans ZIP.
- [ ] Essai Samsung sur **la même photo de magazine** : extraire les quatre lectures, chronométrer LM, relever nombre de lignes modifiées/erreurs et ordre exact ; comparer visuellement les colonnes. Tester TTS.
- [ ] Vérifier précision réelle du beam CTC sur oracles officiels Python, limites de mémoire et stabilisation des blocs ; ne pas annoncer améliorations de qualité avant test.
- [ ] Liquid/MobileMoE neural Android à réaliser plus tard ; pas de merge main ni GitHub Release sans validation.

### VMR-011 — Livraison CI vérifiée du 29/10/2026
- [x] Run final de code `40957543423`, commit `74b98023dfb1ed69c6ba97fb72929487716786a5` : **success**, Java n-gram/CTC oracle, Gradle tests, Android lint, APK/AAB, manifest offline, SHA Indra y compris `lm.npz`, certificat LAB.
- [x] APK seul sans ZIP `VisionMaxRead-0.5.1-INDRA-LM-COLUMNS-LAB.apk`, artifact `12934910992`, `358136623` octets, SHA256 `8cc889df544afbd8cdb7b3795ff8c4df46a64f87248b9c18d12bf86a186959b4` ; version code `12` ; certificat LAB `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- [ ] Validation **sur Samsung** encore nécessaire : la CI ne démontre ni l'efficacité de la correction linguistique, ni l'ordre des colonnes sur la page de Fab. Tester les quatre lectures dans le rapport et vérifier `lmRuns/lmChanged/lmErrors`, mémoire et temps. Liquid/MobileMoE restent absents du runtime.

## VMR-012 — Liquid Android réel et audit multi-passes
- [x] Créer branche isolée `feature/vmr012-liquid-semantic-experts` depuis VMR-011, sans Qwen et sans fusion main.
- [x] Intégrer `libvmr_liquid.so` JNI C++ via source llama.cpp épinglée et build NDK ARM64, source en `native/liquid`.
- [x] Préparer fichier LiquidAI LFM2.5-230M QAD Q4_0 + SHA256 fixe, licence LFM Open 1.0 et licence MIT llama.cpp incluses dans APK, aucun téléchargement côté Android.
- [x] Connecter bouton opt-in Liquid après Indra, limiter CPU aux deux premiers segments de 650 caractères, interroger réellement le modèle et proposer une lecture sans modifier silencieusement le texte OCR.
- [x] Ajouter P4A/P4B audits de langue/faits et P5 arbitrage Liquid avec garde-fou des nombres et rapports individuels. **Pas un vrai Meta MobileMoE** : le P4 Meta reste `unavailable`.
- [ ] [CI] Confirmer le dernier commit source après corrections JNI/attributions, Gradle/lint/APK signé/ARM64 SHA et absence d'Internet. Ne pas utiliser les artefacts plus anciens.
- [ ] [SAMSUNG] Installer VMR-012 en mode avion, vérifier chargement réel modèle, latence P3/P4/P5, PSS mémoire, chauffe, arrêt, relecture Siwis ; transmettre rapport anonymisé de la lettre 1942, comparer texte et photo.
- [ ] [RECHERCHE] Étudier vrai MobileMoE-S Meta (runtime architecture custom, quantification, licence FAIR NC, qualité français, mémoire ~0.7Go), ne pas le faire passer pour un moteur fonctionnel sans preuve.
- [ ] [QUALITÉ] Faire une mesure CER/WER, rejet des faux chiffres/noms, impact des prompts, comparaison de documents et abstention. Pas de publication générale avant validation juridique des licences des poids et des voix.

### VMR-012 — Build final signé et direct confirmé (29/10/2026)
- [x] GitHub Actions **run `40971403081`**, code HEAD `3b51607dba476d508b6b29e5fcdd23f6b1f31391` : SUCCESS. Java core tests, lint, APK/AAB, contrats d'archive, signature et hash Indra/Liquid, permission Internet absente. Compilateur C++ ARM64 llama.cpp `36a0e2ffc0b352b774f8d71c43668b75b3061ef5` OK.
- [x] Fichier modèle Liquid vérifié (149081056 octets) + licence LFM et MIT embarquées ; aucune inférence sur réseau ; P3/P4A/P4B/P5 seulement sur bouton demandé.
- [x] APK direct sans ZIP `VisionMaxRead-0.6.0-LIQUID-EXPERTS-LAB.apk` artifact `12940647519` : **522168389 octets**, SHA256 `28cfe4c658a7813f802564e991b540d88da8ec26c8d3f5d6de80c251c02dab91`.
- [x] LAB package `fr.visionmaxread.labdebug`, versionCode **13**, certificat SHA256 `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`, même certificat qu'avant. AAB non signé 505040026 octets non destiné à l'installation.
- [ ] **Essai Samsung avant conclusion fonctionnelle** : chargement réel du moteur natif, génération P3, P4A/P4B, vote P5 ; PSS/chauffe/vitesse, report/copie texte, mode avion, TTS. `CI success` ne valide pas cette exécution matérielle. Meta MobileMoE absent.
- [ ] Aucune fusion `main` ni GitHub Release sans validation distincte de Fab.

## VMR-013 — livraison boutons de copie confirmée
- [x] `0.6.1-copy-logs-lab` code14 : Copier logs techniques (sans texte) / Copier tous les résultats (confirmation) / Copier une passe (sélection), logs d'erreur Liquid et métriques.
- [x] GitHub Actions `40975513380` **SUCCESS**, APK direct `12941924736`, 522171009 octets, SHA256 `d787c4891a6e46a6aee7916dac0cbb712fe7c1b37aebebcce6e0b705bafab52b`.
- [x] Modèle Meta MobileMoE-S-QAT véritable tenté via GitHub Actions `40975194290`, **non exécuté** : HF 401 gated, aucun accès aux poids ; GitHub workflow de recherche manuel uniquement (token autorisé en secret HF_TOKEN).
- [ ] Test Samsung VMR-013 : copier sans captures d'écran les métriques et une ou plusieurs passes pour diagnostiquer précisément l'insuffisance de Liquid.
## VMR-014 — expérimentation vrai HobbyLM MoE Android
- [x] Branche isolée et code natif Rust JNI du véritable sparse MoE HobbyLM-1B-Instruct, prompt sur une zone courte, validation chiffres, sortie choisie manuellement, logs P4/P5.
- [x] Script de build source upstream épinglée, modèle Q4_K_M SHA `0a1d1d4a7e545c51e8fdaa61707b5bf7d09cb4e948fc9b09c3ec2789c493cdbb` (729 Mo), licence Apache, signature LAB code15 visée.
- [ ] **BLOQUANT** CI VMR-014 compile réellement Rust aarch64, télécharge modèle public, valide APK et hash/signature, puis fournir APK direct non zip si succès. Ne pas livrer un APK de run échoué.
- [ ] **BLOQUANT** Premier essai Samsung de chargement/inférence HobbyLM (moteur Rust créé à l'origine pour 500M) : mémoire, stabilité, qualité français manuscrit, sauvegarde Indra et débit; si échec, diagnostiquer via boutons de copie VMR-013.
- [ ] Aucune fusion main ni publication de Release, éviter de déclarer le modèle Meta exécuté ; vérifier licences et utilité réelle avant Bonze.

- [x] [VMR-014/CI] Inférence smoke CPU GitHub de HobbyLM-1B Q4 **réellement exécutée** et réussie dans run `40976686174`. Build Rust ARM64 réalisé. Le même modèle n'a **pas encore** été lancé sur Samsung.
- [x] [VMR-014/UX] « Copier logs techniques » disponible avant OCR et après relance d'une application tuée, journal persistant et ligne début MoE pour détecter un kill Android ; textes OCR toujours absents des diagnostics standards.
- [ ] [VMR-014/CI] Réussir le build final du correctif logs après relance, relever SHA et lien APK direct sans ZIP, effectuer essai utilisateur.

### VMR-014 — livraison finale vérifiée le 30/10/2026
- [x] Run GitHub Actions `40977108326`, code source `d88c5002b0321d5c622c5975155fdc3bf7a3b559` **SUCCESS** : Rust HobbyLM-1B ARM64, véritable génération host CPU MoE, Indra/Liquid hashes, Gradle/lint, signature LAB, permission INTERNET absente.
- [x] APK unique direct sans ZIP/AAB `VisionMaxRead-0.6.2-REAL-MOE-LAB.apk`, artifact `12942629751` (https://github.com/greenpower2669/VisionMaxRead/actions/runs/40977108326/artifacts/12942629751) : **1 252 630 529 octets**, SHA256 `4cb627a8c7065f3f45e643d196faf4a5b4091b2566ac471e824e25bc8f2e31e9`. Package `fr.visionmaxread.labdebug`, versionCode 15, certificat `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- [ ] **Test utilisateur Samsung obligatoire** : faire tourner réellement HobbyLM en mode avion, mesurer mémoire/PSS, temps, qualité, no hallucination, voir P4/P5 et copier les logs en texte. Host smoke et CI ne sont pas test mobile. Gros APK et 729 Mo de poids privés copiés à la première exécution, prévoir espace disque ; annulation/remplacement document. 
- [ ] Préserver aucun merge main et aucune Release publique avant accord.

### VMR-014 — preuve définitive du 30/10/2026
- [x] **Run GitHub Actions `40977108326` vert**, source SHA `d88c5002b0321d5c622c5975155fdc3bf7a3b559` : vrai test d'inférence HobbyLM-1B sur CPU GitHub, compilation native Rust ARM64, tests/lint Gradle, APK signé, SHA modèles, licence et zéro permission INTERNET.
- [x] APK direct (sans ZIP) `VisionMaxRead-0.6.2-REAL-MOE-LAB.apk`, artifact `12942629751`, **1252630529 octets**, SHA256 `4cb627a8c7065f3f45e643d196faf4a5b4091b2566ac471e824e25bc8f2e31e9`, code15, certificat LAB `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`. Modèle HobbyLM quantifié 729156672 octets.
- [ ] **Essai Samsung requis** : vraie inférence ARM64, délai/chauffe/mémoire/espace nécessaire, qualité de correction manuscrit français, logs copiables même après crash ; éviter d'annoncer validé sur téléphone avant preuve.

## VMR-015 — HobbyLM chat template + jetons diffusés en direct
- [x] Corriger prompt HobbyLM pour `<|user|>...<|assistant|>` conforme au tokenizer du modèle, sans hypothèse Liquid et sur 260 caractères ; préserver chiffres/noms.
- [x] Coder retour de vrais jetons Rust via JNI et interface Java, événement par jeton, UTF-8 et journal de nombre de tokens sans contenu sensible.
- [x] Fenêtre UI à contraste élevé affichant le flux généré en direct, compteur jetons/temps, bouton Arrêter, Copier flux volontaire, fermeture sur changement de document ; pas de faux canal de raisonnement.
- [x] Gérer annulation avant début d'inférence, afficher sortie interrompue en `OcrTrace`, préserver l'original et bloquer « Utiliser ce texte » sur les propositions rejetées.
- [x] Ajouter `scripts/test_vmr015_stream_contract.py`, actualiser contrats de confidentialité VMR-010/013 et test CPU HobbyLM avec vrais marqueurs.
- [ ] **BLOQUANT** Attendre CI Rust/Java+lint/APK complète et vérification directe SHA/signature/code16 avant partage de la 0.6.3 ; ne pas considérer un build intermédiaire comme livraison.
- [ ] **BLOQUANT** Réessayer sur le Samsung la même lettre et noter premier jeton, vitesse, interruption, copies des passes, qualité et conservation des chiffres. Comparer à VMR-014, ne pas déduire un gain sans mesurer.
- [ ] Ne pas merge main ni publier Release sans validation.

### VMR-015 — livraison signée contrôlée le 30/10/2026
- [x] GitHub Actions **run `41038502413` SUCCESS**, source code HEAD `2cd829b0445ab922037d91dc716f5146295392a8`. Test CPU réel HobbyLM sur prompt au format corrigé, Rust ARM64 JNI per-token, contrats de streaming, confidentialité, Gradle, lint, signature et vérification modèles/offline : tous verts.
- [x] APK direct sans ZIP `VisionMaxRead-0.6.3-MOE-LIVE-LAB.apk`, artifact `12969740814`, **1 252 670 765 octets**, SHA256 `800f3460b53de7c62957718fcbef5921f308fee4bdb96ab672458b6c2d6851ca`, LAB versionCode 16, certificat SHA256 `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`. Pas de permission INTERNET ; Indra+Liquid+HobbyLM présents. `main` et Release non modifiés.
- [ ] **À valider sur Samsung** : diffusion réelle de chaque jeton sur la fenêtre, stop pendant copie du modèle/génération, bouton Copier flux, qualité comparée sur la même lettre 1942, chiffres, PSS, temps premier jeton. La réussite CI ne prouve pas la précision de HobbyLM en français.

## VMR-016 — session MoE FR/EN, premier jalon
- [x] Pont de passation à Fab avant coding, analyse source HobbyLM Q4→Q8 et besoins.
- [x] Branche `feature/vmr016-moe-memory-fr-en` depuis VMR-015, sans `main`.
- [x] JNI Rust: conserver Engine HobbyLM-1B Q8 durant lot, fonctions nativeOpen/GenerateLoaded/Close, stop et flux réels ; fermer à la fin (pas de cache KV partagé).
- [x] Interface FR/EN, trois lignes au plus (≤150 caractères), transcription individuelle et garde-fou sur chiffres, logs session/ligne et streaming non écrasé.
- [x] Tests de régression structurels et source/docs, version 0.6.4 LAB code17.
- [ ] Vérifier CI Rust/Java/lint/APK signé avec son SHA et permission hors ligne ; livrer APK direct uniquement si SUCCESS.
- [ ] Mesurer sur Samsung à partir de la même lettre et de lignes anglaises connues : load_ms, PSS après chargement et au pic, 3 latences individuelles, premier jeton, mémoire après fermeture, altération de chiffres et exactitude.
- [ ] Deuxième expérimentation : Q4 natif mmap avec patch officiel llama.cpp BailingMoe2 QKV, ARM64, contrôler mémoire du modèle et page faults, comparer au Q8 ; en cas de compatibilité prouvée, examiner cache d'experts borné sans modifier routage.
- [ ] Ne pas confondre réduction des rechargements avec réduction du pic Q8 ; aucune fusion main/Release sans accord.

### VMR-016 — Preuve de livraison directe signée, 30/10/2026
- [x] Run final `41058583152`, source `7f7a53f27b6107d911fbd1d42ecbf2075693e33c` : **SUCCESS**. VMR-015 et VMR-016 contrats, vrai smoke CPU HobbyLM, build Rust JNI ARM64, Java/Gradle/lint, sécurité des modèles, signature, offline tous validés.
- [x] APK `VisionMaxRead-0.6.4-MOE-SESSION-FR-EN-LAB.apk`, artefact direct sans ZIP `12976964615` : **1 252 679 509 octets**, SHA256 `5fadf95c071f7048096de11cf76ecfed063a791fb69c7971f157021502cfc1d6`, code17, certificat LAB `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- [ ] **Essai réel Samsung** indispensable : succès des trois sous-passes, latence de chargement unique, comparaison PSS RAM, temps/token, qualité français/anglais, arrêt et sortie partielle, validité des chiffres. Ne pas confondre build vert avec gain mémoire ou qualité linguistique.
- [ ] Phase suivante **Q4 natif**: tester patch officiel HobbyLM dans llama.cpp sous Android ARM64 (mmap/quantisation sans Q8 global), comparer pics RSS/PSS, qualité; cache d'experts selon mesures.
