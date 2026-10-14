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
