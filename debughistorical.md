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

## 31/10/2026 — VMR-021, service foreground (preuves limitées)
- HEAD VMR-020 `b715440c` : CI `success`, run 41135591282 ; APK et métadonnées disponibles via Actions. **Ne prouve aucun OCR sur Samsung.**
- VMR-021 : `SpecialistOcrDownloadService` remplace l'exécuteur d'installation lié à MainActivity ; `Network.openConnection` empêche les bascules involontaires vers les données mobiles, reprise HTTP et notification. CI du HEAD VMR-021 / installation réelle / mode avion non confirmés à ce stade.
- Android 15+ : quota dataSync 6 h/24 h en arrière-plan ; `onTimeout` ajouté. Redémarrage après force-stop Android impossible à garantir ; vérifier Samsung 16/17.
- Nemotron C-RADIOv4/NaViT+mBART reste non porté. Source architecture NVIDIA : https://huggingface.co/nvidia/Nemotron-3.5-Parse
