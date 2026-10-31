# VisionMaxRead — TODO actif VMR-021 (31 octobre 2026)

## Terminé en source
- [x] VMR-019 : pont d'inférence image CPU ARM64 mtmd pour Surya OCR 2 et LightOnOCR-3 Q4, import manuel ancien, compilation Android CI verte code22 / run 41117112603.
- [x] VMR-020 : installer automatique public HTTPS Wi-Fi non facturé, SHA-256, reprise Range de fichiers partiels, limite de taille, stockage privé ; ne plus demander à Fab de télécharger/importer un GGUF.
- [x] UI : préparation des deux moteurs, action OCR proposant installation, état/progression, arrêt, suppression volontaire des poids, Indra restant disponible.
- [x] CI et manifest adaptés : INTERNET pour poids seuls, réseau non facturé, trafic clair interdit ; version de travail 0.7.0/code23.
- [x] Cinq mémoires vivantes archivées intégralement puis raccourcies avec carte centrale légère.

## Bloquants prioritaires — à valider, ne pas marquer sans preuve
- [ ] **CI HEAD VMR-020** : tests source, Gradle lint, APK signé, permissions, certificat, binaire natif et lien artifact direct ; corriger le run si rouge. Pas de Release sans accord.
- [ ] **Vraie installation sur Samsung** : Wi-Fi/Xet/CloudFront, HTTP redirects/Range, hash, affichage du progrès, reprise, espace libre, économie de données et coût. Vérifier source du modèle/empreinte réels, pas uniquement contrat statique.
- [ ] **Vraie inférence Surya et LightOn sur le même manuscrit de 1942** : prise en charge GGUF/mmproj, sortie stdout propre, prompt conforme, CER/WER après transcription humaine, RAM/PSS/RSS, chauffe, mémoire et latence.
- [ ] **Transfert robuste hors Activity** : service foreground/WorkManager + notification, persistance d'état, reprise après veille/rotation/kill Android, réseau perdu, annulation et nettoyage des fichiers sensibles.
- [ ] **Nemotron 3.5 Parse** : portage indépendant du runtime C-RADIOv4/NaViT+mBART+tokenizer, évaluer ONNX/NNAPI/CPU quantification, licences OpenMDW, poids et SHA, auto-provisionner seulement quand exécutable.
- [ ] **Distribution sans étape de téléchargement post-installation** : envisager AAB + Play Asset Delivery install-time ; respecter tailles Play (base 500 Mo, packs 1,5 Go chacun, total install-time 4 Go). APK direct reste avec téléchargement automatique de poids.
- [ ] Vérifier régression confidentialité, couverture des noms/dates/chiffres, TTS, masquage local, comparaison PP-OCR, mode avion **après installation**, stockage faible et absence de POST d'images.
- [ ] Tests Android 16/17 Samsung SM-A576B, rotation, screen-off, notification, volume, passage en arrière-plan.

**Contrainte** : aucune publication Release/merge `main` sans validation explicite de Fab ; aucun APK présenté comme « trois OCR fonctionnels » sans preuves.

[TODO et réalisations historiques intégraux](docs/memory-archives/2026-10-30-pre-vmr020/todo.md)

## VMR-021 — avancement strict
- [x] Code ajouté : service Android foreground de téléchargement indépendant de l'Activity, notification, arrêt, reprise sur Wi-Fi et liaison des sockets HTTPS au réseau vérifié.
- [x] Interface liée au service ; pas d'annulation involontaire à la destruction de l'Activity ; récupération d'état sans persistance d'images privées.
- [ ] Valider CI de l'ultime HEAD VMR-021 : test statique, lint/compilation, APK direct, certificat LAB constant.
- [ ] Tests Samsung SM-A576B : transfert réel, SHA, veille, kill, Wi-Fi interrompu, refus de notifications, limite FGS Android 15+, température.
- [ ] Mesurer Surya/LightOn sur lettre 1942 : référence humaine, CER/WER, erreurs critiques, PSS/RSS, temps et chauffe.
- [ ] Audit et port Nemotron : ONNX INT8 753 Mo ORT1.31 / GGUF Q8 866 Mo SkyVision.cpp, licences, tokenizer NaViT, ABI arm64 et test local avant activation.
- [ ] Étudier `JobScheduler` UIDT API34+ pour les longs transferts / AAB asset packs install-time, sans APK massif.
- [ ] Mode avion post-installation ; confirmer absence de connexion d'images/texte, sous-titres, TTS, Indra/PP-OCR et comparaisons.
- [ ] APK LAB direct uniquement après CI verte HEAD ; pas de Release, merge main ni Play.
