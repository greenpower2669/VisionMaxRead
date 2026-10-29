# VMR-010 — Indra + LiquidAI + MoE : reconstruction sémantique vérifiable

Date : 29 octobre 2026. Branche : `feature/vmr010-indra-liquid-moe-review`, créée depuis VMR-009. **Conception/mission, pas implémentation ni APK.** Ne pas fusionner `main` ni publier de Release sans validation de Fab.

## Décision de Fab
Abandonner Qwen dans la prochaine variante expérimentale en raison de son poids et de la pression sur la mémoire partagée Android. Conserver Indra comme OCR local, ajouter les composants officiels d'ordre de lecture (OrderNet) et de correction n-gramme, puis bâtir une **architecture multicouche LiquidAI → MoE → revalidation MoE**. Objectif : un texte fidèle, remis en ordre, lisible à voix haute, avec conservation intégrale du texte original et des preuves OCR.

## Chemin de traitement proposé
0. **Acquisition et vérité source.** Image originale, boîtes/coordonnées, texte brut Indra, confiance par ligne, candidats de reconnaissance (si disponibles), identifiants des blocs. Ne jamais écraser les observations brutes.
1. **Structure avant sémantique.** `order.onnx` (OrderNet) : prédire blocs/ordre de lecture ; repli XY-cut géométrique. Distinguer colonnes, titres et tableaux, et vérifier l'ordre avec exemples de pages réelles. `lm.npz` et beam CTC : départager des lectures OCR incertaines ; ce modèle n'est **pas** un modèle sémantique global.
2. **Liquid (première interprétation).** Petit modèle quantifié à tester : LiquidAI LFM2.5-230M, déjà connu de Bonze. Sur les seuls blocs ambigus, proposer des corrections ciblées (span original, suggestion, raison courte), en conservant les alternatives et la confiance OCR. Ne jamais traiter sa confiance déclarée comme une probabilité étalonnée.
3. **MoE (deuxième lecture distincte).** Routeur vers des experts à définir et entraîner/évaluer : langue/syntaxe, cohérence de paragraphe, nombres-unités, entités/noms propres ; un contrôleur de cohérence géométrique reste séparé du langage. Chaque expert doit examiner les **données OCR originales** et les candidats, pas seulement la proposition de Liquid. Si l'implémentation provisoire ne comporte que des règles et un routeur, la nommer honnêtement « système multi-experts », pas véritable MoE neuronal.
4. **Revalidation et arbitrage.** Second passage conditionnel du MoE sur texte brut, variante Liquid, variante MoE, indices OCR et positions. Comparer les hypothèses, accepter uniquement les changements soutenus par les observations ; préserver l'original lorsqu'il y a désaccord, incertitude ou contexte insuffisant. La répétition d'un même réseau n'est **pas une preuve indépendante** ni une probabilité statistique fiable. Étalonner expérimentalement les scores sur un jeu de vérité terrain.
5. **Restitution.** Montrer texte brut et texte corrigé, signaler les corrections avec retour arrière, corriger l'ordre des colonnes avant TTS, conserver les voix/accès actuels. Les chiffres, doses, montants, dates, références et noms propres restent inchangés sans indice OCR explicite et confirmation quand nécessaire.

## Budget de calcul : conditions
- Ne pas appeler le LLM/MoE systématiquement : texte simple = Indra + ordre + LM ; texte incertain = Liquid puis experts ; arbitrage seulement en cas de conflit ou score ambigu.
- Faire tourner les étapes séquentiellement sur CPU ; mémoire mesurée en PSS/RSS sur le Samsung de Fab, pas estimée avec les seuls poids. Libération des sessions si nécessaire. Ne pas annoncer de latence, d'autonomie ni de qualité sans mesures sur appareil.
- LFM2.5-230M Q4 quantifié est une **hypothèse**, pas un choix final validé. Vérifier runtime Android, disponibilité de checkpoints et licences ; comparer éventuellement variantes QAD Q4_0 / GGUF.
- Retirer le code/poids Qwen **seulement de la branche VMR-010 expérimentale** après préserver une référence fonctionnelle VMR-009, et vérifier la taille réelle APK/AAB, la signature LAB, le nommage correct, l'absence de permission INTERNET et les compatibilités de mise à jour.
- Préserver la synthèse vocale et la chaîne caméra ; erreurs/timeout/OOM doivent conduire au texte OCR original, sans supprimer le texte ni la lecture.

## Critères de validation
- Corpus local non sensible imprimé FR : accents, noms, chiffres, unités, colonnes (2 ou 3), titres traversants, tableaux, listes, notes et manuscrit ; référence humaine ligne/bloc.
- Comparatifs ablation : Indra brut, +OrderNet/LM, +Liquid, +experts, +arbitrage. Comparer CER/WER, ordre des blocs, nombre de corrections justes/fausses, erreurs critiques et abstentions. Mesurer latence médiane/P95, pic PSS, batterie/chauffe sur Samsung, mode avion et stabilité après plusieurs pages.
- Pas de présentation comme « IA plus précise » tant que les mesures n'ont pas établi le gain ; ne jamais faire passer un score heuristique pour une probabilité calibrée.
- Garder un commutateur de désactivation des corrections, un aperçu accessible et les textes originaux en mémoire sans stockage du contenu sensible ; pas de log du texte brut.

## Sources publiques de conception (à ne pas confondre avec un résultat de test)
- Indra officiel (code source) : https://github.com/datalab-to/indra — `indra_ocr/lm.py`, `indra_ocr/order_post.py`, `indra_ocr/layout_post.py`.
- LiquidAI, LFM2.5-230M (25/06/2026, mesures sur appareils de référence, **pas sur notre Samsung**) : https://www.liquid.ai/blog/lfm2-5-230m
- LiquidAI, QAD Q4_0 (19/08/2026) : https://www.liquid.ai/blog/qad
- Lin et al., « NeKo: Cross-Modality Post-Recognition Error Correction with Tasks-Guided Mixture-of-Experts Language Model », ACL 2025 : https://aclanthology.org/2025.acl-industry.17/

## Statut au 29 octobre
Architecture définie ; **aucun code Liquid/MoE/OrderNet ajouté à l'APK dans cette mission**, aucune compilation ni validation Samsung, pas d'APK/AAB VMR-010 livré. Ne pas annoncer de travail matériel réalisé. L'étape suivante est l'intégration incrémentale sur cette branche avec tests oracle et CI, puis les essais matériels.

## Réalisation constatée à l'issue du premier push et CI
L'essai compile sur GitHub Actions : [VMR-010 run 40952014104](https://github.com/greenpower2669/VisionMaxRead/actions/runs/40952014104). APK Indra + OrderNet `VisionMaxRead-0.5.0-INDRA-ORDER-SIWIS-LAB.apk` SHA256 `9b6cc5d2401bccea217851801e38f26f5cedb01613570ca76b273bb0d3a39189` (343172170 octets), package LAB code 11, certificat inchangé, zéro permission INTERNET ; `main` intact. L'ancien texte « aucun APK » ci-dessus décrit le statut de la conception initiale, non le statut final de la branche.

Le **runtime Android actuel** exécute P1 Indra det/rec, P2 `order.onnx`, P5 arbitrage conservateur qui conserve intégralement le texte ordonné. Il journalise P2B Indra LM, P3 Liquid et P4 MobileMoE en `unavailable` car aucun moteur neuronal n'a été invoqué ; une sortie de relecture sémantique n'est jamais simulée. Entraînement/portage/évaluation nécessaires. Le banc PC `scripts/mobilemoe_benchmark.py` sait lancer les modèles réels en mode explicite, mais n'est pas une preuve d'Android. Contrôle qualité sur Samsung toujours ouvert.

L'export du journal brut du système ne contient aucune reconnaissance textuelle. Le bouton **Passes Indra · rapport** permet, après double confirmation, de copier un rapport qui **contient les textes** de chaque passe réellement exécutée. Il faut relire et anonymiser avant de l'envoyer à quiconque.
