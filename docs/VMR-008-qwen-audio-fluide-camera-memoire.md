# VMR-008 — Optimisations après essai Samsung du 15 octobre 2026

## Décision de Fab
« Ok on garde qwen et on optimisé le reste, l audio est pas mal juste mal découpé à mon gout »

**Conserver Qwen.** Ne pas substituer automatiquement PP-OCR ou PaddleOCR-VL. Aucun merge main, aucune Release sans autorisation distincte.

## Observations d'entrée (SM-A576B, Android 16, VMR-007 v0.4.4)
- Qwen initialisé : 15,665 s après copie du modèle, 3 523 caractères sur image 1500 × 2000, 93,133 s de temps total à froid (inférence seule plus courte).
- Qwen sur 2e image 1448 × 1086 : 324 caractères, 13,805 s.
- PP-OCRv6-Small : 1817 caractères en 5,487 s sur photo 1, 286 caractères en 1,873 s sur photo 2.
- PSS après Qwen 2380 puis 2840 Mo. Mesure **processus total**, pas attribution native par moteur; ne pas forcer le GC ou sacrifier la précision Qwen sans benchmark.
- Deux CAM start/open quasi simultanés à 00:14:41, risque de session redondante.
- Retour utilisateur : qualité vocale acceptable, découpage et silences mal ressentis.

## Modifications
- Nouveau planificateur \`TextChunks.speech\` (Java pur, sous tests contracts), ponctuation terminale puis ponctuation de clause puis espace. M. / Mme / Dr / Pr / etc. protégés. Regroupement des lignes OCR en une phrase; séparation des paragraphes vrais; maximum de 340 codepoints Unicode par passage.
- \`PiperSpeechEngine\` anticipe la synthèse du prochain passage pendant la lecture du précédent ; un seul thread natif génère l'audio, un seul thread \`VoicePCM\` joue le PCM, tampon limité à un passage. La garde anti-coupure du début (250 ms) reste. 180 ms uniquement aux vrais changements de paragraphes, pas de silence arbitraire à chaque tranche.
- \`PersistentVoicePcmPlayer.stop\` invalide la lecture en cours immédiatement sans patienter sous le verrou \`play\`; \`release\` termine proprement le PCM. Vérifier la réactivité de l'arrêt sur Samsung.
- \`CameraController.start\` devient idempotent pour un même document tant que la caméra est voulue; aucun changement de la rotation JPEG/matrice caméra.
- \`MainActivity.onTrimMemory\` libère Qwen et spécialiste uniquement en réponse à une pression mémoire Android, sur leur file sérielle, et les réinitialise si une reconnaissance ultérieure le nécessite.

## Identité et packaging
- Branche \`feature/vmr008-qwen-audio-flow-camera-memory\`.
- Debug \`fr.visionmaxread.labdebug\` 0.4.5-debug, versionCode 9, même keystore LAB persistant.
- Aucun ajout INTERNET ; pas de modèle téléchargé par l'application.
- Actions utilise \`upload-artifact@v7\`, \`archive:false\` pour les APK/AAB livrés séparément.

## Recette Samsung avant validation
1. Installer APK signé LAB versionCode 9.
2. Déclencher caméra une fois. Vérifier journal avec 1 seul \`CAM start\`, 1 seul \`CAM open\`, \`CAM duplicate start ignored\` possible; JPEG bon et aperçu lisible après rotations.
3. Refaire les deux photos Qwen (retour texte, pas d'échec cache), comparer PP-OCR indépendant, reporter temps et PSS.
4. Avec voix Siwis, lire deux pages : français, nombres, paragraphes, M./Mme/Dr, citations, interrogations et point d'exclamation ; vérifier l'absence de mots coupés et de longs silences.
5. Arrêter pendant une phrase puis relancer ; le bouton doit répondre immédiatement, le début de la lecture suivante ne doit pas être tronqué.
6. Refaire Jessica pour comparaison.
7. Observer mémoire et chauffe; provoquer retour de tâche/pression mémoire seulement si test sans risque, vérifier le rechargement Qwen.
8. Aucune preuve de supériorité audio ni de gain RAM sans ces essais physiques.
