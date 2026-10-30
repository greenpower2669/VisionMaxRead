# VisionMaxRead LAB 0.6.3 — HobbyLM en direct

**VMR-015 expérimental** : HobbyLM-1B est interrogé avec son chat template officiel `<|user|>` / `<|assistant|>`, sans hypothèse Liquid ajoutée. La génération **réellement produite jeton par jeton par le moteur Rust** apparaît dans une fenêtre accessible avec compteur, bouton **Arrêter** et **Copier flux** après consentement. Une réponse rejetée pour incohérence de nombres ne peut plus être sélectionnée comme texte final ; Indra reste intact. Le flux n'est pas un canal caché de raisonnement. Voir [protocole et limites VMR-015](docs/VMR-015-moe-live-token-stream.md).

**Aucune amélioration de qualité encore démontrée sur Samsung** : il faut tester l'APK sur le même manuscrit. Le laboratoire reste **sans Qwen, entièrement hors ligne**, avec Indra, OrderNet, LM, Liquid en option, vrai sparse MoE HobbyLM, Siwis/Jessica et tous les boutons de copie des passes. Aucune fusion main, aucune Release sans validation.

---

# VisionMaxRead VMR-014 — premier vrai MoE sur Android (EXPÉRIMENTAL)

**À ne pas confondre avec la LAB 0.6.1, plus stable, qui ajoute déjà tous les boutons de copie.** VMR-014 `feature/vmr014-hobbylm-moe-android` tente un **véritable réseau MoE HobbyLM-1B**, indépendant de Liquid, avec 64 experts routés et 8 activés par jeton. C'est une *alternative* au Meta MobileMoE-S-QAT (gated 401, licences de recherche non commerciale), pas une version Meta.

Le bouton **Tester vrai MoE · HobbyLM (hors ligne)** tente une correction des 460 premiers caractères OCR Indra. Le reste demeure inchangé ; les chiffres sont comparés et l'utilisateur choisit explicitement toute proposition à utiliser. Les appels Indra, Liquid et MoE sont sérialisés, aucun texte personnel n'est envoyé en ligne. Boutons permanents après OCR : **Copier logs techniques**, **Copier résultats · toutes les passes** et **Copier une passe** depuis le rapport. Aucun besoin de screenshots, les logs s'envoient par copier-coller. Les textes sont potentiellement sensibles : anonymiser avant partage.

**Modèle expérimental ~729 Mo + stockage et mémoire de travail supplémentaires.** LLM entraîné surtout en anglais, aucun gain de fidélité français démontré. La CI vérifie compilation, signatures, hashes et licence, mais seul le Samsung vérifie la possibilité réelle de charger le modèle Rust à 1B et la vitesse. En cas de défaillance, garder Indra et utiliser la LAB 0.6.1 comme secours. Voir [documentation détaillée VMR-014](docs/VMR-014-hobbylm-real-moe.md), [VMR-013 boutons de copie](docs/VMR-013-copy-logs-moe.md).

---

# VisionMaxRead 0.6.0 LAB — Indra + LiquidAI hors ligne

**VMR-012, branche `feature/vmr012-liquid-semantic-experts`.** Version expérimentale sans Qwen. Le lecteur conserve Indra brut, OrderNet, XY-cut, LM Indra, PP-OCR et la synthèse Siwis/Jessica. Nouveau bouton opt-in **Tester Liquid + experts (hors ligne)** après capture : un vrai modèle `LiquidAI/LFM2.5-230M-QAD-Q4_0.gguf` (GGUF quantifié Q4) s'exécute via `llama.cpp` JNI CPU ARM64, téléchargé et verrouillé uniquement à la compilation de l'APK. Le téléphone n'a pas besoin d'Internet.

**Passes du test :** P3 Liquid propose un texte ; P4A/P4B font deux contrôles spécialisés par le **même Liquid** ; P5 arbitre sous contrainte de conservation des chiffres. Deux premiers blocs de ~650 caractères maximum seulement pour commencer ; le reste est conservé original. Aucune correction n'est appliquée automatiquement au document ; le bouton Rapport garde les preuves (textes seulement dans le rapport sensible confirmé). **Meta MobileMoE-S n'est pas inclus** : les sous-passes ne sont pas un vrai MoE neuronal. La qualité de correction, la mémoire et la vitesse ne sont **pas encore validées sur Samsung**.

Modèles et licences : poids LFM Open License v1.0 (termes commerciaux et attribution), code llama.cpp MIT, Indra OCR poids OpenRAIL-M modifiée. Voir [l'architecture et le protocole VMR-012](docs/VMR-012-liquid-offline-multiexperts.md) et [les notices de tiers](THIRD_PARTY_NOTICES.md). Pas de fusion `main`, pas de Release sans accord.

---

# VisionMaxRead 0.5.1 LAB — Indra OCR + correction linguistique et comparaison colonnes

**VMR-011 expérimental (sans Qwen).** Indra reconnait la photo, son modèle n-gramme latin `lm.npz` réexamine les lignes incertaines par beam CTC, OrderNet recompose les blocs et XY-cut fournit un autre ordre de lecture. Bouton **Passes Indra · rapport** → **Comparer les ordres** : visualiser/choisir OrderNet+LM, XY-cut+LM et les deux versions sans LM. Les textes d'origine restent consultables dans le rapport. Mode avion, voix Siwis/Jessica et secours PP-OCR conservés.

L'essai de magazine remonté par Fab a motivé ces correctifs ; **aucune précision de lecture VMR-011 encore prouvée sur Samsung**. Le journal standard ne garde que les métriques ; le rapport complet avec textes/boîtes est copié uniquement avec deux confirmations. Les moteurs neuronaux **Liquid et MobileMoE restent non implémentés sur Android** : ils apparaissent en `unavailable`, jamais comme des corrections réalisées. La licence des poids Indra est modifiée OpenRAIL-M. Voir [VMR-011](docs/VMR-011-layout-language-model.md).

---

# VisionMaxRead LAB 0.5.0 — Indra + OrderNet, sans Qwen

**VMR-010 expérimental, branche `feature/vmr010-indra-liquid-moe-review`.** Reconnaissance primaire hors ligne : Indra latin (det/rec) avec ordre de lecture `order.onnx` et repli XY-cut pour les colonnes ; PP-OCRv6-Small conserve un bouton de secours. Caméra, corrections manuelles, synthèse Siwis/Jessica et masquage d'historique sont conservés. Le poids Qwen (~1,3 Go) et la dépendance LiteRT-LM sont retirés ; le fichier Qwen privé d'une ancienne LAB est nettoyé précisément au démarrage, sans effacement de l'historique.

**Rapports de passes :** bouton « Passes Indra · rapport » après traitement ; P1 OCR brut, P2 ordre, P2B modèle linguistique, P3 Liquid, P4 MobileMoE, P5 choix final. Les étapes non portées apparaissent **indisponibles**, sans résultat fictif ; seuls P1/P2 et l'arbitrage conservateur s'exécutent dans cette LAB. Le journal standard stocke **durées, longueur, statut, PSS** sans texte privé. Un rapport comprenant les textes peut être copié après une double confirmation explicite ; relire/anonymiser avant toute transmission.

**LiquidAI et MobileMoE ne sont pas encore des moteurs Android exécutables ici.** Le banc desktop facultatif `scripts/mobilemoe_benchmark.py` est prévu pour de vrais essais avec modèles installés localement. Voir [architecture et limites](docs/VMR-010-indra-liquid-moe-architecture.md) et [todo](todo.md). La compilation CI ne prouve pas encore la précision sur Samsung. Aucune fusion `main`, aucune Release automatique.

---

# VisionMaxRead — branche expérimentale Indra OCR VMR-009

**LAB Android 0.4.6-indra-debug / code 10 (non validé sur Samsung)** : cette branche, issue de VMR-008, conserve l'OCR automatique Qwen3.5, le bouton PP-OCRv6-Small, le module spécialisé optionnel et la voix Siwis/Jessica. Elle ajoute **« Indra OCR (latin) / Comparer »**, qui reconnaît la même photo via un portage Android expérimental d'Indra (`det.onnx` + `rec.onnx`, ONNX Runtime CPU). Résultats à lire et à corriger, aucune reconnaissance en ligne ; mesure de la latence et de la PSS totale du processus. Le portage n'intègre pas encore les langues non latines, la détection d'orientation, le modèle de langage ou la mise en page d'Indra Python. Il n'a pas encore passé une CI complète ni un test téléphone.

Construire : `python3 scripts/prepare_indra_models.py`, `python3 scripts/prepare_models.py`, puis Gradle et `python3 scripts/verify_apk.py`. Les poids Indra sont téléchargés au **build**, pas depuis le téléphone. Modèles `det` et `rec` épinglés SHA256 ; fichiers de licence inclus. Cf. [VMR-009](docs/VMR-009-indra-ocr-latin.md). Aucune fusion `main` ou publication Release autorisée pour cet essai.

---

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
