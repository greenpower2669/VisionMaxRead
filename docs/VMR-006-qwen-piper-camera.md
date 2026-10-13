# VMR-006 — Qwen / PP-OCR / Piper / caméra

Date : 13 octobre 2026  
Branche : `feature/qwen-piper-camera-v1`  
Statut : développement LAB, pas de merge main, pas de Release autorisée.

## Flux OCR
Capture/import → bitmap normalisé → Qwen3.5-0.8B-VL automatiquement. Le bouton **PP-OCRv6-Small / Comparer** relance le moteur Paddle historique sur exactement le même bitmap. L'interface affiche temps et taille des deux sorties puis permet de sélectionner le texte à corriger/lire.

## Flux vocal
Sherpa-ONNX/Piper UPMC medium, Jessica sid 0. Le modèle reste chargé. Avant la première lecture, « Ah, bon. » est généré puis réellement envoyé dans le même AudioTrack à gain de piste nul ; la fin est détectée via playback head. Chaque vraie lecture reçoit 250 ms de silence de garde. Le volume global Android n'est jamais modifié.

## Caméra
La rotation de prévisualisation combine SENSOR_ORIENTATION et la rotation Display ; la transformation est recalculée lors des changements de taille. La capture JPEG utilise son calcul d'orientation, puis ImageLoader normalise EXIF avant OCR.

## Validation exigée
CI : compilation Kotlin/Java, lint, APK/AAB, signature, ARM64, absence INTERNET, intégrité modèles.  
Samsung : orientation, mode avion, Qwen imprimé/colonnes/manuscrit, comparaison Paddle, première phrase Jessica, arrêt/reprise, RAM, chauffe, stockage et durée. Aucune réussite terrain ne doit être déduite de la CI.
