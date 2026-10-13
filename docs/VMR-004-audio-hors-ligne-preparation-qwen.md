# VMR-004 — Banc d'essai vocal hors ligne ; préparation Qwen Vision

Date : 2026-10-13. Branche : `feature/offline-tts-qwen-prep-v1`.
Statut : **code de l'option vocale poussé ; résultats CI et téléphone à vérifier**.

## Découpage validé oralement
- Étape immédiate : comparer notre Supertonic 3 à la synthèse vocale Android **explicitement locale**. L'objectif est de mesurer le prix réel de la génération audio, tout en conservant la lecture française.
- Étape suivante de développement : Qwen3.5-0.8B *Vision* quantifié sur le même Samsung, face à PaddleOCR et ML Kit sur le même cliché ; aucun faux succès si la partie vision n'est pas intégrée.
- Exploration distincte, ultérieure : Qwen Omni, vraie entrée/sortie multimodale avec audio natif. **Ce modèle n'est pas contenu dans cette branche.**

## Code VMR-004
- Application d'essai **VisionMaxRead LAB** : `fr.visionmaxread.lab` (debug seulement), indépendante du stable `fr.visionmaxread` et de l'APK OCR TEST `fr.visionmaxread.ocrtest`.
- Menu Réglages : Supertonic par défaut, option Android hors ligne volontaire. Supertonic reste inchangé et présent dans le laboratoire.
- `OfflineAndroidTts` sélectionne uniquement une voix `fr` non marquée `isNetworkConnectionRequired()`, refuse une voix non installée, bloque la lecture sur le worker existant, libère l'engine après chaque demande et accepte l'annulation. Ce test n'écrit pas les textes.
- La mesure « synthèse commencée » repose sur `UtteranceProgressListener.onStart` ; **ce n'est pas une mesure du premier échantillon audible**. Ne pas présenter cette durée comme latence perceptive précise.
- L'APK n'a pas de permission INTERNET ; revérifier le manifeste final, la signature debug et l'intégrité des poids dans la CI.
- Aucun accès réseau depuis VisionMaxRead ; le fonctionnement d'un service TTS tiers est extérieur à l'application. En cas de doute, tester en mode avion.

## Comparaison Samsung (à réaliser)
1. Conserver la même page française (une phrase, puis une page longue) et une transcription corrigée.
2. Régler la même vitesse ; écouter Supertonic F1 puis la voix Android locale, en mode avion.
3. Noter : délai d'initialisation, démarrage synthèse, délai **perçu** jusqu'aux premiers mots, temps total de lecture, intelligibilité, mémoire et chauffe.
4. Tester Arrêter/Relire et sortie de l'application ; vérifier que l'audio ne reste pas en cours.
5. Si aucune voix `fr` hors ligne disponible, constater l'échec explicite, sans remplacement réseau.

## Préparation Qwen Vision (pas encore intégrée)
- Référence officielle : https://huggingface.co/Qwen/Qwen3.5-0.8B (licence Apache 2.0).
- Piste GGUF Q4 et projecteur vision séparé (`mmproj`) à sélectionner **ensemble**, puis tester dans une application Android hors ligne avec runtime vision qui supporte exactement Qwen3.5 (`libmtmd` par exemple).
- Les ~760 Mo évoqués ne mesurent que les fichiers quantifiés **à confirmer** ; RAM, cache KV, dimensions d'image et délai de transcription ne sont pas établis.
- Ne jamais traiter un GGUF *texte seul* comme un lecteur d'image ; ne pas ajouter de permission INTERNET à VisionMaxRead pour télécharger le modèle.
- Avant inclusion du modèle, tester hors APK par import local, empreintes SHA256 des fichiers et traitement d'une photographie d'écriture manuscrite française non sensible.
- Un modèle vision-langage n'est pas nécessairement un TTS. Qwen Omni, avec audio, sera une autre expérimentation conditionnée aux résultats.

## Distribution
- Push CI permis sur cette branche, **aucun merge main ni release** sans accord spécifique. La CI produit des artefacts de test ; AAB unsigned = non publiable Google Play.
- Validation matérielle Samsung indispensable avant toute affirmation de gain de performance.
