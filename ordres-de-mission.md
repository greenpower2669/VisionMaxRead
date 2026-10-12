# Ordres de mission — VisionMaxRead

## VMR-001 — Application Android d’aide à la lecture (Fab, 11 octobre 2026)
**État : Première version construite et vérifiée ; livraison des fichiers, essai téléphone à effectuer.** Autorisation explicite : « Ok on code ça VisionMaxRead le repo est créé […] on y va ».

- Reconnaître du texte dans des images ; français ; fonctionnement hors ligne.
- Caméra : prévisualisation vidéo basse résolution pour viser seulement.
- Bouton rouge : figer une image ; reconnaître la photo nette, pas l’aperçu dégradé.
- Sélectionner une image présente sur le téléphone.
- Afficher le texte et un bouton Play pour le lire avec Supertonic 3.
- Recueillir les textes et les anonymiser si une IA hors ligne le permet.
- Dépôt fourni : greenpower2669/VisionMaxRead.

Interprétation annoncée : historique local après masquage automatique ; texte original uniquement à l’écran et en mémoire pour la lecture. Aucune transmission réseau. L’anonymisation automatique n’est pas une garantie d’anonymat.

La vidéo demandée ici est l’aperçu caméra. L’import de fichiers vidéo, évoqué pendant l’étude, est enregistré séparément comme possibilité à confirmer et ne doit pas remplacer les fonctions caméra/photo prioritaires.

## Contraintes de livraison
Branche dédiée ; pas de fusion dans main ni de publication de release sans validation de Fab. APK de test nommé VisionMaxRead + version, icône identifiable ; AAB distinct si produit. Tests téléphone à marquer « à valider ».

## VMR-002 — Essai contrôlé de reconnaissance (Fab, 13 octobre 2026)
**État : prototype en branche de test, validation CI et Samsung en attente.** Accord utilisateur : « On test? », en réaction à la proposition de comparer nos moteurs et d'évaluer une IA multimodale.

Objectif : ne pas toucher à la version stable ; créer un comparateur local de reconnaissance PaddleOCR vs ML Kit latin (bundled), même image, durées et résultats lisibles. Préserver confidentialité et fonctionnement avion. Comparer séparément une vraie IA vision/langage via application de démonstration compatible ; aucune affirmation de multimodalité si seul OCR fonctionne.

Branche : `feature/ocr-ab-benchmark-v1` ; protocole : `docs/VMR-002-essai-ocr.md`. Ne pas fusionner/releaser avant accord et résultats d'essai.
