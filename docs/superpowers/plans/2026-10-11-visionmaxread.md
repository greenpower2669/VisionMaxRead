# VisionMaxRead Implementation Plan

> Pour l’exécution agent : superpowers:executing-plans, réalisation native dans cette session.

**Goal:** Application Android OCR/lecture locale avec historique masqué.
**Architecture:** UI native Java, SDK PaddleOCR Kotlin isolé, sessions ONNX chargées à la demande ; frontière stricte entre texte affiché et texte persisté.
**Tech Stack:** Android 26–36, Java 17, Gradle 8.13, AGP 8.13, Kotlin 2.1, ONNX Runtime Android, OpenCV.
**Spec:** `docs/superpowers/specs/2026-10-11-visionmaxread-design.md`.

## Contraintes globales
Pas de permission INTERNET ; modèles inclus ; français ; aperçu léger/photo nette ; originaux non persistés ; branche dédiée ; pas de merge/release automatique.

## Points de revue
Orientation EXIF/caméra ; import trop volumineux ; arrêt pendant inférence/audio ; entités au bord d’un segment ; erreurs NER sans sauvegarde brute.

## Tâche 1 — Noyau et construction
- [ ] Créer les tests de `core/PrivacyRules`, `TextChunks`, `SessionGate` et constater leur échec.
- [ ] Implémenter ces contrats et obtenir des tests verts.
- [ ] Épingler sources SDK/modèles, licences et manifest d’intégrité ; fournir script de préparation.

## Tâche 2 — Image vers texte
- [ ] Intégrer SDK officiel dans `ppocr-sdk`, sans logs de contenu ni images persistées.
- [ ] Implémenter `CameraController`, `ImageLoader`, `OcrService` et UI ; gérer rotation et erreurs.
- [ ] Vérifier compilation et contrat image/résolution.

## Tâche 3 — Lecture et confidentialité
- [ ] Implémenter `SupertonicEngine`, `LocalAnonymizer`, `HistoryStore`.
- [ ] Tester règles, offsets, segments et refus de persistance après échec de masquage.
- [ ] Brancher Lire/Arrêter, réglages, historique, partage de texte et effacement.

## Tâche 4 — Livraison vérifiable
- [ ] Exécuter tests, lint et build APK/AAB ; contrôler modèles et permission réseau.
- [ ] Mettre à jour les cinq mémoires avec preuves et limites.
- [ ] Pousser sur branche dédiée, ouvrir PR et fournir les fichiers effectivement construits.
