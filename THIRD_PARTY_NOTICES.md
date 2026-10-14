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
