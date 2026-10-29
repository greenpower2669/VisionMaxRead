# VisionMaxRead VMR-012 — LiquidAI + relecture multi-experts, hors ligne

Date : 29/10/2026. Source : branche `feature/vmr012-liquid-semantic-experts`, héritée de VMR-011. **Pas de merge main, pas de Release publique sans feu vert.**

## Version Android construite
- Indra `det.onnx` et `rec.onnx` → reconnaissance brute ; OrderNet → ordre des colonnes ; correcteur n-gramme Indra `lm.npz` → suggestions CTC ; XY-cut reste comparable. Aucune régression intentionnelle du lecteur Siwis/Jessica, historique masqué ni du PP-OCR.
- Nouveau bouton **Tester Liquid + experts (hors ligne)**, visible après une lecture Indra réussie : **pas d'inférence neuronale automatique sur chaque capture**.
- LFM2.5-230M GGUF **QAD Q4_0 quantifié**, environ 149 Mo, modèle préchargé depuis les assets de l'APK. Aucune permission INTERNET ; runtime `libvmr_liquid.so` ARM64 JNI, compilé en CI depuis un commit précis de llama.cpp avec CPU 2 threads.
- Chargement/fichier `LFM2.5-230M-QAD-Q4_0.gguf` verrouillé SHA256 `e75f83268de11b2a1bcfab5f3b5c5c0c97569ddbbc0990aad88437e45b8ba292`. Le premier chargement copie le modèle dans `noBackupFilesDir` avec vérification SHA ; attention espace disque requis pour APK + modèle copié. Aucun téléchargement depuis l'application.
- Les **deux premiers segments de 650 caractères** sont traités pour limiter le coût CPU avant validation Samsung. Le reste du document est **recopié inchangé**. Les résultats peuvent être plus lents que l'OCR (~0,7s observé sur VMR-010), temps exact à mesurer sur téléphone.
- Pour chaque segment : (P3) véritable génération Liquid, (P4A) revue linguistique par le même Liquid sous consignes distinctes, (P4B) revue de dates/noms/chiffres par le même Liquid, (P5) sélection Liquid A=OCR/B=proposition. Une garde déterministe **interdit le vote B lorsque la séquence des nombres change** et borne les longueurs. Aucun résultat sémantique ne remplace le texte automatiquement ; bouton explicite pour voir et choisir une proposition.
- Tous les résultats réels sont consignés dans `OcrTrace` **en mémoire uniquement** ; le journal standard ne contient que statut, durée, caractères, PSS et flags. Rapport complet avec texte, seulement sur confirmation manuelle, à anonymiser avant transmission.
- Erreur native, manque de mémoire ou annulation : retour au texte Indra. L'app s'abstient d'annoncer un succès neuronal si l'appel a échoué. Arrêt de calcul sur remplacement de document/destruction.
- Un nom spécial **P4_MOBILE_MOE** reste marqué `unavailable` : cette version **n'intègre pas** le MobileMoE-S-QAT de Meta. Des prompts spécialisés successifs sur Liquid sont une **chaîne multi-experts**, pas une architecture mixture-of-experts à réseaux distincts et routeur appris. Licence Meta FAIR non-commerciale, anglais, et absence de portage/quantification compatible avec l'APK actuel.
- licences : code llama.cpp MIT statiquement lié (copie de licence source dans `assets/liquid/LLAMA_CPP_LICENSE`), modèle Liquid LFM Open License v1.0 (copie dans `assets/liquid/LICENSE`). Les conditions commerciales du modèle ne sont pas celles de MIT ; consulter `THIRD_PARTY_NOTICES.md`.

## Test demandé à Fab
1. Installer la nouvelle LAB après compilation CI complète. Ne pas désinstaller l'ancienne LAB si conservation souhaitée ; package LAB `fr.visionmaxread.labdebug` identique, versionCode 13, certificat LAB stable.
2. En mode avion, importer **la même lettre manuscrite de 1942** et une page de magazine à plusieurs colonnes ; conserver les photos sans partage forcé.
3. Cliquer « Tester Liquid + experts (hors ligne) » après Indra, attendre le résultat ; comparer les blocs traités à la photo et aux versions OrderNet / XY-cut / Indra LM. Vérifier nombre de caractères et chiffres/numéros de pensions/noms historiques.
4. Depuis « Passes Indra · rapport », examiner `P3_LIQUID_n`, `P4A_LINGUISTE_n`, `P4B_FACTS_n`, `P5_LIQUID_ARBITRE_n` puis transférer seulement un **rapport relu et anonymisé** pour analyse conjointe.
5. Mesurer latence, PSS, chauffe, battery drain, annulation, audio Siwis et reproduction de l'essai en mode avion.
6. Vérifier que le modèle Meta n'est jamais présenté comme exécuté ; **tout gain de fidélité sémantique reste à démontrer sur corpus**.

## Contrôles
- `scripts/prepare_liquid.py` : poids + licence officiels et SHA de poids vérifié ; `scripts/build_liquid_android.sh` : source llama.cpp au SHA `36a0e2ffc0b352b774f8d71c43668b75b3061ef5`, NDK Android 27.2, bibliothèque CPU ; `scripts/verify_apk.py` : signature, Qwen absent, zéro permission INTERNET, assets Indra et Liquid, licence, bibliothèque native.
- Les runs CI ne constituent **pas** une preuve d'inférence réelle sur le Samsung : le runtime natif peut encore révéler une erreur à l'initialisation ou à la génération qui n'est visible qu'en test matériel.
- Une revue légale doit précéder toute publication générale de l'APK, compte tenu des licences des poids Indra, du modèle Liquid et des voix.
