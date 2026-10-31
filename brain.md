# VisionMaxRead — cerveau opérationnel (VMR-021)

## Objectif actuel
Lire des documents historiques et manuscrits en français, hors ligne une fois les modèles installés. Fab refuse de manipuler ou télécharger lui-même les fichiers GGUF : le téléphone doit installer les poids automatiquement avec son consentement, vérifier l'intégrité, puis les garder dans son stockage privé. **Stockage persistant ≠ trois modèles en RAM simultanée**. Budget faible RAM/stockage, un seul gros OCR chargé à la fois.

## Chaîne actuelle
Indra det/rec + OrderNet / XY-cut + n-gram CTC restent les moteurs OCR de secours immédiatement embarqués. PP-OCRv6-Small compare sur le même bitmap. Surya OCR 2 et LightOnOCR-3 0.8B : moteur ARM64 `llama-mtmd-cli` compilé depuis commit épinglé, modèle GGUF et mmproj SHA256 épinglés ; VMR-020 réalise leur installation automatique Wi-Fi à la demande. Nemotron 3.5 Parse : C-RADIOv4/NaViT + mBART, portage distinct à faire, ni binaire ni poids fonctionnels dans l'APK.

L'APK VMR-020 autorise `INTERNET` et `ACCESS_NETWORK_STATE` **uniquement pour installer les modèles** ; `usesCleartextTraffic=false`. Pas de transfert de documents / OCR. Résultats candidats à comparer à la photo avant choix humain, jamais écrasement silencieux. Historique uniquement masqué, logs ordinaires sans textes, copies sensibles après confirmation.

## Preuves et limites
VMR-019/code22 : build ARM64 signé vert, run GitHub 41117112603, APK ≈396 Mo ; cela ne valide pas les sorties OCR réelles. VMR-020/code23 : vérifier le run du HEAD après changements ; premiers téléchargements de plusieurs Go et tests sur Samsung non confirmés. L'exécuteur de téléchargement dans Activity ne garantit pas l'achèvement en cas d'arrêt du processus : service foreground/WorkManager nécessaire.

## Précautions
Protéger identité LAB `fr.visionmaxread.labdebug` et certificat SHA256 `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`. Conserver bouton Arrêter, lisibilité, français, Wi-Fi non facturé pour acquisition, SHA256 à la copie et avant inférence, authentification amont et licences. Pas de fusion main, pas de Release sans accord. Référence détaillée : `docs/VMR-020-auto-models.md`.

[Historique intégral archivé](docs/memory-archives/2026-10-30-pre-vmr020/brain.md)

## Nouveau VMR-021
Les téléchargements Surya/LightOn s'exécutent dans `SpecialistOcrInstallService` Android foreground, et non plus dans l'exécuteur de l'Activity. Notification, état local, pause sur perte de Wi-Fi, reprise de `.part` et HTTPS lié au `Network` Wi-Fi/Ethernet non facturé. Pas de stockage de photo pour une reprise ultérieure après destruction Activity. À valider sur Samsung ; Android 15+ peut imposer une limite dataSync cumulée de 6 h/24 h et stopper l'application. Comparer l'option UIDT Android 14+ dans la suite.
Pistes Nemotron : pack ONNX INT8 tiers ~753 Mo (ORT 1.31 vs ORT Android 1.27), GGUF Q8 ~866 Mo via moteur tiers SkyVision.cpp ; **aucune inférence Android ni licence finale vérifiée**. Rapport : `docs/VMR-021-implementation.md`.
