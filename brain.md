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
