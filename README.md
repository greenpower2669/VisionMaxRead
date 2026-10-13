> **LAB VMR-006 — branche `feature/qwen-piper-camera-v1`** : Qwen3.5-0.8B-VL est l'OCR automatique ; **PP-OCRv6-Small** remplace ML Kit comme référence/comparateur sur le même bitmap ; la voix est **Jessica (Piper/Sherpa-ONNX, sid 0)** avec amorce silencieuse ; l'orientation Camera2 utilise maintenant capteur + Display. Cette LAB 0.4.0 n'est ni fusionnée sur `main` ni publiée. Le modèle Qwen fait ~1,30 Go et impose une validation de stockage/RAM sur téléphone. État de build : correction Kotlin 2.4 en cours après un premier échec CI avant APK.

# VisionMaxRead

> **Branche expérimentale VMR-002 (0.2.0-test)** : application de test distincte `VisionMaxRead TEST OCR` / `fr.visionmaxread.ocrtest`, non fusionnée sur main. Après capture/import d'une photo, bouton **Comparer les OCR (test)** : PaddleOCR contre ML Kit latin (modèle embarqué) sur les mêmes pixels, avec temps et textes. Ce **n'est pas un modèle multimodal** ; l'essai Gemma est séparé. Protocole : [VMR-002](docs/VMR-002-essai-ocr.md). L'APK de test est disponible **directement**, sans ZIP, via la [préversion v0.2.0-ocr-test](https://github.com/greenpower2669/VisionMaxRead/releases/tag/v0.2.0-ocr-test). Le ZIP technique GitHub Actions contient aussi un AAB unsigned à ne pas publier sur Play. Compter un espace supplémentaire pour cette installation à côté du stable. Mode avion après installation. Pas de transmission réseau.

Lecteur Android natif en français, avec OCR, voix et masquage **sur le téléphone**. Android 8+ (API 26), ARM64 ; première cible : téléphone Samsung SM-A576B. Version 0.1.0, APK de test signé debug.

## Utilisation

1. **Caméra** : l’aperçu léger sert à viser. Le bouton rouge **Figer l’image** prend une photo à meilleure résolution et lance l’OCR.
2. **Photo** : sélectionne une image du téléphone. Rotation EXIF et taille de décodage prises en compte.
3. Corrige le texte affiché, puis **Lire**. Le même bouton devient **Arrêter**. **A−/A+** changent la taille du texte.
4. Tu peux aussi coller du texte, partager une image/du texte vers VisionMaxRead, ou utiliser l’action Android sur un texte sélectionné.
5. Les textes reconnus sont conservés après masquage si l’historique automatique est activé. **Garder masqué** enregistre un texte saisi/collé. **Voir le masquage** prépare un aperçu modifiable avant copie/partage.

Aucune permission INTERNET. Modèles inclus : PaddleOCR PP-OCRv6 Small, Supertonic 3 (F1/M1), BERT NER multilingue quantifié. Les modèles pèsent 610 Mo ; l’APK complet environ 531 Mo. Prévoir environ 2 Go libres : certains modèles sont copiés dans l’espace privé lors de leur première utilisation. Le premier traitement peut être lent. Le sélecteur de photos Android peut proposer des images cloud : choisis une image réellement disponible sur le téléphone pour l’usage hors ligne.

La lecture utilise le texte original affiché. Images et textes originaux ne sont pas enregistrés par l’application. L’historique conserve uniquement la sortie du masquage local (noms, lieux, organisations, dates et coordonnées). Si le moteur échoue, rien n’est enregistré. Pas de télémétrie, sauvegarde Android désactivée, historique dans le dossier privé sans backup, maximum 100 entrées.

**Le masquage automatique peut manquer des informations identifiantes.** Relis et corrige avant partage. Le manuscrit cursif français, les colonnes et les photos floues ne sont pas garantis. Pas d’import de fichier vidéo dans cette première version : « vidéo » désigne l’aperçu caméra.

## Construire

Installer JDK 17, Android SDK (platform android-36 et build-tools 36.0.0), Python 3.10+. Définir ANDROID_HOME ou local.properties. Les téléchargements se font uniquement lors de la préparation/construction.

```sh
python3 scripts/prepare_models.py
./gradlew :core:check :app:lintDebug :app:assembleDebug :app:bundleRelease
bash scripts/test-archive.sh
python3 scripts/verify_apk.py app/build/outputs/apk/debug/app-debug.apk
```

Les 18 assets sont verrouillés par révision immuable et SHA256 dans models.lock.json. Le build refuse les fichiers absents, corrompus ou imprévus. Le gros modèle vocal est emballé sans perte (octets séparés par blocs puis XZ) : le décodeur Java rétablit le SHA256 ONNX d’origine, avec mémoire de décompression limitée à 32 MiB. Les modèles utilisent les mêmes poids ; leur première extraction demande un peu de CPU. Ils ne sont pas suivis par Git. Le SDK officiel PaddleOCR est conservé dans ppocr-sdk avec sa provenance dans THIRD_PARTY_NOTICES.md ; licences et notices incluses dans l’APK.

APK : app/build/outputs/apk/debug/app-debug.apk, signé avec la clé de test Android. AAB : app/build/outputs/bundle/release/app-release.aab, **non signé**, nécessite la clé d’upload pour publication. Changer la clé APK demandera une désinstallation/réinstallation ; l’historique privé sera alors perdu. Aucun déploiement ni fusion automatique.

## Vérifications et limites

- 47 contrôles Java : masquage/chevauchement/offsets Unicode, découpage sans perte, tokenizer cased, annulation audio, indépendance caméra/document/historique.
- Test réel des modèles sur CPU desktop : ligne française imprimée reconnue exactement (accents), détection sur page synthétique, entités françaises PER/LOC, signal vocal Supertonic fini et non nul.
- Compilation APK/AAB et analyse statique Android ; vérification des hashes dans l’APK, signature, restauration exacte du modèle compressé, ARM64 et absence de permission INTERNET.

Pour reproduire les essais des modèles : installer scripts/smoke-requirements.txt puis exécuter python3 scripts/smoke_models.py (police DejaVu Sans nécessaire).

À vérifier sur téléphone en mode avion et avec grande police : autorisation caméra et reprise, netteté/orientation, import galerie, OCR réel/manuscrit, prononciation française et volume, arrêt pendant préparation/lecture, appel ou perte de focus audio, historique après effacement et désactivation/réactivation, temps et mémoire. L’essai desktop ne prouve pas ces comportements matériels.

Restent pour une évolution : suppression individuelle d’une entrée (tout effacer disponible) et orientation des tablettes dont l’affichage naturel est paysage. Voir todo.md et les cinq mémoires à la racine.
