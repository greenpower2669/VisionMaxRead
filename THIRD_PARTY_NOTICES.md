# VMR-012 — Licences des composants réellement présents dans la LAB

**Version expérimentale `0.6.0-liquid-experts-lab`.** Les lignes VMR-007 ci-dessous documentent les anciennes variantes et ne constituent **pas** l'inventaire du paquet VMR-012. **Qwen, LiteRT-LM et PaddleOCR-VL ne sont pas distribués ou exécutés dans VMR-012**.

- **Indra OCR** : source Datalab Apache-2.0 ; poids sous licence OpenRAIL-M modifiée, copie incluse dans `assets/indra/LICENSE`. Les modèles `det.onnx`, `rec.onnx`, `order.onnx` et `lm.npz` sont authentifiés au build.
- **Liquid AI LFM2.5-230M QAD Q4_0** : poids `LiquidAI/LFM2.5-230M-GGUF`, LFM Open License v1.0 (https://www.liquid.ai/lfm-license). Copie exacte de `LICENSE` distribuée dans `assets/liquid/LICENSE` ; SHA256 des poids vérifié : `e75f83268de11b2a1bcfab5f3b5c5c0c97569ddbbc0990aad88437e45b8ba292`. La licence LFM impose des conditions de redistribution et une limite de revenus pour certains usages commerciaux : vérifier les conditions avant publication générale.
- **ggml-org/llama.cpp** : code source MIT, compilation statique du commit `36a0e2ffc0b352b774f8d71c43668b75b3061ef5`, copie de sa licence source `assets/liquid/LLAMA_CPP_LICENSE`. Seul le moteur CPU ARM64 est embarqué ; aucun service réseau.
- **Meta MobileMoE-S-QAT** : **aucun poids ni code incorporés**. Modèle FAIR Non-Commercial Research, architecture non disponible via le runtime mobile employé ici. Les étapes dites « experts » dans VMR-012 sont plusieurs tâches du même modèle Liquid et ne constituent **pas** un MobileMoE indépendant.
- **PP-OCR, Sherpa-ONNX, Piper Siwis/Jessica, ONNX Runtime/OpenCV, BERT NER** : licences et avis historiques détaillés plus bas, toujours applicables à leurs composants présents.

---

# Components included in VisionMaxRead — VMR-007 LAB

- PaddleOCR Android SDK and PP-OCRv6 Small detection/recognition, Apache-2.0. The adapted SDK remains in `ppocr-sdk/`; exact model revisions and hashes are in `models.lock.json`.
- Qwen3.5-0.8B-VL LiteRT-LM bundle from `litert-community/Qwen3.5-0.8B`, Apache-2.0. CI validates the 1 303 966 384-byte file with SHA-256 `5e6d69e55f92d21799fe0a2552da979c95a2533b64e4f0ed73fe87dd9d759ab9`.
- LiteRT-LM Android 0.18.0, Apache-2.0, used for local Qwen and optionally an imported specialist LiteRT model.
- Sherpa-ONNX Android 1.13.4 and its Piper/VITS integration. The AAR is fetched by CI and SHA-256 checked.
- Piper `fr_FR-upmc-medium`: French, two speakers, 22,050 Hz; VisionMaxRead selects Jessica (speaker 0). Piper voice packaging/model is MIT; its UPMC model card identifies CC BY-SA 4.0 training data. Preserve attribution when redistributing the voice.
- Piper `fr_FR-siwis-medium`: French female voice, 22,050 Hz, original model and its dataset identified in https://huggingface.co/rhasspy/piper-voices/blob/main/fr/fr_FR/siwis/medium/MODEL_CARD ; Siwis dataset CC-BY 4.0 (University of Edinburgh), voice model MIT repository listing. Attribution to the dataset and voice authors must be preserved. Official Sherpa-ONNX packaged archive hash SHA-256: `375909aa30842b3a4efa10b1beb1d761af792960ae6873b4d53889f96c66195b`.
- PaddleOCR-VL-1.6 specialist model is not bundled/distributed in the APK; the user may import their independently obtained file (`litert-community/PaddleOCR-VL-1.6`, Apache-2.0) through Android's file picker. Its SHA-256 is checked by the app.
- Multilingual BERT NER (Davlan/Xenova conversion) for local masking; see `models.lock.json` and bundled model card/license.
- ONNX Runtime/OpenCV and AndroidX/Kotlin dependencies under their respective upstream licenses.

VMR-007 does not package or execute ML Kit, Supertonic 3, or the Android system-TTS prototype. No user document is used for training. The application is required to remain without `android.permission.INTERNET`; CI verifies the compiled APK, not merely the source manifest.

## Indra OCR — essai VMR-009 (latin uniquement)
Source : https://github.com/datalab-to/indra (code Apache-2.0).
Poids : https://huggingface.co/datalab-to/indra-ocr (licence OpenRAIL-M modifiée, conditions d'utilisation commerciales particulières).
Révision des modèles : bd02167fdf2005a19962cc15188f8496a2c87d3b.
L'APK d'essai embarque le texte original de licence dans assets/indra/LICENSE. Portage Android expérimental du détecteur et du reconnaisseur latin, non affilié à Datalab.
