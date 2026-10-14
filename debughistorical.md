# Historique de diagnostic

## VMR-D001 — Initialisation, 11 octobre 2026
Dépôt vierge de code, seul LICENSE présent. Aucun bug applicatif préexistant à corriger. La taille annoncée de 31 Mo concerne seulement les modèles OCR Small, sans runtime ni voix ni NER. Reconnaissance du manuscrit français non validée. L’OCR photographique et la reconnaissance des traits du stylet sont des fonctions différentes.

Ne jamais annoncer une APK ou une reconnaissance téléphone validée sans résultat de construction ou test réel. Ne jamais sauvegarder le texte brut après un échec du masquage.

## VMR-D002 — Construction locale
SDK/Gradle Java ne reprennent pas les variables proxy de cette session. Téléchargements Python officiels réussis. SDK installé depuis repository2-3.xml ; Gradle utilise les propriétés JVM du proxy local, hors dépôt.

## VMR-D003 — SDK CI et versions
Première CI arrêtée dans setup-android, paquet `tools` retiré, avant compilation. Utiliser SDK préinstallé du runner et demander seulement android-36/build-tools 36.0.0. AGP prend 35 par défaut : version 36 explicitée. Modèles exécutés sur CPU desktop : OCR imprimé français exact, PER/LOC repérés, audio Supertonic fini/non nul. Caméra/audio Android à tester.

## VMR-D004 — Revue et livraison
Perte de focus : annulation de l’opération vocale, contrôles avant étapes/écriture audio, retour UI Lire. Historique : séparer les générations source/document/confidentialité, pour conserver texte/caméra lors d’un effacement ou changement de préférence. Reproductions RED→GREEN ; 47 checks verts. Build final réussi ; 0 erreur lint ; APK signé et 18 modèles vérifiés ; alignements 16 KiB. Restent essai Samsung, suppression individuelle et tablettes paysage.

## VMR-D005 — Taille et empaquetage
Limite de livraison 512 MiB : premier fichier refusé avant transfert. Deux fichiers temporaires de téléchargement accidentellement présents dans les assets ont été détectés par un nouveau contrôle de liste exacte (RED→GREEN). Compression ZIP standard encore trop volumineuse ; un seul gros modèle est emballé sans perte (blocs de 64KiB, séparation des octets, XZ). Décodeur Java borné à 32MiB, vérifié sur le modèle entier 256 534 781 octets et son SHA256 d’origine ; archives tronquées refusées. Aucun poids quantifié/modifié pour réduire la voix.
Construction incrémentale locale : D8 voit des classes transitoires du système de synchronisation dans les sorties javac. Nettoyer uniquement les sorties générées et refaire le build ; aucune modification des sources pour masquer ce défaut d’environnement.

## VMR-D006 — Hypothèses sur la lenteur / qualité, prototype VMR-002
Constat utilisateur : performance très médiocre de VisionMaxRead face à une IA multimodale ; nature exacte (délai, exactitude, structuration, voix) encore à mesurer. PaddleOCR crée et libère son moteur à chaque photo (`OcrService.kt`). `SupertonicEngine` charge ses quatre sessions ONNX à chaque lecture. Le worker unique sérialise OCR, NER et TTS. **Ce sont des hypothèses de ralentissement, pas des temps mesurés.** Le prototype ne modifie pas ces comportements ; il compare d'abord l'OCR et dissocie les étapes. Ne pas promettre que Gemma 3n/PaddleOCR-VL fonctionnent sur ce téléphone avant un test de mémoire/accélération. CI et test terrain à valider.

## VMR-D007 — Diagnostic CI VMR-002 (13 octobre 2026)
Premier échec : manifeste debug d'identification en conflit avec le label de la version stable ; corrigé par `tools:replace` dans `app/src/debug/AndroidManifest.xml`. La compilation suivante (run 38315196736, commit 5a43248b) réussit Gradle, Java, lint, APK et AAB ainsi que la vérification d'archive, mais `scripts/verify_apk.py` échoue sur l'assertion combinée « INTERNET absent / CAMERA présent ». **Cause probable** : une dépendance ML Kit ajoute la permission INTERNET lors de la fusion des manifestes. Ne pas attribuer la cause définitivement sans vérification du manifeste final. Correctif VMR-D007 : retrait explicite de `android.permission.INTERNET` dans le manifeste fusionné via `tools:node="remove"`, plus diagnostic détaillé et refus de livrer si l'APK contient encore INTERNET. Prochaine étape : CI verte avec preuve des permissions exactes, hashes modèles et signature. Aucun téléphone testé.

## VMR-D008 — Correctif privacy validé CI (13 octobre 2026)
Suite à VMR-D007, run `38316437083` **success** au SHA `53356ee7` : build Gradle/Java, lint, APK/AAB, restauration XZ et vérification de l'APK (permissions, signature et hashes) terminés. Le manifeste Android retire `android.permission.INTERNET` malgré l'ajout de dépendance ML Kit. Artefact GitHub Actions disponible, mais uniquement dans un ZIP de ~1,07 Go, donc pas conforme à l'idéal FAB-APK-TEMP-001 d'un téléchargement APK direct. Distribution directe et tests sur Samsung restent à vérifier. Aucun lien de Release inventé.

## VMR-D009 — Distribution binaire inaccessible directement depuis artifact Actions
Artifact `11777960102` du run CI vert `38316437083` : environ 1,07 Go car contient APK et AAB. Le transport via l'outil externe disponible refuse les archives au-delà de 512 MiB. Contournement durable FAB-APK-TEMP-001 : télécharger/décompresser dans **le runner GitHub Actions**, publier **seulement l'APK** comme asset de GitHub Release, vérifier SHA256 après téléchargement de cette Release. Ce changement de packaging n'altère pas le binaire testé. Publication et SHA final doivent encore être confirmés sur l'URL effective avant annonce.

## VMR-D010 — Prépublication : 404 de tag traité comme objet JSON
Run `38317719077` : téléchargement de l'artefact de 1,07 Go dans le runner et test intégrité APK **réussis**. SHA256 de l'APK `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438`. Échec de publication avant tout upload : `gh api .../git/ref/tags/v0.2.0-ocr-test` sur tag encore inexistant renvoie un objet JSON `{"message":"Not Found",...}` qui a été interprété comme un SHA existant. Correctif : `--jq '.object.sha // ""'` renvoie chaîne vide sur 404, mais conserve la vérification si un tag réel pointe vers un SHA non approuvé. Doit être vérifié par nouvelle CI. Aucune Release créée par ce run.

## VMR-D011 — Lecture du tag Git robuste (13 octobre 2026)
La tentative de gestion de 404 par `gh api --jq '.object.sha // ""'` n'a pas suffi : le wrapper `gh api` a encore propagé le JSON brut 404 comme chaîne non vide sur le run `38317821483`. L'APK de SHA256 `ca39674a...` est toujours validé, aucune Release créée. La vérification de référence bascule sur `git ls-remote --refs --tags origin "refs/tags/$TAG" | cut -f1` : absence du tag = chaîne vide, existence = SHA réel, sans confusion avec un corps HTTP d'erreur. Corriger uniquement le diagnostic de publication, sans reconstruction ni altération de l'APK source.

## VMR-D012 — Publication directe enfin vérifiée
Le run `38318000959` se termine **success** : la détection du tag avec `git ls-remote` a corrigé la fausse alerte des précédentes tentatives. Le runner a extrait le ZIP source de l'artefact validé (aucun transfert ZIP au téléphone), puis GitHub a publié la préversion `v0.2.0-ocr-test` avec un seul APK. La Release, non-brouillon, et son asset direct ont été confirmés par API GitHub. Re-téléchargement de l'APK depuis la Release et comparaison du SHA-256 `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438` réussis. Pas de fusion main ; aucun essai réel Samsung encore réalisé.


## VMR-D013 — Expérience Supertonic vs voix Android locale
Problème signalé : latence importante de la lecture Supertonic. Code observé : quatre sessions ONNX recréées à chaque lecture et huit pas de diffusion par segment ; **aucune mesure temps réel sur Samsung**. La nouvelle branche LAB propose une voix Android française `isNetworkConnectionRequired() == false` et refuse toute voix non disponible ; elle mesure `UtteranceProgressListener.onStart` depuis l'appel `speak`, **pas le premier son audible**. Attention : la synthèse Android dépend de l'engine TTS installé, indépendamment de la permission INTERNET de VisionMaxRead. Vérification CI et essai mode avion requis avant qualification.

## VMR-D014 — Livraison LAB, dossier temp demandé (13 octobre 2026)
L'artefact du run LAB `38365535174` (vert) existe en ZIP GitHub de ~1,07 Go contenant APK et AAB. Un simple `temp/` dans le dépôt Git ne peut pas conserver un APK de ~500 Mo (limite GitHub des blobs) ; le dossier temporaire d'un runner GitHub est éphémère. La procédure VMR-005 utilise `actions/download-artifact@v4` dans `RUNNER_TEMP/VisionMaxRead-LAB` (extraction du ZIP **côté serveur**), vérifie `unzip -t` et SHA256 de l'APK, puis l'attache seul à une préversion LAB, avec re-téléchargement SHA256. Ne prétendre à la livraison qu'une fois la Release GitHub vérifiée. Aucun Android modifié et `main` inchangée.

### Conclusion VMR-D014 — Livraison GitHub validée
Le build original `38365535174` et l'artefact `11796812007` ont été validés dans le workflow `38394243158`. L'extraction dans `RUNNER_TEMP`, la vérification de l'APK, sa publication en préversion `v0.3.0-lab-test` et sa re-vérification SHA256 après téléchargement ont tous réussi. SHA256 `dfebd355d7aba7db182a1d9ccc7d88616265f94249a562ab5ed1055144e23042`. Le fichier `VisionMaxRead-0.3.0-LAB.apk` est directement disponible sans ZIP, taille 539 256 754 octets. Aucun merge main, aucun test matériel Samsung prétendu.


## VMR-D015 — Première intégration Qwen/Piper et orientation (13 octobre 2026)
Le défaut de prévisualisation caméra provenait d'un calcul basé sur `SENSOR_ORIENTATION` seul ; la rotation réelle de l'affichage n'entrait pas dans la matrice et `onSurfaceTextureSizeChanged` ne recalculait rien. VMR-006 combine désormais capteur et Display et applique une orientation JPEG séparée. Validation physique portrait/paysage reste nécessaire.

Pour le TTS, GeckoDoku recréait l'AudioTrack à chaque phrase. VMR-006 sépare donc la chauffe de l'inférence et celle de la sortie audio : Piper est gardé en mémoire, une courte forme d'onde d'amorce traverse la vraie piste à gain zéro, la consommation est suivie par la tête de lecture et 250 ms de silence précèdent la vraie phrase. Aucun réglage de volume global.

Qwen3.5-0.8B-VL est volontairement testé comme OCR principal, avec PP-OCRv6-Small comme contrôle. Le fichier Qwen fait 1 303 966 384 octets ; son SHA-256 observé en CI est `5e6d69e55f92d21799fe0a2552da979c95a2533b64e4f0ed73fe87dd9d759ab9`. La taille APK, la copie privée, la RAM et la chauffe doivent être mesurées sur Samsung.

## VMR-D016 — Échec CI initial Kotlin, cause identifiée
Run `38435343498` : téléchargement Sherpa/Piper réussi, téléchargement Qwen réussi, préparation des modèles réussie. Échec à `:app:compileDebugKotlin` avant APK : LiteRT-LM 0.18.0 et ses dépendances Kotlin portent des métadonnées Kotlin 2.4.0 alors que le projet utilisait le plugin Kotlin 2.1.20. Ce n'est pas un échec de Qwen/Piper au chargement ; correction appliquée : plugin Kotlin 2.4.0, puis nouvelle CI requise. Ne pas déclarer de binaire valide tant que cette nouvelle CI n'est pas verte.


## VMR-D017 — Collision ONNX Runtime après passage Kotlin 2.4
Run `38436112539` : Kotlin 2.4 franchit la compilation Kotlin, puis Android échoue à `:app:mergeDebugNativeLibs` car deux `lib/arm64-v8a/libonnxruntime.so` sont présentes : Sherpa-ONNX 1.13.8 et ONNX Runtime Android 1.23.2 du projet. Audit upstream du tag Sherpa v1.13.8 : son build Android ARM64 utilise ONNX Runtime **1.28.2**. Correction choisie : aligner l'ORT Java/Android de Paddle + NER sur 1.28.2 et supprimer seulement la copie native ORT à l'intérieur de l'AAR Sherpa après vérification SHA de l'AAR. Une seule ORT 1.28.2 doit donc être empaquetée.

La CI montrait aussi des avertissements D8/R8 sur les métadonnées Kotlin 2.4. Le projet conserve AGP 8.13 mais surcharge R8 vers 9.4.14 via `settings.gradle`, mécanisme officiellement supporté pour AGP 9.2 et antérieurs. Nouvelle CI requise.


## VMR-D018 — ORT 1.28.2 non publié sur Maven et orientation encore verrouillée (14/10/2026)
Run `38436956702` : 47 contrôles core passent, puis `:ppocr-sdk:checkDebugAarMetadata` échoue car `com.microsoft.onnxruntime:onnxruntime-android:1.28.2` est introuvable dans Google/Maven Central. Le correctif D017 était donc conceptuellement aligné avec Sherpa 1.13.8 mais non résolvable par Gradle. Correction : Sherpa 1.13.4 (AAR SHA-256 `03f9c4df965f21c71269365a7951a7f23b5696fddd093fa318c80d65550ab780`) + ORT Android 1.27.0, avec une seule copie native empaquetée.

Audit simultané VMR-006 : le manifeste conservait `screenOrientation="portrait"`, incompatible avec l'objectif portrait/paysage, et `previewRotation()` utilisait le signe avant/arrière opposé à la formule JPEG. Le verrou est retiré, le signe est harmonisé et un listener de rotation recalcule le transform. L'AudioTrack était également libéré par `stop()`; il est désormais conservé entre arrêt/reprise et n'est libéré qu'au `release()`. Preuve CI et appareil encore requise.


## VMR-D019 — Refactor AudioTrack : helper manquant (14/10/2026)
Run `38504519405` : téléchargement Sherpa/Piper, Qwen et préparation des modèles réussis ; ORT 1.27.0 est bien résolu et les 47 contrôles core passent. Échec ensuite à `:app:compileDebugKotlin` sur deux appels `head(current)` laissés sans définition dans `PersistentVoicePcmPlayer`. Correction locale : lire directement `playbackHeadPosition` (non signé 32 bits) aux deux points de départ. Aucun APK n'a été produit par ce run rouge.


## VMR-D020 — Rotation Activity et diagnostic « première génération » incomplets (14/10/2026)
Après le run vert `38504818600`, audit de conformité fonctionnelle : retirer seulement le verrou portrait autorisait Android à recréer l'Activity lors d'une rotation, ce qui pouvait interrompre la session caméra. Ajout de `configChanges="orientation|screenSize"` pour conserver l'Activity et laisser CameraController recalculer le transform.

Le diagnostic PCM générait auparavant deux échantillons seulement lorsque l'utilisateur ouvrait le diagnostic, donc après l'amorce initiale : il ne représentait pas les deux **premières** générations réelles. L'amorce initiale génère maintenant deux fois le même texte, mémorise durées/hash/début de signal/fréquence, puis envoie la seconde génération dans l'AudioTrack muet. Nouvelle CI requise.


## VMR-D021 — VMR-006 CI verte finale (14/10/2026)
Run `38505688283` sur `cf7083b5d204626617aa17bc1515c8c910f82dc6` : **success**. Les 47 contrôles passent, Gradle/lint produisent APK+AAB, le contrat d'archive passe et `verify_apk.py` confirme signature debug, ARM64, tous hashes modèles et absence INTERNET. Mesures produites par `build-manifest.txt` :
- APK : 1 582 753 113 octets ; SHA-256 `be97ff8f4b90f3f5aa6cccb9564e1d9a13f0cefab8b341a47fea05da4c3686af`.
- AAB unsigned : 1 333 001 356 octets ; SHA-256 `977679dce7a4726f99a5f1219db3db99438002b8d694757369d768de22b680e2`.
Artifacts : binaire `11867298469`, metadata `11867068707`. Le poids très élevé vient notamment du Qwen VL 1 303 966 384 octets embarqué. Aucun résultat Samsung n'est inventé : test matériel encore à faire.


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

## 2026-10-14 — VMR-007, audit risques avant validation matérielle
- Le bundle PaddleOCR-VL-1.6 (1 390 305 472 o, SHA256 314df8b34d6911990d488e825602550a5bbcc5d6700cb1a8914e853e9043f4f1) est trop gros pour l'ajouter naïvement à l'APK Qwen existant. Import hors ligne avec picker Android et validation SHA256 (aucune permission INTERNET).
- Le modèle travaille en 560×560 fixe d'après sa fiche ; risques sur menus et petits caractères. Benchmark Samsung nécessaire, ne pas revendiquer ses scores de benchmark comme taux OCR local.
- Deux grosses instances LiteRT simultanées risquent l'OOM : la file Qwen commune décharge le moteur actif avant le suivant ; PP-OCR reste sur worker distinct.
- Le modèle Siwis source de k2-fsa est vérifié par SHA256 d'archive 375909aa30842b3a4efa10b1beb1d761af792960ae6873b4d53889f96c66195b, sans téléchargement appli. Tester prosodie en français ; Kokoro français pas encore validé dans Sherpa 1.13.4.
- Attention au premier caractère, à la reprise AudioTrack, au choix du texte utilisateur et à la stabilité Android 16 ; la CI teste le packaging mais pas le matériel.
- Actions GitHub anciennes déclenchées par les commits intermédiaires : seule une CI verte pour le dernier HEAD fait preuve.
