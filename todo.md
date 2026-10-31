# VisionMaxRead — TODO actif VMR-021 (31 octobre 2026)

## Terminé en source
- [x] VMR-021 : service foreground dataSync, notification/arrêt, état d'installation privé, reprise sans Activity, liaison stricte au réseau Wi-Fi non facturé, réessais, contrôle HTTP 416 et quota Android 15+. Tests statiques ajoutés ; **CI du HEAD à vérifier**.
- [x] VMR-020 HEAD b715440c : CI `success` run 41135591282, code23, APK signé selon pipeline ; aucune mesure d'OCR Samsung.
- [x] VMR-019 : pont d'inférence image CPU ARM64 mtmd pour Surya OCR 2 et LightOnOCR-3 Q4, import manuel ancien, compilation Android CI verte code22 / run 41117112603.
- [x] VMR-020 : installer automatique public HTTPS Wi-Fi non facturé, SHA-256, reprise Range de fichiers partiels, limite de taille, stockage privé ; ne plus demander à Fab de télécharger/importer un GGUF.
- [x] UI : préparation des deux moteurs, action OCR proposant installation, état/progression, arrêt, suppression volontaire des poids, Indra restant disponible.
- [x] CI et manifest adaptés : INTERNET pour poids seuls, réseau non facturé, trafic clair interdit ; version de travail 0.7.0/code23.
- [x] Cinq mémoires vivantes archivées intégralement puis raccourcies avec carte centrale légère.

## Bloquants prioritaires — à valider, ne pas marquer sans preuve
- [ ] **CI HEAD VMR-021** : tests source, Gradle lint, APK signé, permissions, certificat, binaire natif et lien artifact direct ; corriger le run si rouge. Pas de Release sans accord.
- [ ] **Vraie installation sur Samsung** : Wi-Fi/Xet/CloudFront, HTTP redirects/Range, hash, affichage du progrès, reprise, espace libre, économie de données et coût. Vérifier source du modèle/empreinte réels, pas uniquement contrat statique.
- [ ] **Vraie inférence Surya et LightOn sur le même manuscrit de 1942** : prise en charge GGUF/mmproj, sortie stdout propre, prompt conforme, CER/WER après transcription humaine, RAM/PSS/RSS, chauffe, mémoire et latence.
- [ ] **Valider le service VMR-021 sur Samsung** : notification visible, écran éteint, sortie Activity, kill Android, reprise réseau, permission refusée, quota dataSync, coupure, annulation et fragments `.part`. Le code source est écrit ; preuve terrain manquante. Évaluer WorkManager/DownloadManager pour les limites OS.
- [ ] **Nemotron 3.5 Parse** : portage indépendant du runtime C-RADIOv4/NaViT+mBART+tokenizer, évaluer ONNX/NNAPI/CPU quantification, licences OpenMDW, poids et SHA, auto-provisionner seulement quand exécutable.
- [ ] **Distribution sans étape de téléchargement post-installation** : envisager AAB + Play Asset Delivery install-time ; respecter tailles Play (base 500 Mo, packs 1,5 Go chacun, total install-time 4 Go). APK direct reste avec téléchargement automatique de poids.
- [ ] Vérifier régression confidentialité, couverture des noms/dates/chiffres, TTS, masquage local, comparaison PP-OCR, mode avion **après installation**, stockage faible et absence de POST d'images.
- [ ] Tests Android 16/17 Samsung SM-A576B, rotation, screen-off, notification, volume, passage en arrière-plan.

**Contrainte** : aucune publication Release/merge `main` sans validation explicite de Fab ; aucun APK présenté comme « trois OCR fonctionnels » sans preuves.

[TODO et réalisations historiques intégraux](docs/memory-archives/2026-10-30-pre-vmr020/todo.md)
