# 🌱 VISIONMAXREAD — PASSATION VMR-020 → VMR-021

Dépôt : https://github.com/greenpower2669/VisionMaxRead
Branche de travail à lire en premier : `feature/vmr020-auto-provision-ocr` (vérifier son dernier HEAD).
Ancêtre fonctionnel de référence : `feature/vmr019-native-ocr-surya-lighton`, commit `ec174988`, CI Android verte run `41117112603`, APK LAB 0.6.9 / code 22. **CI verte ≠ OCR Surya/LightOn prouvé sur Samsung.**
Cible VMR-020 en branche : LAB 0.7.0 / code 23. Vérifier le dernier HEAD et la CI avant d'annoncer APK.

## Demande de Fab
Surya OCR 2, LightOnOCR-3 0.8B et Nemotron 3.5 Parse **sans téléchargements ni imports manuels**. Les modèles restent sur le stockage privé du téléphone ; charger un seul gros moteur en RAM à la fois. L'utilisateur veut un parcours fluide, accessible, quasi automatique : ouvrir/figer une photo, choisir l'OCR, l'application prépare ce qu'il faut. Éviter de gonfler l'APK au point de ne plus pouvoir l'installer.

## Réalisé VMR-019 / VMR-020 (code, pas garantie terrain)
- Moteur ARM64 local `llama-mtmd-cli` épinglé au SHA `792fcf4e7501972b595d3aeeb85153f9926f6aef`. Surya + LightOn image avec poids GGUF et projecteur ; import manuel historique VMR-019 **remplacé** en VMR-020.
- `SpecialistOcrAutoInstaller.java` : modèles publics HF/CDN HTTPS, Wi-Fi non facturé, consentement, vérification SHA-256, Range/reprise, limites de taille, stockage privé, contrôle espace libre + réserve 300 Mio. Aucun fichier imposé à l'utilisateur.
- `MainActivity.java` : « Préparer mes OCR automatiquement », installation au premier OCR si absent, progression, annulation, effacement volontaire par moteur ; Indra/PP-OCR restent utilisables ; aucune substitution silencieuse.
- `AndroidManifest.xml` : `INTERNET` et `ACCESS_NETWORK_STATE` **uniquement pour acquisition des poids** ; `usesCleartextTraffic=false`. Aucune image, OCR brut ou historique n'est transmis au réseau.
- `SpecialistOcrModels.java` : SHA exact des 4 fichiers de Surya/LightOn et URL source ; `SpecialistOcrProcess.java` : inférence CPU locale, images/sorties temporaires, timeout, suppression du cache. **Aucun modèle lourd ajouté à l'APK**.
- Mémoires `brainmap.md`, `brain.md`, `todo.md`, `debughistorical.md`, `ordres-de-mission.md` raccourcies ; originaux conservés mot pour mot sous `docs/memory-archives/2026-10-30-pre-vmr020/`.

## Éléments NON faits / ne pas présenter comme livrés
- Exécution et qualité OCR Surya/LightOn réellement mesurées sur Samsung ; vérifier le modèle + mmproj et les formats de sorties avant toute affirmation de réussite.
- Téléchargement continu pendant mise en veille et après destruction de l'Activity : le code actuel est lié à un Executor de l'Activity ; **à remplacer ou renforcer** par un service foreground/WorkManager avec notification, reprise, contrôle énergie et annulation.
- Nemotron 3.5 Parse **n'est pas compatible avec l'ancien runtime mtmd GGUF** ; architecture NVIDIA C-RADIOv4 + NaViT + décodeur mBART, ~631 M paramètres. Aucun poids ni moteur de Nemotron n'est installé. Faire un vrai portage CPU ONNX/quantification Android avec révisions, licence OpenMDW, tokenizer/NaViT, mémoire mesurée avant activation.
- Packs d'installation Play Store : aucun AAB Asset Delivery configuré. Pour éliminer même la préparation initiale de modèles sur l'app, étudier des packs *install-time* pour l'AAB ; la base est limitée à 500 Mo compressés, packs individuels à 1,5 Go et cumul base + install-time à 4 Go selon Google Play.
- Autorisations release/main non reçues : **ne pas merger main, ne pas publier Release ou Play**.

## Ordre impératif d'exécution
1. Lire `brainmap.md` puis seulement les tiroirs utiles et la méthode FAB-Copilot. Vérifier HEAD, dernier run CI, signature LAB/permissions exactes et APK direct; corriger toute régression.
2. Sur vrai Samsung SM-A576B : Wi-Fi non facturé, >2,2 Go stockage + réserve, téléchargement sans manipulation de fichiers, TLS redirection Hugging Face/Xet, checksum, absence de POST image/texte, interruption/reprise, écran éteint, faible espace.
3. Corriger persistance arrière-plan par service foreground notification/WorkManager, ne jamais bloquer UI. Reprendre sans double téléchargement et sans perdre état.
4. Sur la même image manuscrite historique de janvier 1942, vérifier deux sorties réelles Surya/LightOn ; comparer Indra et PP-OCR avec transcription humaine CER/WER et chiffres/noms propres intacts. Profiler pic RAM/PSS, latence, chauffe, rotation, arrêt/reprise.
5. Implémenter réellement Nemotron 3.5 Parse sur Android (révision source, pre/postprocess NaViT, ONNX/decoder mBART, poids quantifiés, SHA, licence), **ne pas simuler**.
6. Évaluer asset packs AAB pour installation tout inclus si publication Play souhaitée ; pour tests GitHub privilégier APK direct sans ZIP et téléchargement automatique des poids.
7. Tenir les cinq mémoires vivantes à jour, archivage sans perte, tests de régression, CI preuves. Ne pas réintroduire Liquid, Qwen textuel ni HobbyLM dans la chaîne OCR.

## Exigences de livraison
Source/push sur branche dédiée, tests verts réellement observés, APK LAB signé code incrémenté avec certificat constant si CI réussie, URL d'artefact direct. Journal de limites vérifiables. Aucun lien d'APK inventé. Aucune fusion ni Release sans autorisation de Fab.
