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
