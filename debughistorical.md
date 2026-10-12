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
