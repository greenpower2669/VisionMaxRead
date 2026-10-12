# VisionMaxRead v0.2.0 OCR TEST — préversion pour Samsung

**Test expérimental uniquement, hors ligne.** Version `fr.visionmaxread.ocrtest` installable à côté de `fr.visionmaxread`. Ne remplace pas la version stable, ne doit pas être publiée sur Google Play.

## Contenu
- Reconnaissance originale : PaddleOCR PP-OCRv6 Small.
- Comparateur expérimental : Google ML Kit latin v2, **modèle embarqué** dans l'APK.
- Après une photo, bouton **Comparer les OCR (test)** : même image, temps des deux inférences, affichage de chaque transcription, choix du texte à lire.
- Aucun modèle vision/langage (VLM) n'est encore intégré : c'est un test de moteurs OCR, pas une IA multimodale.
- Aucune permission Android INTERNET dans l'APK ; les modèles fonctionnent en mode avion. Le protocole de sauvegarde/masquage existant s'applique à la reconnaissance normale ; la comparaison ne sauvegarde pas son texte brut.

## Installation et test
1. Télécharger **directement** `VisionMaxRead-0.2.0-OCR-TEST.apk` dans les assets ci-dessous (pas de ZIP, pas d'AAB).
2. Installer en autorisant temporairement les applications issues de GitHub si Android le demande. Privilégier un espace libre d'au moins 2 Go ; les modèles locaux sont volumineux.
3. Passer en **mode avion**, ouvrir la nouvelle app « VisionMaxRead TEST OCR ».
4. Importer un imprimé français, un tableau en colonnes et une image manuscrite non sensibles. Après chaque capture, utiliser **Comparer les OCR (test)** et comparer exactitude, chiffres, accents, ordre de lecture et temps affichés.
5. Pour une comparaison à chaud, recommencer sur la même image ; la première utilisation inclut la préparation des modèles.
6. Envoyer un retour sur les erreurs OCR et la vitesse. Les essais sur téléphone ne sont **pas encore validés**.

## Traçabilité
- Branche : `feature/ocr-ab-benchmark-v1`, PR #2 maintenue ouverte et non fusionnée.
- Source de l'APK : commit `53356ee746dcb856cde0e37f851d12010dde74c7`.
- GitHub Actions : https://github.com/greenpower2669/VisionMaxRead/actions/runs/38316437083 — Java/Gradle, lint, APK et AAB, contrat d'archives, signature, ARM64, 18 hashes de modèles et absence de permission INTERNET réussis.
- La Release publie **uniquement l'APK de test** (debug signé). L'AAB expérimental reste dans l'artefact CI et est non signé.
- Distribution approuvée par Fab le 13/10/2026 ; aucun merge sur `main`.
