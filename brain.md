# VisionMaxRead — contrat

Application Android native d’aide à la lecture française, version initiale 0.1.0. Android 8+ ; ARM64 ; cible Android 16. La caméra aide à viser ; le bouton rouge prend une photo JPEG distincte de meilleure résolution puis gèle l’affichage. Import d’image par le sélecteur Android ; texte éditable et collable ; bouton Lire/Arrêter explicite.

Reconnaissance locale : PaddleOCR PP-OCRv6 Small, SDK Android ONNX officiel adapté. Le manuscrit français reste à évaluer sur téléphone. Lecture locale : Supertonic 3, français, voix F1 par défaut, seed 2026, choix F1/M1 et vitesse dans les réglages. Aucun remplacement silencieux par une autre voix.

Historique : seulement le texte masqué par NER multilingue (PER/LOC/ORG) et règles (mail, téléphone, IBAN, identifiants, adresses et dates), dans le stockage privé sans sauvegarde cloud. Originaux photo/texte non persistés. Si l’IA de masquage échoue, aucune entrée brute n’est sauvegardée. L’utilisateur peut relire, supprimer l’historique et le vider. Aucun corpus réel dans Git, logs ou tests.

L’application ne demande pas INTERNET. Modèles téléchargés uniquement par la chaîne de construction, identifiés par révisions et SHA-256. Icône vectorielle propre au projet. Modèles et binaires générés non commis dans Git.

État initial : seul LICENSE existait au commit main 3b0b10a9dc16ef0d2e8ff3e1f9bf4b8e1e34d05e. Implémentation et compilation en cours ; aucune validation téléphone annoncée.

## Réalisation 0.1.0
Code caméra/OCR/voix/NER/historique créé sur feature/android-offline-reader-v1. 38 contrôles du noyau passent après observation de l’échec initial. Modèles verrouillés : 609 645 027 octets au total. Cette taille concerne tous les modèles ; prévoir environ 2 Go libres. Aucune validation sur téléphone pour l’instant.

## Livraison vérifiée
Build local APK/AAB réussi après corrections de revue. 47 contrôles Java, lint 0 erreur (37 avertissements), tous les SHA256 modèles vérifiés dans APK, aucune permission INTERNET, signature debug vérifiée, alignement ZIP/ELF 16 KiB. APK final 531 168 786 octets, AAB unsigned 525 254 807 octets. Compression sans perte d’un modèle, décodeur borné à 32MiB et vérifié sur ses 256 534 781 octets ; aucun poids changé. Source/API actuelle : voir docs/build-evidence.json. Deux corrections importantes validées RED→GREEN (focus audio, scopes d’historique/source). Portrait téléphone ; aucune validation matérielle annoncée.

## Contrat du prototype VMR-002 (branche d'essai)
L'accord « On test? » autorise un **prototype** de comparaison, pas le remplacement définitif du moteur OCR ni une release. Préférer une mesure instrumentée PaddleOCR/ML Kit latin **embarquée** (hors ligne) sur le **même bitmap**, suivie d'un essai distinct d'un modèle vision/langage. La version stable et les cinq mémoires projet restent canoniques. APK debug séparé sous `fr.visionmaxread.ocrtest` pour éviter d'écraser le stable ; numéro 0.2.0-test. Ne jamais présenter ML Kit OCR comme « multimodal ». Aucun modèle VLM téléchargé par l'application ; benchmark ne persiste pas le texte brut. État réel build/téléphone à vérifier via CI. Voir `docs/VMR-002-essai-ocr.md`.

## Distribution VMR-003 approuvée
Fab autorise maintenant explicitement la **préversion de test avec APK direct**, sans merge `main`. Le binaire publié doit provenir uniquement du run vert `38316437083` / SHA `53356ee7`, app ID de test `fr.visionmaxread.ocrtest`, archive source GitHub décompressée **dans GitHub Actions** (non sur le téléphone). Seul l'APK debug signé sera joint à la préversion `v0.2.0-ocr-test`. La publication est conditionnée à la preuve du fichier sur GitHub Release et à la comparaison du SHA256 après re-téléchargement. Aucune validation terrain annoncée sans retour de Fab.

## Preuve de livraison VMR-003
La préversion APK de test `v0.2.0-ocr-test` est maintenant publiée et vérifiée sur GitHub (run `38318000959` vert). L'asset direct `VisionMaxRead-0.2.0-OCR-TEST.apk` a été re-téléchargé côté GitHub, SHA-256 confirmé `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438` (539 252 854 octets). Seul l'APK test debug est distribué ; l'usage Samsung reste à valider. La version `main` n'a pas été modifiée.


## VMR-004 — Expérience audio, Qwen ultérieur (13/10/2026)
Choix utilisateur : procéder par étapes, tester une solution locale compacte avant l'expérience Qwen Omni. La branche LAB conserve Supertonic 3 par défaut et ajoute une **option explicite** de TTS Android avec voix française marquée hors ligne ; ne pas basculer en ligne ni remplacer silencieusement la voix du stable. La piste Qwen3.5-0.8B vision sera évaluée ensuite ; pas de modèle Qwen livré à ce stade. Application debug distincte `fr.visionmaxread.lab`. Ne pas confondre preuve de compilation et essai Samsung.

## VMR-005 — Extraction en temp et téléchargement direct LAB
La CI `38365535174` de la LAB v0.3.0 est **verte**, source `644d5177`. Fab souhaite que GitHub décompresse l'archive dans un répertoire temporaire et fournisse le `.apk`. Le workflow dédié opère dans `RUNNER_TEMP/VisionMaxRead-LAB` côté runner (pas sur Samsung ni dans le dépôt), puis rend le seul APK LAB téléchargeable via préversion `v0.3.0-lab-test` avec SHA256 vérifié après téléchargement. Ne pas confondre déclenchement du workflow et publication réussie, à confirmer. `main` et les packages stables restent inchangés.

### Validation de livraison VMR-005
La préversion LAB directe `v0.3.0-lab-test` est publiée et vérifiée : workflow `38394243158` success ; asset `VisionMaxRead-0.3.0-LAB.apk` (539 256 754 octets), SHA256 `dfebd355d7aba7db182a1d9ccc7d88616265f94249a562ab5ed1055144e23042`, lien https://github.com/greenpower2669/VisionMaxRead/releases/download/v0.3.0-lab-test/VisionMaxRead-0.3.0-LAB.apk. Un seul APK de laboratoire est livré. La compilation source d'origine reste `38365535174` au SHA `644d5177`. L'état "testé sur téléphone" est encore **non validé**.


## VMR-006 — Qwen OCR + Piper Jessica + orientation caméra (13/10/2026)
Autorisation explicite de Fab : « Ok code ça ». Branche exclusive `feature/qwen-piper-camera-v1`, dérivée de la LAB, sans modification de `main` et sans autorisation de Release. Qwen3.5-0.8B-VL devient l'OCR lancé automatiquement après capture/import ; PP-OCRv6-Small reste la référence et remplace le bouton ML Kit pour relire exactement le même bitmap et comparer les temps/résultats. ML Kit est retiré de cette branche.

La lecture remplace Supertonic par Sherpa-ONNX 1.13.4 + Piper `fr_FR-upmc-medium`, voix Jessica `sid=0` (Pierre `sid=1` reste seulement une référence GeckoDoku). Le moteur Piper reste chargé ; l'amorce « Ah, bon. » traverse un AudioTrack persistant à volume de piste nul, puis la vraie phrase reçoit 250 ms de silence de garde. Le diagnostic première phrase compare deux PCM d'un texte fixe sans enregistrer le texte utilisateur.

La caméra calcule désormais l'orientation relative capteur/affichage et recalcule la transformation lors d'un changement de taille de TextureView ; JPEG et ImageLoader doivent conserver la cohérence prévisualisation → photo figée → bitmap OCR. Validation Samsung encore obligatoire.

Le bundle Qwen est `Qwen3.5-0.8B-VL_int8.litertlm`, 1 303 966 384 octets, SHA-256 `5e6d69e55f92d21799fe0a2552da979c95a2533b64e4f0ed73fe87dd9d759ab9`. Il est très volumineux et est recopié au premier usage dans l'espace privé pour fournir un chemin fichier natif à LiteRT-LM. Ne pas annoncer l'APK comme prêt avant CI verte et essai matériel.


### VMR-006 — Ajustement de compatibilité et mesures (14/10/2026)
La branche utilise Sherpa-ONNX 1.13.4 avec ONNX Runtime Android 1.27.0 afin d'utiliser une dépendance réellement publiée et commune à Paddle/NER/Sherpa. L'AAR Sherpa est authentifié par SHA-256 avant retrait de sa copie native ORT afin de n'empaqueter qu'une seule `libonnxruntime.so`. L'Activity n'est plus verrouillée en portrait ; la rotation aperçu/JPEG suit la même formule capteur/affichage avec correction avant/arrière et recalcul lors d'un changement d'orientation. Le comparateur affiche aussi la PSS approximative du processus Android après Qwen puis après Paddle. Ces changements restent à qualifier par CI et test Samsung.


### VMR-006 — Dernier raffinement avant preuve finale (14/10/2026)
Le changement d'orientation est désormais traité sans recréer MainActivity (`configChanges=orientation|screenSize`) afin de conserver la session caméra pendant le passage portrait/paysage ; CameraController recalcule son transform. L'amorce TTS compare et mémorise réellement les **deux premières générations** du même texte d'amorce avant la première lecture audible, puis fait traverser la seconde au même AudioTrack en silence. Le diagnostic expose temps d'initialisation, première/dernière amorce, dernière lecture, fréquence audio et comparaison PCM initiale. Le workflow écrit maintenant taille + SHA-256 APK/AAB dans un manifeste compact. Nouvelle CI complète requise avant clôture.


### VMR-006 — Preuve CI finale (14/10/2026)

- Code validé : `cf7083b5d204626617aa17bc1515c8c910f82dc6`.
- CI : run `38505688283` **success** ; 47 contrôles contractuels ; Gradle/lint/APK/AAB success ; `verify_apk.py` confirme APK signé, ARM64, hashes modèles conformes et absence de permission INTERNET.
- APK test : `VisionMaxRead-0.4.0-QWEN-PIPER-LAB.apk` — 1 582 753 113 octets — SHA-256 `be97ff8f4b90f3f5aa6cccb9564e1d9a13f0cefab8b341a47fea05da4c3686af`.
- AAB non signé : `VisionMaxRead-0.4.0-QWEN-PIPER-LAB-unsigned.aab` — 1 333 001 356 octets — SHA-256 `977679dce7a4726f99a5f1219db3db99438002b8d694757369d768de22b680e2`.
- Artifact binaire Actions : ID `11867298469`, nom `VisionMaxRead-0.4.0-QWEN-PIPER-LAB`, expiration 28/10/2026.
- Artifact preuve légère : ID `11867068707`, `VisionMaxRead-0.4.0-QWEN-PIPER-LAB-metadata`.
- Validation Samsung réelle encore requise pour orientation dynamique, qualité OCR, RAM/PSS/chauffe, première phrase Jessica, arrêt/reprise/inactivité et mode avion.
- Aucun merge `main`, aucune Release.



## VMR-006 — pré-Release test directe publiée (2026-10-14)
- Feu vert explicite de Fab reçu pour la pré-Release de test VMR-006.
- Publication effectuée sans merge de `main`.
- Tag : `v0.4.0-qwen-piper-lab-test`.
- Source validée : `cf7083b5d204626617aa17bc1515c8c910f82dc6`.
- Build source vert : GitHub Actions `38505688283`.
- Workflow de publication : `38537482245` — succès.
- Artefact source extrait dans `RUNNER_TEMP` côté GitHub Actions.
- APK publié seul : `VisionMaxRead-0.4.0-QWEN-PIPER-LAB.apk`.
- Taille : `1582753113` octets.
- SHA-256 vérifié avant et après publication : `be97ff8f4b90f3f5aa6cccb9564e1d9a13f0cefab8b341a47fea05da4c3686af`.
- AAB unsigned non publié.
- Validation réelle Samsung encore à effectuer.


## VMR-006 — instrumentation OCR/caméra (2026-10-14)
- Branche : `feature/vmr006-ocr-camera-debug` ; `main` inchangée.
- Retour Samsung : Qwen/PP-OCR perçus comme inactifs ; aperçu caméra encore mal orienté alors que l'Activity tourne.
- PP-OCRv6-Small est désormais réellement indépendant du worker Qwen et reste utilisable même si Qwen bloque ou échoue.
- Bouton PP-OCR grisé visiblement lorsqu'il est indisponible/en cours.
- Barre de progression OCR ajoutée : progression réelle pendant la copie locale du modèle Qwen ; mode indéterminé pendant les phases sans télémétrie fiable (initialisation/inférence), pas de faux pourcentage.
- Journal debug privé ajouté : dimensions image, étapes OCR, temps, PSS processus, erreurs, paramètres caméra (sensor/display/rotation/buffer), sans texte OCR brut ni image.
- Le journal est consultable/copier/effaçable depuis Réglages > Journal debug.
- Orientation caméra : instrumentation seulement dans ce push ; aucune nouvelle formule de transform appliquée avant retour des valeurs réelles Samsung.
- Validation CI Android attendue avant APK de test.


### Validation CI instrumentation OCR/caméra
- Code validé : `331b0207009477857dd9c2bfa42034558bad2937`.
- GitHub Actions : `38558165918` — SUCCESS.
- `PASS 47 contract checks`, Gradle/lint/APK/AAB verts, contrat d’archive vert.
- `verify_apk.py` : APK signé, ARM64, hashes modèles conformes, aucune permission INTERNET.
- APK : `1582759577` octets ; SHA-256 `f2d1a7604900413507f524f27bbaccdada7c398ef28eb3d2feab0ac2dde135ba`.
- AAB unsigned : `1333003862` octets ; SHA-256 `eb570dd4bc5bf64de9e057681aa19ad17b4cc5cda8b160e47be76f5e3274d70f`.
- Artefact principal : `11892045941` ; métadonnées : `11891441218`.
- Validation fonctionnelle Samsung encore requise pour observer le journal caméra/OCR et corriger ensuite la formule d’orientation avec les valeurs réelles.


### Pré-Release directe VMR-006 DEBUG publiée
- Feu vert explicite de Fab reçu le 2026-10-14.
- Tag : `v0.4.0-qwen-piper-debug-test`.
- Source : `331b0207009477857dd9c2bfa42034558bad2937`.
- Build source : `38558165918` — SUCCESS.
- Workflow publication : `38570794104` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK exact vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.0-QWEN-PIPER-LAB.apk`.
- Taille : `1582759577` octets.
- SHA-256 : `f2d1a7604900413507f524f27bbaccdada7c398ef28eb3d2feab0ac2dde135ba`.
- `main` inchangée ; AAB unsigned non publié.


## VMR-006 — signature LAB persistante / installation Samsung (2026-10-14)
- Cause du message Android « Application non installée » : anciennes LAB et nouvelle LAB partageaient `fr.visionmaxread.lab` mais chaque runner GitHub générait sa propre clé debug.
- Correction validée sur `feature/vmr006-stable-lab-signing`.
- Nouvelle identité debug isolée : `fr.visionmaxread.labdebug`.
- `versionCode=5`, `versionName=0.4.1-debug`, label `VisionMaxRead LAB DEBUG`.
- Signature LAB persistante dédiée aux préversions/debug uniquement ; elle ne doit JAMAIS signer l'AAB release/Play.
- Empreinte certificat LAB SHA-256 de référence : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- La CI reconstruit la clé LAB test, vérifie son empreinte, puis vérifie après build package/version/certificat de l'APK.
- Code validé : `587cba2fcbb2157f364e48fcbc94a9be760832c7`.
- GitHub Actions : `38576497507` — SUCCESS ; 47 contrôles, Gradle/lint/APK/AAB, archive et verify_apk verts.
- APK : `VisionMaxRead-0.4.1-QWEN-PIPER-DEBUG.apk`, `1582759585` octets, SHA-256 `6edef1b2d4a62d415823023ee56b865fba6623eecc4fced633546d3d88c5053e`.
- AAB unsigned : `1333003863` octets, SHA-256 `afb71708069d31ca3c4962a7d3765827f09deb70c2e18c4cdbd7a8eef7cbe631`.
- Artefact principal : `11900497303`; métadonnées : `11900677088`.
- L'ancienne `fr.visionmaxread.lab` peut rester installée : la nouvelle LAB DEBUG s'installe à côté.
- Aucun merge de `main` et aucune nouvelle Release directe créée sans nouveau feu vert explicite.


### Pré-Release directe v0.4.1 DEBUG publiée
- Feu vert explicite de Fab reçu le 2026-10-14.
- Tag : `v0.4.1-qwen-piper-debug-test`.
- Source validée : `587cba2fcbb2157f364e48fcbc94a9be760832c7`.
- Build source : `38576497507` — SUCCESS.
- Workflow de publication : `38578433124` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK exact vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.1-QWEN-PIPER-DEBUG.apk`.
- Taille : `1582759585` octets.
- SHA-256 : `6edef1b2d4a62d415823023ee56b865fba6623eecc4fced633546d3d88c5053e`.
- Package : `fr.visionmaxread.labdebug` ; versionCode `5`.
- Certificat LAB SHA-256 : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- `main` inchangée ; AAB unsigned non publié.


## VMR-006 — Qwen cache LiteRT corrigé (2026-10-14)
- Retour Samsung SM-A576B Android 16 : modèle Qwen copié correctement (1 303 966 384 octets), puis échec moteur LiteRT-LM :
  `INVALID_ARGUMENT: Cache directory does not exist or is not writable: /data/user/0/fr.visionmaxread.labdebug/cache/qwen_litert_cache`.
- Cause confirmée : `QwenOcrService` passait le chemin `cacheDir` à LiteRT-LM sans créer le sous-répertoire.
- Correction : création explicite du dossier `qwen_litert_cache`, vérification `isDirectory/canWrite`, trace debug `QWEN cache ready`.
- Robustesse : si création/initialisation du moteur échoue, le moteur partiellement créé est fermé proprement et l'exception est journalisée.
- Version de test : `versionCode=6`, `versionName=0.4.2-debug`, package `fr.visionmaxread.labdebug`, même certificat LAB persistant.
- Code validé : `208032d92e682d3b9e43b621627bd7efc579cf78`.
- GitHub Actions : `38583098184` — SUCCESS.
- `PASS 47 contract checks`, Gradle/lint/APK/AAB, archive et `verify_apk.py` verts.
- APK : `VisionMaxRead-0.4.2-QWEN-PIPER-DEBUG.apk`, 1 582 759 977 octets, SHA-256 `74f6d2c71189d71214a750837034ed18c5ea7e1a081bcb3979ddf7e59f68d2a6`.
- Certificat LAB SHA-256 inchangé : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- Artefact principal : `11904645649`; métadonnées : `11903244873`.
- Valeurs caméra Samsung relevées pour la suite : back camera id=0, sensor=90°, display=0°, preview=640x480, still=4000x3000, TextureView=1012x759, transform actuel=90°, JPEG=90°.
- Correction du redressement d'aperçu caméra non incluse dans ce push ; conserver ces valeurs pour l'audit/correctif dédié.
- Aucun merge de `main` ; aucune nouvelle Release directe créée sans feu vert explicite.


### Pré-Release directe v0.4.2 DEBUG publiée
- Feu vert explicite de Fab reçu le 2026-10-14.
- Tag : `v0.4.2-qwen-piper-debug-test`.
- Source validée : `208032d92e682d3b9e43b621627bd7efc579cf78`.
- Build source : `38583098184` — SUCCESS.
- Workflow de publication : `38586381436` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK exact vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.2-QWEN-PIPER-DEBUG.apk`.
- Taille : `1582759977` octets.
- SHA-256 : `74f6d2c71189d71214a750837034ed18c5ea7e1a081bcb3979ddf7e59f68d2a6`.
- Package : `fr.visionmaxread.labdebug` ; versionCode `6`.
- Certificat LAB SHA-256 : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- Correctif principal : création/vérification explicite du cache LiteRT `qwen_litert_cache` avant initialisation du moteur.
- `main` inchangée ; AAB unsigned non publié.


## VMR-006 — Qwen cache racine + aperçu caméra Samsung (2026-10-14)
- Retour réel Samsung SM-A576B Android 16 :
  - portrait : sensor=90°, display=0°, preview 640x480, TextureView 1012x759 ;
  - paysage : display=90°, TextureView 2272x759 ;
  - ancien transform aperçu calculait 0° en paysage et l'image restait visuellement tournée ; JPEG figé restait correctement orienté.
  - Qwen continuait à lever `INVALID_ARGUMENT: Cache directory does not exist or is not writable: .../cache/qwen_litert_cache`.
- Audit de la source 0.4.2 : le correctif sous-dossier était bien présent dans le code, mais le log réel ne montrait pas la ligne `QWEN cache ready`, donc impossibilité de prouver que le binaire réellement lancé était la source attendue.
- 0.4.3-debug ajoute package/version/versionCode à la première ligne du journal pour lever toute ambiguïté de binaire.
- Qwen : abandon du sous-dossier dédié ; LiteRT-LM reçoit directement `context.cacheDir.absolutePath`, chemin Android existant/writable recommandé par l'API.
- Caméra : séparation stricte aperçu/JPEG. Le JPEG conserve la logique précédente déjà validée. La matrice TextureView suit le schéma Camera2 : portrait=0°, ROTATION_90=-90°, ROTATION_270=+90°, ROTATION_180=180°, avec scaling/cadrage indépendant de `SENSOR_ORIENTATION`.
- Branche : `feature/vmr006-qwen-camera-fix-043`.
- Version : package `fr.visionmaxread.labdebug`, versionCode `7`, versionName `0.4.3-debug`, certificat LAB persistant inchangé.
- Code validé : `79e905ab69723716eef22a14738f4df7d3e91dd1`.
- GitHub Actions : `38592842511` — SUCCESS.
- `PASS 47 contract checks`, Gradle/lint/APK/AAB, archive et `verify_apk.py` verts.
- APK : `VisionMaxRead-0.4.3-QWEN-PIPER-DEBUG.apk`, 1 582 760 261 octets, SHA-256 `7e285969c3b0fd33c2e1bb0f2acb21d92af8154727278b1c4b85c017733186f2`.
- Certificat LAB SHA-256 : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- Artefact principal : `11908775821`; métadonnées : `11908402484`.
- Validation fonctionnelle Samsung requise : la première ligne du prochain log doit annoncer `version=0.4.3-debug code=7`; Qwen doit ensuite montrer `QWEN cache ready path=/data/user/0/fr.visionmaxread.labdebug/cache ...`.
- Aucun merge de `main`; aucune Release directe 0.4.3 sans feu vert explicite.


### Pré-Release directe v0.4.3 DEBUG publiée
- Demande explicite de Fab : nouvel APK/push.
- Tag : `v0.4.3-qwen-piper-debug-test`.
- Source validée : `79e905ab69723716eef22a14738f4df7d3e91dd1`.
- Build source : `38592842511` — SUCCESS.
- Workflow de publication : `38601765196` — SUCCESS.
- Artefact extrait dans `RUNNER_TEMP`, APK vérifié avant publication puis re-téléchargé et re-vérifié après publication.
- APK seul : `VisionMaxRead-0.4.3-QWEN-PIPER-DEBUG.apk`.
- Taille : `1582760261` octets.
- SHA-256 : `7e285969c3b0fd33c2e1bb0f2acb21d92af8154727278b1c4b85c017733186f2`.
- Package : `fr.visionmaxread.labdebug` ; versionCode `7`.
- Certificat LAB SHA-256 inchangé : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- `main` inchangée ; AAB unsigned non publié.

## VMR-007 — OCR spécialisé / audio haute articulation (2026-10-14)
Feu vert de Fab pour explorer l'OCR avancé et la qualité audio, pas pour merger `main` ni publier une Release. Branche isolée `feature/vmr007-specialist-ocr-audio-hq-v1`. Implémentation LAB 0.4.4-debug code 8 : OCR Qwen principal conservé, PP-OCR secours indépendant, import d'un fichier PaddleOCR-VL-1.6 `.litertlm` via sélecteur de documents Android puis contrôle taille+SHA256 dans le stockage privé. Une seule grosse inférence LiteRT à la fois sur `qwenWorker`. Audio Siwis français haute articulation et Jessica UPMC sélectionnables ; jamais deux TTS simultanés. Modèles vérifiés côté CI, aucune permission INTERNET. Projet encore soumis à CI et tests sur Samsung ; voir `docs/VMR-007-ocr-specialise-voix-hd.md`.

### VMR-007 — preuve CI complète (14/10/2026)
- HEAD compilé et validé : `fb59d7b1a16438f1f6d264657fc865044b687fc8`.
- GitHub Actions : https://github.com/greenpower2669/VisionMaxRead/actions/runs/38632764998 — SUCCESS.
- 47 contrôles contractuels PASS ; Gradle/lint/APK/AAB, archive, verify_apk verts.
- APK test : `VisionMaxRead-0.4.4-QWEN-SIWIS-DEBUG.apk`, 1 651 093 817 octets, SHA-256 `644e77ff4a162bb980793c55191904772f7333ef7349104846f53ae5ce422dce`.
- Package `fr.visionmaxread.labdebug`, versionName `0.4.4-debug`, versionCode `8`, ARM64, aucune permission INTERNET.
- Certificat LAB SHA256 inchangé : `2004c354038a441292e861ff1513391d26d3dfc011289dea16dee1d72f145182`.
- AAB unsigned non publié : 1 400 474 764 octets, SHA-256 `d14062ef0ef64952588d8608f1dd7d677b92b0e771f738258369a39bbd0ac1c5`.
- Artefact GitHub Actions binaire : `11926398102` ; métadonnées : `11926628152`.
- Modèle additionnel PaddleOCR-VL-1.6 non intégré à l'APK : import manuellement via SAF + SHA256, format .litertlm (1 390 305 472 octets, `314df8b34d6911990d488e825602550a5bbcc5d6700cb1a8914e853e9043f4f1`).
- CI ≠ validation Samsung : test matériel Qwen/cache/caméra, import OCR, comparatif précision, Siwis/Jessica, RAM/température toujours nécessaires.
- Ne pas merger `main` ni créer de Release sans autorisation expresse.

### VMR-007 — livraison APK direct, sans ZIP (14/10/2026)
- Changement GitHub Actions : `actions/upload-artifact@v7`, `archive: false` pour l'APK unique et pour l'AAB unsigned dans deux artefacts distincts ; upload de métadonnées séparé.
- CI source du packaging : `7ff23a04dc69e6327bac611485bdb2d511417fa4`, run https://github.com/greenpower2669/VisionMaxRead/actions/runs/38635132541 — SUCCESS. Mêmes sources Android version 0.4.4-debug code 8, même clé LAB, mêmes tests verts.
- **APK Android seul (non zippé, 1 651 093 817 octets)** : https://github.com/greenpower2669/VisionMaxRead/actions/runs/38635132541/artifacts/11927379292 ; artefact nommé `VisionMaxRead-0.4.4-QWEN-SIWIS-DEBUG.apk`.
- AAB unsigned non installable (distinct) : artefact `11927274270`. Métadonnées : `11926934378`.
- Cette voie de livraison GitHub n'est ni une Release ni un merge `main`.
- La branche ajoute une correction de notices : `THIRD_PARTY_NOTICES.md` mentionne désormais Siwis CC-BY 4.0 et la version Sherpa-ONNX réelle 1.13.4.
- Reste à valider matériellement sur Samsung la qualité vocale et la lecture PaddleOCR-VL importé ; la CI ne démontre pas les performances réelles.
