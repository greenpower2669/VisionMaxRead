# VisionMaxRead — journal diagnostic actif, VMR-021

## 30/10/2026 — état des preuves
- VMR-019 (branche `feature/vmr019-native-ocr-surya-lighton`) : CI finale run 41117112603 `success`, signature LAB constante et APK direct `VisionMaxRead-0.6.9-VMR019-NATIVE-OCR-LAB.apk` (~396 Mo). Cela prouve uniquement la compilation et les contrôles statiques/archives, **pas** l'inférence des modèles sur Samsung.
- VMR-020 : suppression de l'import manuel, autorisation réseau modifiée pour acquisition des poids, stockage privé, Wi-Fi non facturé, SHA256, reprise et annulation. Contrôles en cours sur le HEAD, non assimilables à une vérification réseau réelle.
- Préserver OCR de référence Indra + LM/OrderNet et PP-OCR ; ne pas confondre leurs scores avec OmniParseBench. La lettre de Paris de janvier 1942 doit servir de cas difficile avec transcription humaine de référence.
- Risque connu : gros téléchargement à partir d'une Activity, susceptible d'être arrêté par Android en veille/kill ; ajouter service robuste et notification. Risque serveur HTTP Range / Xet ; tester en situation réelle. Pas de preuve que Surya/LightOn terminent ou produisent du texte correct.
- Nemotron (C-RADIOv4/NaViT+mBART) est *non porté* et *non téléchargé* ; ne pas le marquer exécuté.
- Changement de confidentialité volontaire : permission INTERNET présente, réservée dans le code aux poids HTTPS. Contrôler absence d'envoi d'image/texte et mode avion après installation.

## Reprise d'un bug
Ouvrir les logs GitHub Actions du run exact, noter commit SHA, étape en défaut, correctif ciblé, run suivant. Pour les essais sur Samsung, utiliser « Copier logs techniques » et le rapport des passes, anonymiser toute copie sensible. Ne pas s'appuyer sur des captures d'écran uniquement.

[Historique complet des diagnostics, corrections et essais](docs/memory-archives/2026-10-30-pre-vmr020/debughistorical.md)

## 31/10/2026 — audit et correctifs VMR-021
- HEAD VMR-020 `b715440c16fc` vérifié, run Android `41135591282` conclu `success` le 30/10 ; **aucune mesure de téléchargement ni OCR Samsung**.
- Le téléchargement était auparavant dans `MainActivity` et annulé par `destroyed`/`onDestroy`; migration vers `SpecialistOcrInstallService` + notification et `START_REDELIVER_INTENT`.
- Sécurisation des connexions HTTP(S) via `Network.openConnection` après confirmation Wi-Fi/Ethernet non facturé pour éviter la bascule par défaut vers cellulaire.
- Audit de code a détecté deux définitions `onPause` lors de l'intégration initiale ; corrigé en conservant pause caméra et désabonnement receiver.
- Correctifs source uniquement : CI du HEAD final, redélivrance réelle, notification Android et réseaux Hugging Face/Xet restent à tester. Nouveaux candidats Nemotron ONNX INT8 et SkyVision GGUF **non intégrés**.
- Risque FGS `dataSync` : six heures cumulées / 24 h arrière-plan Android 15+. Ne pas promettre une reprise après arrêt forcé système. Voir `docs/VMR-021-implementation.md`.
