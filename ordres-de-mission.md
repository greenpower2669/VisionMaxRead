# Ordres de mission — VisionMaxRead

## IMMUTABLE / À PRÉSERVER — FAB-IMMUTABLE-001
- Identité Android actuelle de l'application principale : `fr.visionmaxread` (projet en expérimentation ; **aucune identité Google Play publiée attestée**).
- Variante **debug uniquement**, distincte et installable en parallèle : `fr.visionmaxread.ocrtest` sur la branche VMR-002. Ne jamais substituer ce package expérimental à l'identité principale ni publier son AAB sans réévaluation du package et de la signature.
- Données sources et images privées : aucune permission `android.permission.INTERNET` dans l'APK validé ; contrôle obligatoire `scripts/verify_apk.py` après fusion des manifestes.
- Distribution test VMR-002 : `VisionMaxRead-0.2.0-OCR-TEST.apk` (debug signé) et `VisionMaxRead-0.2.0-OCR-TEST-unsigned.aab` (non signé, non publiable sur Play en l'état).
- Aucune Upload Key ni App Signing Key Google Play établie pour cette application dans ce dépôt. Ne conserver dans Git/mémoires ni secret ni keystore privé. Toute modification d'identité ou de protocole de signature nécessite accord de Fab et avenant daté.

## VMR-001 — Application Android d’aide à la lecture (Fab, 11 octobre 2026)
**État : Première version construite et vérifiée ; livraison des fichiers, essai téléphone à effectuer.** Autorisation explicite : « Ok on code ça VisionMaxRead le repo est créé […] on y va ».

- Reconnaître du texte dans des images ; français ; fonctionnement hors ligne.
- Caméra : prévisualisation vidéo basse résolution pour viser seulement.
- Bouton rouge : figer une image ; reconnaître la photo nette, pas l’aperçu dégradé.
- Sélectionner une image présente sur le téléphone.
- Afficher le texte et un bouton Play pour le lire avec Supertonic 3.
- Recueillir les textes et les anonymiser si une IA hors ligne le permet.
- Dépôt fourni : greenpower2669/VisionMaxRead.

Interprétation annoncée : historique local après masquage automatique ; texte original uniquement à l’écran et en mémoire pour la lecture. Aucune transmission réseau. L’anonymisation automatique n’est pas une garantie d’anonymat.

La vidéo demandée ici est l’aperçu caméra. L’import de fichiers vidéo, évoqué pendant l’étude, est enregistré séparément comme possibilité à confirmer et ne doit pas remplacer les fonctions caméra/photo prioritaires.

## Contraintes de livraison
Branche dédiée ; pas de fusion dans main ni de publication de release sans validation de Fab. APK de test nommé VisionMaxRead + version, icône identifiable ; AAB distinct si produit. Tests téléphone à marquer « à valider ».

## VMR-002 — Essai contrôlé de reconnaissance (Fab, 13 octobre 2026)
**État : APK expérimental compilé et vérifié CI sur commit 53356ee7 ; validation Samsung en attente.** Accord utilisateur : « On test? », en réaction à la proposition de comparer nos moteurs et d'évaluer une IA multimodale.

Objectif : ne pas toucher à la version stable ; créer un comparateur local de reconnaissance PaddleOCR vs ML Kit latin (bundled), même image, durées et résultats lisibles. Préserver confidentialité et fonctionnement avion. Comparer séparément une vraie IA vision/langage via application de démonstration compatible ; aucune affirmation de multimodalité si seul OCR fonctionne.

Branche : `feature/ocr-ab-benchmark-v1` ; protocole : `docs/VMR-002-essai-ocr.md`. Ne pas fusionner/releaser avant accord et résultats d'essai.

## VMR-003 — Publication de la préversion APK directe (Fab, 13 octobre 2026)
**Commande explicite : « Feu vert »** après proposition de publier VisionMaxRead OCR TEST sous forme de préversion avec APK téléchargeable directement, sans ZIP sur Samsung et **sans fusion main**.

- Diffusion autorisée **uniquement** pour la variante test `fr.visionmaxread.ocrtest`, tag `v0.2.0-ocr-test`, APK direct `VisionMaxRead-0.2.0-OCR-TEST.apk` (source vérifiée au SHA `53356ee7`).
- Réutiliser la CI verte `38316437083`, préserver l'absence de permission INTERNET ; jamais d'AAB unsigned présenté comme prêt Google Play.
- Publier via un workflow contrôlé GitHub Actions ; re-télécharger l'asset Release et vérifier son SHA256 avant d'annoncer la livraison.
- **Statut : Livré, à valider sur Samsung.** Release de préversion publiée et vérifiée : https://github.com/greenpower2669/VisionMaxRead/releases/tag/v0.2.0-ocr-test. APK direct 539 252 854 octets, SHA-256 `ca39674a8c408a59bcb69cdf93dd422dccd4c1c5adb8e297de8c4d0f6a69e438`; run de publication `38318000959` vert, source APK du build `38316437083`. Aucun merge `main`, aucun changement de signature du stable.


## VMR-004 — Étapes vocales et IA compacte (Fab, 13 octobre 2026)
Instruction orale : mettre en œuvre d'abord la solution locale proposée pour VisionMaxRead, puis évaluer une IA Qwen plus intégrée dans un temps distinct. Interprétation prudente : **prototype audio Android hors ligne face à Supertonic**, puis **Qwen3.5-0.8B Vision** pour la reconnaissance ; Qwen Omni (voix et vision intégrées) comme expérience séparée. Ne pas présenter le travail préparatoire comme une intégration Qwen accomplie.
Branche exclusive `feature/offline-tts-qwen-prep-v1` : identité expérimentale `fr.visionmaxread.lab` (debug), stable `fr.visionmaxread` et ancien OCR test `fr.visionmaxread.ocrtest` préservés. Aucun merge, Release, changement de signature, permission INTERNET ou texte brut persisté sans autorisation. Voir `docs/VMR-004-audio-hors-ligne-preparation-qwen.md`.

## VMR-005 — Décompression de l'APK LAB en dossier temp GitHub (13 octobre 2026)
Suite à la demande de Fab « Demande à Git de nous dézipper dans un répertoire temp un .apk » après le succès CI LAB, extraire l'artefact **sur GitHub Actions** en `RUNNER_TEMP/VisionMaxRead-LAB`, sélectionner uniquement `VisionMaxRead-0.3.0-LAB.apk` du run vert `38365535174` (commit `644d5177`). Le dossier temp est éphémère ; pour livrer directement le binaire au téléphone sans ZIP, créer une **préversion expérimentale** `v0.3.0-lab-test` contenant uniquement cet APK, après vérification SHA256 en sortie. Ne toucher ni à `main`, ni au package stable, ni à une Release de production. La livraison est à confirmer par lien réel et CI verte. La branche du laboratoire et son code Android restent inchangés.

### Preuve de clôture VMR-005 (13 octobre 2026)
**Terminé et vérifié** : workflow `38394243158` vert. Préversion LAB isolée `v0.3.0-lab-test` publiée (https://github.com/greenpower2669/VisionMaxRead/releases/tag/v0.3.0-lab-test), APK direct `https://github.com/greenpower2669/VisionMaxRead/releases/download/v0.3.0-lab-test/VisionMaxRead-0.3.0-LAB.apk`, 539 256 754 octets, SHA-256 `dfebd355d7aba7db182a1d9ccc7d88616265f94249a562ab5ed1055144e23042`. Le répertoire `RUNNER_TEMP/VisionMaxRead-LAB` était éphémère et a seulement servi à l'extraction côté GitHub ; **seul l'APK signé** a été publié, pas l'AAB. L'APK téléchargé depuis la préversion a été comparé par SHA256 avec l'original. `main` n'a pas changé. Tests réels Samsung à effectuer.


## VMR-006 — Qwen principal, PP-OCR référence, Piper Jessica et caméra (Fab, 13 octobre 2026)
**Autorisation explicite : « Ok code ça ».** Branche `feature/qwen-piper-camera-v1`. Aucun merge `main` et aucune Release sans nouvel accord.

1. Remplacer Supertonic par Sherpa-ONNX/Piper UPMC medium, Jessica sid 0.
2. Garder le moteur chargé et amorcer silencieusement la chaîne réelle ; pas de timer arbitraire, pas de volume système ; garde audio ~200–300 ms et diagnostic première génération.
3. Lancer Qwen automatiquement comme OCR après capture/import.
4. Remplacer le bouton ML Kit par PP-OCRv6-Small, moteur de référence déjà connu.
5. Comparer Qwen et PP-OCRv6-Small sur le même bitmap, avec temps et choix du résultat à conserver/lire.
6. Corriger l'orientation de l'aperçu caméra avec la rotation réelle de l'écran, et conserver la cohérence aperçu/photo/OCR.

Contraintes : hors ligne, permission INTERNET absente dans l'APK final, aucune persistance supplémentaire des images/textes bruts, variante LAB `fr.visionmaxread.lab`. Livrable seulement après compilation, lint, vérification APK et essais Samsung explicitement distingués.


### VMR-006 — Note d'exécution du 14/10/2026
La version précise de Sherpa n'était pas imposée par l'ordre utilisateur : elle peut être ajustée pour compatibilité Android tant que Piper `fr_FR-upmc-medium`, Jessica sid 0, la chaîne hors ligne et les critères fonctionnels restent inchangés. Correctif en cours sur la branche VMR-006 : Sherpa 1.13.4 / ORT 1.27.0, déverrouillage portrait/paysage, AudioTrack conservé après arrêt, diagnostics temps/PCM/PSS. Ne pas transformer cet état en preuve de livraison avant CI verte et conserver les tests Samsung comme validation humaine distincte.


### VMR-006 — Raffinement de conformité du 14/10/2026
Pour satisfaire littéralement « première et deuxième génération du même texte » et « rotation du terminal », le premier warm-up capture la vraie paire PCM initiale avant lecture utilisateur et MainActivity absorbe les changements orientation/screenSize afin que CameraController puisse corriger la prévisualisation sans perdre la session. La CI doit également publier une preuve textuelle légère des tailles et SHA-256 APK/AAB. Toujours : aucun merge main, aucune Release sans feu vert explicite de Fab.


### VMR-006 — État de livraison technique après CI (14/10/2026)
Le code SHA `cf7083b5d204626617aa17bc1515c8c910f82dc6` est la version construite et vérifiée par le run `38505688283` vert. APK test identifié : `VisionMaxRead-0.4.0-QWEN-PIPER-LAB.apk`, 1 582 753 113 octets, SHA-256 `be97ff8f4b90f3f5aa6cccb9564e1d9a13f0cefab8b341a47fea05da4c3686af`. AAB unsigned : 1 333 001 356 octets, SHA-256 `977679dce7a4726f99a5f1219db3db99438002b8d694757369d768de22b680e2`. Artifacts Actions : `11867298469` (binaires) et `11867068707` (preuve compacte). Ceci est une preuve de build, pas encore une preuve d'usage Samsung. Interdictions inchangées : pas de merge main, pas de Release sans nouveau feu vert.


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

## MISSION VMR-007 — Feu vert de Fab (14 octobre 2026)
Demande explicite : « Ok, on se lance et l audio en qualité maximal » à la suite de la comparaison PaddleOCR-VL-1.6 / OvisOCR2 et du besoin d'une voix française plus naturelle.
Exécution autorisée sur branche expérimentale `feature/vmr007-specialist-ocr-audio-hq-v1` uniquement, SANS MERGE MAIN et SANS RELEASE AUTOMATIQUE.
Objectifs : tester OCR spécialisé PaddleOCR-VL-1.6 hors ligne, ne jamais casser Qwen et PP-OCR, préserver leurs workers et les logs diagnostics ; maximiser l'articulation/agrément audio avec Siwis testable et Jessica préservée.
Interdits immuables : permission INTERNET, remplacement silencieux de moteurs validés, nouvelles clés de signature, publication AAB unsigned, annonce de succès sans run CI et test Samsung.
Livrables techniques en préparation : source UI/import/moteur, CI signé LAB 0.4.4 code 8, guide `docs/VMR-007-ocr-specialise-voix-hd.md`.
Audit du compromis : le modèle OCR supplémentaire (~1,39 Go) est importé localement par l'utilisateur après téléchargement externe ; pas d'APK 3 Go. Deux moteurs LiteRT ne doivent pas rester initialisés ensemble. Siwis ne représente pas une garantie de meilleure qualité vocale avant A/B subjectif ; Kokoro en piste ultérieure. Privilégier disponibilité, accessibilité et stabilité du téléphone.
Première vérification requise : CI verte sur le dernier HEAD et validation effective Android 16. Faire rapport honnête en cas d'échec.

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


## VMR-008 — Qwen inchangé, audio naturel, caméra et mémoire (15/10/2026)
Instruction de Fab : « Ok on garde qwen et on optimisé le reste, l audio est pas mal juste mal découpé à mon gout ».
- Branche dédiée \`feature/vmr008-qwen-audio-flow-camera-memory\`, dérivée de VMR-007, \`main\` intact.
- **Qwen reste OCR principal** : même Qwen3.5-0.8B-VL, modèle LiteRT, backend et prompt inchangés. PP-OCRv6-Small conserve son worker indépendant ; spécialiste PaddleOCR-VL toujours facultatif/import séparé.
- Audio Siwis/Jessica : ancien \`TextChunks.split(text,240)\` coupait au dernier espace et le prochain segment n'était synthétisé qu'après la lecture du précédent. Nouveau \`TextChunks.speech(text,340)\` privilégiant les fins de phrase (abbreviations françaises protégées), ponctuation puis espaces; retours simples OCR recollés, paragraphes distincts (180 ms). Nouveau \`VoicePCM\` avec anticipation bornée d'un segment (une seule synthèse native à la fois), AudioTrack persistant, garde initiale 250 ms seulement, arrêt sans verrou audio long.
- Caméra : \`CameraController.start\` ignore un deuxième appel identique pour le même document (évite l'ouverture en double observée le 15 octobre 00:14:41). Capture JPEG et matrices d'orientation non modifiées.
- Mémoire : \`MainActivity.onTrimMemory\` transfère la libération des moteurs LiteRT sur \`qwenWorker\`, après les inférences éventuellement en cours, sans fermer Qwen à chaque lecture OCR/PP-OCR. Le moteur se recharge au prochain besoin après alerte mémoire.
- LAB \`fr.visionmaxread.labdebug\` v0.4.5-debug, versionCode 9, signature LAB persistante inchangée. Pas de permission INTERNET. APK/AAB livrés séparément et directement, sans ZIP.
- Tests unitaires du cœur étendus : paragraphes, retours de ligne OCR, ponctuation, abréviations, Unicode/emoji, bornes max en points de code; ne pas prétendre une CI verte sans preuve du run HEAD.
- Vérifications matérielles nécessaires : 1 seul CAM start/open par session, bonne orientation réelle, 2+ phrases audio à débit continu, arrêt immédiat, changement Siwis/Jessica, Qwen 1ère et 2e image, PP-OCR, PSS/pression mémoire, mode avion.
- **Interdictions** : pas de merge sur \`main\`, pas de Release ni publication Play sans nouvel accord explicite de Fab. La compilation CI n'est pas une validation Samsung.

## VMR-009 — Indra OCR à tester (29/10/2026)
Demande explicite : « Implémente le dans vision max read, on va le tester non? ».
Portée autorisée : nouvelle branche d'essai depuis VMR-008, prototype Indra local Android avec comparaison sur le même bitmap ; les moteurs Qwen et Paddle, la lecture audio, la confidentialité et la clé/identité LAB ne sont pas remplacés. L'implémentation initiale peut être limitée au Latin imprimé à condition d'être clairement marquée « prototype », sans présenter les fonctionnalités Indra Python complètes comme disponibles. Livraison seulement après CI et preuve d'APK ; aucune publication Release ni fusion main sans accord supplémentaire. Détails : docs/VMR-009-indra-ocr-latin.md.

## VMR-010 — commande one-shot Indra + Liquid + MoE sans Qwen (29 octobre 2026)
Fab autorise un essai expérimental unique avec source en branche séparée, sans Qwen, Indra brut/relues, ordre multi-colonnes, Liquid, MobileMoE puis seconde validation, et logs de chaque passe à analyser ensemble ; souhaite un APK direct seulement s'il est réel, compilé et vérifié. Ne pas fusionner main ni toucher à Bonze. Séparer les résultats réellement produits par un moteur et les étapes non exécutées ; ne jamais présenter un simple arbre de règles comme un MobileMoE neuronal. Garder les images/texte privés par défaut, export de toutes les passes avec texte seulement après action consciente ; mode avion ; préserver voix, ancienne identité de labo et reprise. Résultat source initial : moteur Indra/OrderNet, fallback PP-OCR, trace; le portage Android de Liquid et MobileMoE reste un travail bloquant distinct (non validé). Voir `docs/VMR-010-indra-liquid-moe-architecture.md` et `todo.md`.

## VMR-011 — reprise après analyse des logs (29 octobre)
Fab : « Ok passe à la suite », autorisation de continuer le codage sur une branche expérimentale après essai de VMR-010. Corriger le mélange de colonnes sur magazine, porter `lm.npz` officiel plutôt que laisser correction indisponible, fournir A/B OrderNet/XY-cut et logs suffisamment détaillés pour analyse commune ultérieure. Préserver Qwen absent, PP-OCR de secours, lecture Piper/Siwis/Jessica, offline, signature LAB, sans `main` ni Release. Créer source `feature/vmr011-indra-layout-lm` depuis VMR-010, version LAB 0.5.1/code12, APK direct uniquement si CI verte. Liquid/MobileMoE non requis pour ce jalon, ne jamais simuler leur inférence.

## VMR-012 — demande de Fab « Ok, on code » (29/10/2026)
Suite à l'essai d'une lettre manuscrite historique difficile, intégrer une couche sémantique Liquid puis une revalidation multi-experts avec traces, en préservant Indra, l'ordre des colonnes, les voix et les comparaisons. **Un vrai runtime Liquid hors ligne Android**, poids modeste et authentifiés, doit être tenté prioritairement ; ne pas présenter le routeur de prompts du même Liquid comme le vrai Meta MobileMoE. Développer une branche séparée `feature/vmr012-liquid-semantic-experts`, faire tourner une CI APK signée avec zéro INTERNET, fournir APK direct sans ZIP si vert. Comparer sur la lettre exacte de Fab, ne jamais altérer chiffres/noms sans confirmation ; logs P3/P4/P5 accessibles après consentement de copie. Aucun merge main ni Release sans validation distincte. Voir `docs/VMR-012-liquid-offline-multiexperts.md`.

## VMR-013 et VMR-014 — injonction Fab « Il faut le MoE et les boutons pour copier les logs, Liquid n'y arrive pas »
Priorité 1 : pas de captures d'écran ; boutons accessibles et directs après OCR, copie globale sensible uniquement avec confirmation, une passe individuelle ou diagnostics sans texte ; conserver les logs d'échec de Liquid. Résultat VMR-013 LAB code14 compilé vert en branche isolée, pas de merge ni release. Priorité 2 : vrai MoE neuronal indépendant, jamais requalification de plusieurs prompts Liquid en Meta/MoE. Meta QAT gated 401, sans accès autorisé ; étudier en branche VMR-014 un modèle open véritable sparse HobbyLM-1B Instruct Q4 avec JNI Rust, scripts de build/checksums et bouton facultatif, puis examen Samsung avant décision. Ne jamais annoncer un modèle exécuté si seul le build a réussi. Préserver Indra/OrderNet/LM, voix, masquage, PP-OCR et signature LAB. `docs/VMR-013-copy-logs-moe.md`, `docs/VMR-014-hobbylm-real-moe.md`.

## Mission VMR-015 — Fab : « Ok, corrige ça en montrant la chaîne en temps réel ^^ » (30/10/2026)
Modifier code VMR-014 en branche VMR-015 seulement : envoyer les marqueurs de conversation officiels HobbyLM et observer la génération en continu depuis le moteur Rust, **pas des résultats factices après calcul**. Fenêtre live accessible, jetons défilants, Arrêter, Copier flux sans capture d'écran mais avec avertissement confidentialité, métriques et résultat final dans OcrTrace, stop pendant chargement, suppression du bouton « Utiliser ce texte » sur les réponses MoE rejetées. Maintenir Indra, LM, OrderNet, Liquid optionnel, PP-OCR, TTS Siwis/Jessica, boutons de copie des autres passes, signature LAB et mode offline sans Qwen. Faire vérifier la CI/JNI et fournir APK direct non zippé si vert, comparer sur la même lettre manuscrite. Éviter merge main et Release tant que Fab ne valide pas. Qualité MoE améliorée = hypothèse à tester, pas résultat prouvé.

## VMR-016 — autorisation de Fab « avant prépare prompt de passation » puis « Gogogo »
Préparer d'abord un pont de passation complet, ensuite lancer expérimentation mémoire directe du MoE, focalisation anglais/français. Garder vrais modèles, dates, chiffres, noms, rapports par passe et streaming, Indra+Liquid, hors ligne. Expérience initiale à faible risque : session persistante **sur un lot uniquement**, trois lignes ≤150 caractères, choix FR/EN, mesures load/PSS/poids, comparaison, fermer en fin de lot. Ne pas prétendre que cela équivaut à un cache sélectif d'experts ou Q4 natif. Faire une CI complète signée code17, APK direct sans ZIP uniquement si vert, garder `main` et Releases intacts. Pour deuxième jalon, benchmark du runtime llama.cpp spécifique Q4 HobbyLM sur Android ARM64 avant réécrire expert cache. Source `docs/VMR-016-moe-memory-fr-en.md`.

## Mission VMR-017 — demande explicite de Fab (30/10/2026)
Base à examiner : `feature/vmr016-moe-memory-fr-en`, regarder VMR-012 à VMR-016 et mémoires FAB-Copilot/Djipie. Créer branche dédiée. Étapes ordonnées : A audit licences/modèles/runtimes et mémoire ; B correctifs faux positifs OCR, plafond 72, causes de fin (sans les inventer) ; C intégrer deux moteurs textuels **Liquid LFM2.5-1.2B** et **Qwen3-1.7B** Q4, choix OCR seul et expérimentation HobbyLM séparée ; D mémoire volatile progressive FR/EN + comparaison équitable avec/sans contexte ; E optimisations Q4 natif/mmap, tests, signé APK LAB direct après CI. Préserver Indra/OrderNet/LM/PP-OCR/voix/historique/stream/copie/offline/signature ; pas de texte brut persisté ; interruption et effacement. **Jamais de merge main ou Release sans validation explicite.** La première livraison source VMR-017 est incomplète : détail dans `docs/VMR-017-adaptive-model-audit.md`, suivi bloquant dans `todo.md`.

### Suivi d'exécution VMR-017 — ajout moteurs textuels
Fab demande explicitement deux modèles sélectionnables Liquid 1.2B et Qwen3 1.7B, sans écraser Indra ni confondre Qwen visuel. Le chemin source choisi est import hors ligne via Android SAF (pas de téléchargement dans l'app), Q4 vérifiés par SHA256, un seul grand modèle à la fois, sélection « Aucun correcteur », comparaison FR/EN et avec/sans contexte. `TextModelStreamDialog` affiche les jetons avec Stop/Copie ; propositions admises uniquement après EOS natif + garde-fou. Tests réels, mémoire PSS, benchmark qualité et certification du nouveau HEAD restent obligatoires avant d'annoncer livraison utilisable. Aucun merge main ni Release.
