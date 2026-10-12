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
- [ ] [CI] Vérifier sur commit final : compilation APK debug + AAB unsigned, contrôles Java/lint, absence de permission INTERNET, hashes des modèles.
- [ ] [HUMAIN] Télécharger l'archive de la CI puis comparer sur Samsung en mode avion trois documents non sensibles : imprimé, colonnes, manuscrit.
- [ ] [HUMAIN] Tester séparément une vraie IA multimodale dans Google AI Edge Gallery avec modèle vision compatible ; mesurer RAM et compréhension.
- [ ] [AGENT] Après mesures, choisir une architecture VLM réellement exécutable sur ce Samsung et la tester dans une **autre branche**, sans poids énormes ni publication automatique.
- [ ] [HUMAIN] N'autoriser merge main ou release qu'après vérification des mesures.

## VMR-002 — Dernier état CI (13 octobre 2026)
- [x] Le build du commit 5a43248b réussit Gradle, lint, APK/AAB et archive ; **échec de la vérification des permissions**, donc aucun APK déclaré prêt (run 38315196736).
- [x] Contrer la permission INTERNET ajoutée potentiellement par une dépendance avec `tools:node="remove"` et diagnostic explicite dans `verify_apk.py`.
- [ ] [CI] Confirmer sur un nouveau run `verify_apk.py`, signature debug, absence totale d'INTERNET et dépôt de l'artefact APK/AAB.
- [ ] [AGENT] Fournir à Fab un APK direct plutôt que ZIP si le canal de livraison le permet, sans merge ni release non autorisés.

## Preuve CI VMR-002 — 13 octobre 2026
- [x] Build vert sur commit `53356ee746dcb856cde0e37f851d12010dde74c7`, run [38316437083](https://github.com/greenpower2669/VisionMaxRead/actions/runs/38316437083) : Java/Gradle, lint, APK/AAB, archive modèle, `verify_apk.py` (signature, ARM64, 18 hashes, CAMERA et **aucune permission INTERNET**).
- [x] Artifact `VisionMaxRead-0.2.0-OCR-TEST` présent (ID 11777960102, ZIP d'environ 1,07 Go contenant APK de test et AAB non signé), expiration 26/10/2026 ; **aucun APK direct ni GitHub Release publiés**.
- [ ] [AGENT] Conformément à FAB-APK-TEMP-001, prévoir si autorisé un canal de distribution directe de l'APK sans ZIP ; GitHub Actions empaquette son téléchargement et le dépôt Git standard ne convient pas à ce gros fichier. Ne pas créer de Release sans accord de Fab.
- [ ] [HUMAIN] Installation Samsung et tests qualitatifs en mode avion non effectués.

## VMR-003 — Préversion APK directe, accord Fab du 13/10/2026
- [x] Autorisation explicite de publier une **prerelease test uniquement**, sans merge main et sans AAB unsigned : « Feu vert ».
- [x] Run Android source `38316437083`, SHA `53356ee7`, vert complet ; artifact `11777960102` présent.
- [x] Préparer `.github/workflows/publish-ocr-test.yml` avec permissions minimales, vérification de SHA/CI, téléchargement **dans GitHub Actions**, publication de l'APK seul, et re-téléchargement SHA256.
- [ ] [CI] Vérifier que ce workflow a créé la Release `v0.2.0-ocr-test` avec l'asset exact `VisionMaxRead-0.2.0-OCR-TEST.apk` et hash identique.
- [ ] [HUMAIN] Installer et valider sur Samsung en mode avion. Ne pas fusionner `main` avant autorisation distincte.

## VMR-003 — Correctif de publication du 13/10/2026
- [x] Run `38317719077` : téléchargement côté runner et validation ZIP/APK réussis, SHA256 APK `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438`.
- [x] Correction du faux positif « tag existant » sur réponse API 404 JSON. 
- [ ] [CI] Vérifier la nouvelle publication puis son re-téléchargement SHA256.

- [x] Second échec de publication (run `38317821483`) : le wrapper `gh api` expose son JSON 404 brut malgré l'expression jq ; remplacement par `git ls-remote` pour détecter un tag réellement existant.
- [ ] Confirmer nouvelle publication et hachage de l'APK re-téléchargé ; aucun lien direct annoncé avant vérification.
