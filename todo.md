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
