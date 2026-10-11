# VisionMaxRead — première version Android

## Résultat demandé
Une aide à la lecture française utilisable sans connexion : viser une feuille, figer une photo, voir/corriger le texte et appuyer sur Lire ; importer une image ou coller du texte. Les textes reconnus rejoignent un historique local masqué.

## Choix
Application native Java/Camera2 pour distinguer aperçu et capture. PaddleOCR PP-OCRv6 Small fournit la détection et la reconnaissance par son SDK Android officiel. Supertonic 3 produit la voix. Une NER multilingue compatible français repère personnes/lieux/organisations ; des règles complètent les coordonnées.

L’option ML Kit serait plus petite mais ne constitue pas une assurance pour le manuscrit photographié. Un gros modèle vision/langage augmente sensiblement le poids et la latence. Small constitue la première comparaison réalisable sur téléphone ; sa qualité réelle reste à tester.

## Parcours
- Écran principal à commandes larges : Caméra, Photo, Texte ; Figer rouge et Lire/Arrêter.
- Aperçu limité vers 640×480 ; JPEG caméra indépendant jusqu’à 12 MP, bitmap OCR borné à 2560 px pour la mémoire.
- Figer capture la photo, ferme l’aperçu et lance l’OCR ; aucune reconnaissance continue sur l’aperçu.
- Photo ouvre le sélecteur Android, respecte EXIF et ne copie pas l’original dans l’historique.
- Texte accepte collage, partage et sélection Android ; édition lisible, taille réglable.
- Lecture explicite, phrase par phrase, annulation immédiate de l’audio et des prochains segments.
- Historique contient les seuls textes masqués. Suppression individuelle et totale ; aperçu avant partage.

## Confidentialité et erreurs
Pas de permission INTERNET, pas de télémétrie ni de sauvegarde Android. NER et regex ne garantissent pas l’anonymat ; aperçu à vérifier. L’original reste en mémoire pour lire fidèlement. Échec NER = pas d’enregistrement. Résultat périmé ignoré après changement de source. Modèles manquants signalés clairement, pas de fallback vocal silencieux.

## Validation
Tests sur segmentation sans perte de texte, offsets Unicode et remplacement d’entités, coordonnées et chevauchements, callbacks périmés, frontière historique. Compilation et lint Android ; validation des signatures ONNX et des modèles ; APK inspectée sans permission réseau. Tests réels téléphone à effectuer en mode avion : capture/import, longues lectures, arrêt, historique et manuscrit français.
