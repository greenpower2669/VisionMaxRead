# Components included in VisionMaxRead

- PaddleOCR Android SDK, Apache-2.0, vendored from PaddlePaddle/PaddleOCR at `dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf`; local changes: build/dependency pins and the synchronous application adapter. Model PP-OCRv6 Small detection/recognition, Apache-2.0. Model revisions and hashes: models.lock.json.
- Supertonic 3: four ONNX models and F1/M1 styles, BigScience Open RAIL-M (model use restrictions retained). Android inference port derived from Supertone official Java helper, MIT. Upstream: supertone-oss-archive/supertonic and supertone-oss-archive/supertonic-3.
- Multilingual BERT NER, Davlan/bert-base-multilingual-cased-ner-hrl, AFL-3.0; quantized ONNX conversion by Xenova. French supported; news-trained model may miss entities in personal documents. Model card included.
- ONNX Runtime 1.23.2, MIT; OpenCV 4.12.0, Apache-2.0.
- AndroidX Core, ExifInterface, desugar_jdk_libs, Apache-2.0; Kotlin and kotlinx.coroutines, Apache-2.0.

License texts are bundled under app/src/main/assets/licenses. Original SDK copyright headers retained. Source repository is public; changes to the NER inference integration are available here. No training on user documents occurs.

- XZ for Java 1.10, 0BSD (Tukaani), used only to decompress losslessly packed model assets. Original ONNX payloads and SHA256 remain identical. Native libraries are losslessly ZIP-compressed and extracted by Android.
