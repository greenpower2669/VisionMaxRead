# VisionMaxRead — cerveau opérationnel (VMR-022)

## Objectif actuel
Lire des documents historiques et manuscrits en français, hors ligne une fois les modèles installés. Fab refuse de manipuler ou télécharger lui-même les fichiers GGUF : le téléphone doit installer les poids automatiquement avec son consentement, vérifier l'intégrité, puis les garder dans son stockage privé. **Stockage persistant ≠ trois modèles en RAM simultanée**. Budget faible RAM/stockage, un seul gros OCR chargé à la fois.

## Comparaison depuis VMR-022
Un bouton « Tester tous les OCR · même photo » prépare les poids LightOn/Surya manquants, puis évalue séquentiellement Indra, PP-OCR, LightOn et Surya sur le même bitmap. Les résultats s'ouvrent séparément et ne remplacent jamais le texte sans choix explicite. Nemotron est clairement désactivé tant que son runtime ONNX Android n'est pas validé. Version LAB 0.7.1/code24, CI et Samsung à contrôler. Voir `docs/VMR-022-compare-all-ocr.md`.

## Chaîne actuelle
Indra det/rec + OrderNet / XY-cut + n-gram CTC restent les moteurs OCR de secours immédiatement embarqués. PP-OCRv6-Small compare sur le même bitmap. Surya OCR 2 et LightOnOCR-3 0.8B : moteur ARM64 `llama-mtmd-cli` compilé depuis commit épinglé, modèle GGUF et mmproj SHA256 épinglés ; VMR-021 déplace leur installation automatique Wi-Fi dans un service dataSync indépendant de l'Activity ; progression/annulation par notification et état privé persistant, reprise .part/HTTP Range/SHA. Nemotron 3.5 Parse : C-RADIOv4/NaViT + mBART, portage distinct à faire, ni binaire ni poids fonctionnels dans l'APK.

L'APK VMR-020 autorise `INTERNET` et `ACCESS_NETWORK_STATE` **uniquement pour installer les modèles** ; `usesCleartextTraffic=false`. Pas de transfert de documents / OCR. Résultats candidats à comparer à la photo avant choix humain, jamais écrasement silencieux. Historique uniquement masqué, logs ordinaires sans textes, copies sensibles après confirmation.

## Preuves et limites
VMR-019/code22 : build ARM64 signé vert, run GitHub 41117112603, APK ≈396 Mo ; cela ne valide pas les sorties OCR réelles. VMR-020/code23 : CI verte run 41135591282. VMR-021/code23 : service foreground développé, CI du HEAD et essais sur Samsung non confirmés. Redémarrage après force-stop, quota Android dataSync et essai réseau réel restent ouverts ; WorkManager à comparer.

## Précautions
Protéger identité LAB `fr.visionmaxread.labdebug` et certificat SHA256 `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`. Conserver bouton Arrêter, lisibilité, français, Wi-Fi non facturé pour acquisition, SHA256 à la copie et avant inférence, authentification amont et licences. Pas de fusion main, pas de Release sans accord. Références : `docs/VMR-020-auto-models.md`, `docs/VMR-021-foreground-provisioning.md`, `docs/VMR-022-compare-all-ocr.md`.

[Historique intégral archivé](docs/memory-archives/2026-10-30-pre-vmr020/brain.md)
