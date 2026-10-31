# VisionMaxRead — ordres de mission actifs (31 octobre 2026)

## Mission VMR-020 — priorité explicite de Fab
« Intègre-les tous en mémoire ; l'utilisateur ne doit pas télécharger lui-même les fichiers ; mets à jour et optimise les mémoires vivantes, puis prompt de passation. »

**Interprétation opérationnelle** : installer les poids OCR automatiquement par l'application avec accord explicite et Wi-Fi non facturé ; garder les modèles installés sur le téléphone, **sans tenir plusieurs gros modèles simultanément en RAM**. L'utilisateur ne cherche ni GGUF ni projecteur et ne les importe pas. Séparer intégration du modèle, disponibilité des poids et preuve d'inférence. Avec un APK direct, conserver l'installation initiale automatique ; pour une expérience 100 % incluse à l'installation Play, évaluer asset packs install-time.

## Livraisons demandées
1. Surya OCR 2 + LightOnOCR-3 0.8B : authentification SHA-256 des poids GGUF/mmproj, téléchargement et reprise dans le stockage privé, état lisible, annulation, exécution locale et comparaison vérifiable au document original.
2. Nemotron 3.5 Parse : finir **vraiment** le portage C-RADIO/NaViT/mBART Android avant de déclarer disponible ou de télécharger inutilement ses poids.
3. Préserver Indra, PP-OCR, ordre des colonnes, historique masqué, lecteur vocal, boutons de copie, refus des résultats inventés et lisibilité accessible.
4. Vraie CI complète, essais Samsung et preuves mesurées ; APK direct signé sans ZIP uniquement quand validé ; pas de merge main ni de Release sans autorisation distincte.
5. Tenir les cinq mémoires vivantes **légères**, toutes versions antérieures archivées à l'identique. Document de référence : `docs/VMR-020-auto-models.md` ; passation : `docs/VMR-020-passation.md`.

[Ordres de mission historiques exacts](docs/memory-archives/2026-10-30-pre-vmr020/ordres-de-mission.md)

## Mission VMR-021 — passation du 31 octobre
Reprendre depuis VMR-020 HEAD b715440c, ne pas recommencer, conserver les cinq mémoires légères et leurs archives. Installer automatiquement Surya et LightOn avec service Android résistant à la sortie de l'Activity, un modèle lourd en RAM à la fois, sans manipulations GGUF. Ne qualifier d'OCR réel que les exécutions mesurées sur Samsung (lettre française de janvier 1942 : CER/WER, dates, noms, chiffres, PSS, chauffe). Porter Nemotron C-RADIOv4/NaViT+mBART séparément avant de l'activer ; étudier Play Asset Delivery sans gonfler l'APK direct. Livrer APK sans ZIP seulement après CI/signature réussies. Pas de merge main, Release ou Play sans nouvel accord.

État : service foreground implémenté en source sur la branche VMR-021, essais Android et inférence à vérifier. Rapport : `docs/VMR-021-foreground-provisioning.md`.
