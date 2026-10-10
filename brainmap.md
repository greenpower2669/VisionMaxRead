# Carte technique — VisionMaxRead

- `app/` : application Android, Java, interface native accessible.
  - `MainActivity` : navigation caméra/photo/texte, commandes, statut, historique et réglages.
  - `CameraController` : Camera2, aperçu léger, capture JPEG distincte et orientation.
  - `ImageLoader` : import borné, orientation EXIF, mémoire.
  - `OcrService` : adaptation du SDK PaddleOCR, ordre de lecture, résultat texte.
  - `SupertonicEngine` : quatre réseaux ONNX, styles, génération/audio, interruption.
  - `LocalAnonymizer` : NER ONNX multilingue + règles ; ne persiste rien.
  - `HistoryStore` : stockage privé des seuls textes masqués, suppression.
  - `core/` : règles et segmentation testables sans Android ; offsets et protection des callbacks périmés.
- `ppocr-sdk/` : source Android officielle PaddleOCR adaptée ; ONNX Runtime et OpenCV ; licences/source identifiées.
- `scripts/` : préparation et validation des modèles, tests de contrats, construction reproductible.
- `.github/workflows/` : vérification et APK/AAB ; aucun déploiement/release automatique.
- `docs/superpowers/` : conception, plan et journal d’exécution.
- `app/src/main/assets/` : modèles générés localement/CI, exclus de Git ; manifest d’intégrité.

Le thread UI ne réalise aucune inférence. Les commandes annulent la génération précédente. Le texte brut ne traverse pas la frontière de persistance. Les erreurs de modèle sont visibles et empêchent le faux succès.
