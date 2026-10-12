# VMR-002 — Protocole d'essai OCR hors ligne (branche de test)

État : code et compilation GitHub Actions vérifiés (run 38316437083) ; préversion APK directe autorisée, à vérifier sur GitHub Release ; essais Samsung à faire. **Ne pas confondre avec une IA multimodale intégrée.**

## Ce que construit cette branche
- APK de test `fr.visionmaxread.ocrtest` installable à côté du stable, sans toucher à `main`.
- Même photo déjà affichée -> bouton **Comparer les OCR (test)**.
- PaddleOCR PP-OCRv6 Small d'origine vs **ML Kit Text Recognition v2 latin**, modèle **bundled** disponible hors ligne dès l'installation.
- Comparaison séquentielle sur les mêmes pixels, temps d'initialisation + inférence en millisecondes, nombre de caractères ; accès aux textes et choix explicite du texte à afficher.
- Le bouton de comparaison n'enregistre aucun de ses résultats et ne transmet pas l'image. Les règles préexistantes d'historique pour l'OCR normal restent actives.
- Ne pas comparer les nombres de caractères comme s'ils étaient des taux de justesse. Le premier passage inclut le démarrage à froid ; répéter pour une comparaison à chaud.

## Essai terrain recommandé
1. Installer l'APK **OCR-TEST** depuis la GitHub **prérelease v0.2.0-ocr-test** (asset direct `VisionMaxRead-0.2.0-OCR-TEST.apk`), sans télécharger le ZIP GitHub Actions ; préserver la version stable. Vérifier l'espace libre ; chaque installation comporte déjà de gros modèles.
2. Passer en **mode avion**, désactiver l'enregistrement d'historique pour des documents sensibles, et préparer trois images non sensibles : français imprimé, tableau / deux colonnes, manuscrit français.
3. Importer chaque même image puis appuyer sur « Comparer les OCR (test) » ; relever les temps et relire les erreurs visibles. Refaire l'essai une deuxième fois pour séparer démarrage à froid et passage répété.
4. Pour un **vrai essai multimodal**, installer séparément Google AI Edge Gallery et un modèle vision compatible *avant* de passer en mode avion. Ne pas intégrer de gros poids Gemma au dépôt/à l'APK : les exigences mémoire et stockage doivent être mesurées sur le téléphone. Comparer la description et l'ordre de lecture aux deux OCR.
5. Critères : fidélité des noms, chiffres, accents, ordre de lecture, délai jusqu'au texte, stabilité/RAM, confidentialité.

## Garde-fous
- Modèle ML Kit explicitement **bundled** (`com.google.mlkit:text-recognition:16.0.1`), **pas** la variante Play Services qui télécharge les poids.
- Aucune permission INTERNET ; contrôle dans `scripts/verify_apk.py`.
- Fab a donné le 13/10/2026 son accord pour **une préversion de test uniquement**, sans merge `main` : voir VMR-003 dans `ordres-de-mission.md`.
- Reconnaissance d'image par Gemma/LiteRT-LM : **étude séparée**, n'est pas intégrée au code de cette branche.
- Aucun texte utilisateur stocké dans Git, logs de tests ou télémétrie.

Sources techniques :
- https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- https://developers.google.com/edge/litert-lm/android
- https://github.com/google-ai-edge/gallery
