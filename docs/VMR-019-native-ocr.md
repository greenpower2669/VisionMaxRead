# VMR-019 — OCR spécialisés, intégration native expérimentale (30 octobre 2026)

## État exact
Branche isolée `feature/vmr019-native-ocr-surya-lighton` dérivée de VMR-018. Deux chemins **d'exécution réelle d'image** codés pour Surya 2 et LightOnOCR-3 via llama.cpp multimodal. Ce n'est pas une simple liste d'options, mais leur succès dépend du build NDK et surtout d'un essai Samsung après import des deux poids. Tant que la CI ou le téléphone ne l'ont pas confirmé, état : **intégration en validation, pas OCR opérationnel confirmé**.

## Sources amont et intégrité
- llama.cpp CLI multimodal, révision **792fcf4e7501972b595d3aeeb85153f9926f6aef**, code https://github.com/ggml-org/llama.cpp/tree/792fcf4e7501972b595d3aeeb85153f9926f6aef/tools/mtmd
- LightOnOCR-3 0.8B GGUF Q4 (542 MB) https://huggingface.co/staghado/LightOnOCR-3-0.8B-GGUF/blob/main/model-Q4_K_M.gguf SHA256 bb57c9769af18d02f0bde4dd0840737cae170d7340cc0435eddcc36b443459df ; vision projector F16 (205 MB) SHA 101acc9466a587a615bbf7b29c0ecb6744e309363eba8b615fe3e09f5e1c5d5f. Licence Apache-2.0.
- Surya OCR 2 GGUF (1,27 GB) https://huggingface.co/datalab-to/surya-ocr-2-gguf/blob/main/surya-2.gguf SHA256 1f18abe17b1ed8b4e47ee9b1ad0e274c93daf5efbb6b29a04ff1712e37051e05 ; vision projector (205 MB) SHA 98c0563673b1657ff6d021d1e5f04af06cbf61bb40c63ac613e8bb71b42fb2c0. Licence des poids OpenRAIL-M modifiée, conditions particulières.
- Nemotron 3.5 Parse : C-RADIO/NaViT et mBART, pas une architecture GGUF acceptée ici ; portage séparé à faire.

## Architecture
1. L'utilisateur télécharge dans son navigateur les deux fichiers amont ; VisionMaxRead **n'a pas de permission INTERNET**.
2. Import explicite via Android SAF ; vérification en streaming SHA256 + magic GGUF + limite taille, copie temporaire dans le stockage privé puis renommage atomique.
3. À la demande, Image Bitmap déjà capturée → JPEG privé temporaire → subprocess Android ARM64 `libvmr_mtmd_cli.so` via libmtmd, `-m`, `--mmproj`, `--image`, prompt OCR propre au modèle, CPU-only, max 8 min, sortie temporaire en cache.
4. **Avant exécution**, recalcul SHA sur les deux GGUF. stdout lu seulement en cas de code de sortie 0 et borné à 500 kB ; stderr, image et stdout éliminés en fin de tâche. Aucun texte OCR n'est journalisé ; le résultat passe dans une fenêtre de comparaison et n'écrase pas Indra.
5. Arrêt utilisateur et changement de document : interruption du processus ; pas d'inférence sur UI.

## Vérifications restantes
- [ ] CI compilation CMake Android (c++_static, sans réseau d'inférence) et binaire ELF inclus dans l'APK, vérification version/certificat, manifest sans INTERNET.
- [ ] Test Samsung hors ligne : vraies sorties Surya et LightOn, isolation processus, permission d'exécution du binaire packagé, résultats avec image de 1942.
- [ ] Mesures PSS/RSS, latence, batterie, erreurs de troncature, contamination stdout, CER/WER et contrôle utilisateur.
- [ ] Nemotron 3.5 Parse : portage ONNX propre avec tokenizer pré/posttraitement, absence d'options factices.
- [ ] Aucune Release ni merge main sans accord de Fab.

L'intégration libmtmd est un **essai de runtime**, pas une preuve que ses modèles sont validés sur Samsung.
