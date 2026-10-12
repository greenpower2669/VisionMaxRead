# Ordres de mission — VisionMaxRead

## IMMUTABLE / À PRÉSERVER — FAB-IMMUTABLE-001
- Identité Android actuelle de l'application principale : `fr.visionmaxread` (projet en expérimentation ; **aucune identité Google Play publiée attestée**).
- Variante **debug uniquement**, distincte et installable en parallèle : `fr.visionmaxread.ocrtest` sur la branche VMR-002. Ne jamais substituer ce package expérimental à l'identité principale ni publier son AAB sans réévaluation du package et de la signature.
- Données sources et images privées : aucune permission `android.permission.INTERNET` dans l'APK validé ; contrôle obligatoire `scripts/verify_apk.py` après fusion des manifestes.
- Distribution test VMR-002 : `VisionMaxRead-0.2.0-OCR-TEST.apk` (debug signé) et `VisionMaxRead-0.2.0-OCR-TEST-unsigned.aab` (non signé, non publiable sur Play en l'état).
- Aucune Upload Key ni App Signing Key Google Play établie pour cette application dans ce dépôt. Ne conserver dans Git/mémoires ni secret ni keystore privé. Toute modification d'identité ou de protocole de signature nécessite accord de Fab et avenant daté.

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
**État : APK expérimental compilé et vérifié CI sur commit 53356ee7 ; validation Samsung en attente.** Accord utilisateur : « On test? », en réaction à la proposition de comparer nos moteurs et d'évaluer une IA multimodale.

Objectif : ne pas toucher à la version stable ; créer un comparateur local de reconnaissance PaddleOCR vs ML Kit latin (bundled), même image, durées et résultats lisibles. Préserver confidentialité et fonctionnement avion. Comparer séparément une vraie IA vision/langage via application de démonstration compatible ; aucune affirmation de multimodalité si seul OCR fonctionne.

Branche : `feature/ocr-ab-benchmark-v1` ; protocole : `docs/VMR-002-essai-ocr.md`. Ne pas fusionner/releaser avant accord et résultats d'essai.

## VMR-003 — Publication de la préversion APK directe (Fab, 13 octobre 2026)
**Commande explicite : « Feu vert »** après proposition de publier VisionMaxRead OCR TEST sous forme de préversion avec APK téléchargeable directement, sans ZIP sur Samsung et **sans fusion main**.

- Diffusion autorisée **uniquement** pour la variante test `fr.visionmaxread.ocrtest`, tag `v0.2.0-ocr-test`, APK direct `VisionMaxRead-0.2.0-OCR-TEST.apk` (source vérifiée au SHA `53356ee7`).
- Réutiliser la CI verte `38316437083`, préserver l'absence de permission INTERNET ; jamais d'AAB unsigned présenté comme prêt Google Play.
- Publier via un workflow contrôlé GitHub Actions ; re-télécharger l'asset Release et vérifier son SHA256 avant d'annoncer la livraison.
- **Statut : publication automatisée demandée ; preuve de Release à vérifier.** Aucun merge `main`, aucun changement de signature du stable.
