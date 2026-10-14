# VMR-007 — OCR spécialisé optionnel et voix française haute articulation

Date : 2026-10-14. Branche : `feature/vmr007-specialist-ocr-audio-hq-v1`.
Autorisation : Fab « Ok, on se lance et l audio en qualité maximal ».
Statut : implémentation expérimentale, validation CI ET Samsung requises.

## Garanties immuables
- Aucun merge sur `main`, aucune Release sans validation de Fab.
- Aucun accès réseau ni permission INTERNET dans l’application.
- Signature LAB persistante `fr.visionmaxread.labdebug` (SHA256 certificat `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`).
- Qwen3.5 0.8B-VL par défaut, PP-OCRv6-Small indépendant.
- Ne pas toucher à la correction caméra VMR-006/0.4.3 avant retest Samsung.

## OCR spécialisé
- Modèle `litert-community/PaddleOCR-VL-1.6` ; bundle `PaddleOCR-VL-1.6.litertlm`.
- Taille exacte : 1 390 305 472 octets ; SHA256 :
  `314df8b34d6911990d488e825602550a5bbcc5d6700cb1a8914e853e9043f4f1`.
- Page de référence : https://huggingface.co/litert-community/PaddleOCR-VL-1.6 .
- L'utilisateur télécharge le fichier **hors de l'application** dans le navigateur puis l'importe avec Réglages → Importer PaddleOCR-VL-1.6. Le sélecteur natif fournit uniquement un URI temporaire ; le modèle est copié avec SHA256 et taille vérifiés en zone privée. Aucune nouvelle permission INTERNET.
- Une seule inférence LiteRT géante à la fois : le `qwenWorker` existant sérialise Qwen et PaddleOCR-VL ; `unload()` libère le moteur précédent avant initialisation du suivant.
- PP-OCRv6-Small conserve son worker distinct pour le secours.
- Prompt exact spécialisé : `OCR:` ; conversation vierge par document.
- Aucun texte OCR brut ni image dans les logs. Même bitmap pour comparer ; mesures temps global et PSS processus.
- **Limites déclarées du bundle communautaire** : image traitée à 560×560 ; très petits caractères et ratio extrême à tester ; configuration CPU réelle Samsung inconnue. Ne pas promettre gain de qualité ou vitesse avant comparaison.

## Audio
- Deux voix locales Piper via Sherpa-ONNX 1.13.4 :
  - Siwis fr_FR-siwis-medium / speaker 0, proposée comme mode « articulation HD », activée par défaut sur la LAB.
  - Jessica fr_FR-upmc-medium / speaker 0, référence conservée et sélectionnable.
- Un seul moteur TTS chargé : changement de voix effectué entre les lectures par la file sérielle `worker`.
- Le son sort en PCM float 32 bits par AudioTrack `USAGE_ASSISTANCE_ACCESSIBILITY`; silence garde 250ms uniquement sur la première section d'une lecture (limite les coupures entre les sections).
- Modèle Siwis téléchargé uniquement sur le runner GitHub Actions, archive `vits-piper-fr_FR-siwis-medium.tar.bz2`.
- SHA256 de l'asset officiel : `375909aa30842b3a4efa10b1beb1d761af792960ae6873b4d53889f96c66195b` (release k2-fsa/sherpa-onnx ID d'asset 324609218).
- Source : https://k2-fsa.github.io/sherpa/onnx/tts/all/French/vits-piper-fr_FR-siwis-medium.html
- « Haute articulation » n'est pas synonyme de meilleure naturalité mesurée. Kokoro français nécessitera un protocole séparé ; ne pas substituer un modèle sans G2P français Android prouvé.

## Séquence de vérification
1. Vérifier CI : Gradle, lint, SHA256 archive et nouveaux modèles, 47 contrats, APK LAB signé, AAB unsigned.
2. Vérifier APK `fr.visionmaxread.labdebug` version `0.4.4-debug`, code 8, certificat LAB et aucune permission INTERNET.
3. Samsung ARM64 Android 16 : nouveau mode Siwis et bascule Jessica, première syllabe après arrêt/reprise, accentuation, chiffres et phrases longues.
4. Import du modèle OCR : échecs 500Mo / mauvaise empreinte / stockage insuffisant ; import authentifié exact 1,39 Go.
5. Vérifier que l'OCR spécialisé ne charge pas Qwen en parallèle ; Qwen se recharge au document suivant ; PP-OCR utilisable en panne.
6. Benchmark OCR français mêmes images, corrections et erreurs vérifiées, PSS/CPU/chauffe, pivot caméras portrait/paysage sans altérer 0.4.3.
7. Évaluer ensuite OvisOCR2 GGUF/llama.cpp dans une branche distincte, sans nouveau moteur natif par défaut.

## Statut
Un push de code n'est pas un APK validé, et une CI verte n'est pas une preuve de fonctionnement Samsung.
Ne créer ni Release ni merge main avant feu vert distinct de Fab.

### VMR-007 — livraison APK direct, sans ZIP (14/10/2026)
- Changement GitHub Actions : `actions/upload-artifact@v7`, `archive: false` pour l'APK unique et pour l'AAB unsigned dans deux artefacts distincts ; upload de métadonnées séparé.
- CI source du packaging : `7ff23a04dc69e6327bac611485bdb2d511417fa4`, run https://github.com/greenpower2669/VisionMaxRead/actions/runs/38635132541 — SUCCESS. Mêmes sources Android version 0.4.4-debug code 8, même clé LAB, mêmes tests verts.
- **APK Android seul (non zippé, 1 651 093 817 octets)** : https://github.com/greenpower2669/VisionMaxRead/actions/runs/38635132541/artifacts/11927379292 ; artefact nommé `VisionMaxRead-0.4.4-QWEN-SIWIS-DEBUG.apk`.
- AAB unsigned non installable (distinct) : artefact `11927274270`. Métadonnées : `11926934378`.
- Cette voie de livraison GitHub n'est ni une Release ni un merge `main`.
- La branche ajoute une correction de notices : `THIRD_PARTY_NOTICES.md` mentionne désormais Siwis CC-BY 4.0 et la version Sherpa-ONNX réelle 1.13.4.
- Reste à valider matériellement sur Samsung la qualité vocale et la lecture PaddleOCR-VL importé ; la CI ne démontre pas les performances réelles.
