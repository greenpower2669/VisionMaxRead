# VisionMaxRead LAB v0.3.0 — essai de voix Android hors ligne

**Préversion expérimentale** destinée au Samsung, et non à Google Play.

- APK direct : `VisionMaxRead-0.3.0-LAB.apk`, sans ZIP ni AAB à télécharger.
- Identité Android **distincte** : `fr.visionmaxread.lab`, nom `VisionMaxRead LAB`. N'écrase ni la version stable, ni `VisionMaxRead TEST OCR`.
- Conservation du comparateur PaddleOCR et ML Kit de v0.2.0.
- Réglages : choix volontaire **Supertonic 3** (inchangé par défaut) ou **voix française Android déclarée hors ligne**. Si la voix n'est pas installée, l'essai affiche une erreur sans activer de voix réseau.
- L'application ne demande pas la permission INTERNET ; réaliser les tests en **mode avion** pour contrôler le moteur vocal installé.
- Le chronomètre du moteur Android mesure le signal de début de synthèse, pas le premier son audible.
- **Aucune IA Qwen n'est intégrée à cette préversion** ; reconnaissance multimodale Qwen prévue dans une étape distincte.

## Comment tester

Installer le seul APK ci-dessous, ouvrir VisionMaxRead LAB, choisir dans Réglages la voix Android hors ligne, puis comparer la lecture de la même phrase avec Supertonic. Tester l'arrêt, la reprise, une page entière et le mode avion.

## Origine et intégrité

L'APK provient exclusivement du [build GitHub Actions 38365535174](https://github.com/greenpower2669/VisionMaxRead/actions/runs/38365535174), commit `644d5177145062f28aab2bd2f883ddbf0331390d`, vérifié (Java/Gradle, lint, signature APK, modèles et absence de permission INTERNET). L'archive contenant aussi un AAB unsigned est extraite **dans `RUNNER_TEMP` sur GitHub**, puis seul l'APK est attaché à cette préversion. Le workflow retélécharge cet APK et compare son SHA256 avant d'annoncer la livraison.

`main` reste inchangée. Pas de fusion ni de publication stable. Retour d'essai Samsung encore attendu.
