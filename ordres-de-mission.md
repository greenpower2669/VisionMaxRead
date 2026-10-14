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
