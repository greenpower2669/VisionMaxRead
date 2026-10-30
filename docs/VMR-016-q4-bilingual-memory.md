# VMR-016 — Optimisation mémoire MoE Q4 et correction OCR français–anglais

**Passation FAB-Copilot — 30 octobre 2026.** Dépôt `greenpower2669/VisionMaxRead`, base `feature/vmr015-moe-live-tokens`; nouvelle branche `feature/vmr016-q4-bilingual-memory`. Mission autorisée par Fab pour commencer des expériences *progressives* ; jamais de fusion dans `main` ni GitHub Release sans son accord distinct. Ne pas travailler sur un autre projet.

## Point de départ attesté
- LAB VMR-015 `0.6.3-moe-live-lab`, code 16, package `fr.visionmaxread.labdebug`, APK CI direct au run `41038502413` / source `2cd829b0445ab922037d91dc716f5146295392a8`.
- HobbyLM-1B-Instruct GGUF Q4_K_M 729 156 672 octets, 1,037 Md paramètres totaux, environ 305 M actifs par jeton; 19 couches MoE avec 64 experts routés/couche, top-8 et expert partagé. Aucun « expert français » exclusif démontré.
- Code `native/moe/src/lib.rs` : `Engine::load(path,true)` par appel ; streaming JNI réel ; `stop=eos_or_limit` n'attribue pas une cause exacte. `HobbyMoeNative.java` fait un SHA-256 entier du GGUF à chaque appel et installe une copie privée ; `HobbyMoeReview.java` limite la sonde à 260 caractères et 110 jetons.
- Logs Samsung fournis : deux générations de 7 jetons / 22 caractères, en 39,554 et 53,698 s ; PSS ponctuel jusqu'à 1 657 Mo. Le garde-fou a refusé les propositions et préservé l'OCR. Il n'est PAS démontré que tous les 1 657 Mo soient des poids ou qu'il s'agisse d'une fuite.

## Objectif central
Diminuer la **mémoire réellement résidente**, le pic et les rechargements, sans sacrifier la fidélité du manuscrit français ni les documents anglais. Distinguer paramètres totaux, paramètres activés, fichier Q4, représentation de calcul, cache KV, allocations et PSS. Ne pas déduire qu'un MoE à 305 M paramètres actifs ne doit utiliser que 305 M paramètres de RAM.

## Ordre d'exécution — petits morceaux, comparaison avant/après à chaque étape
1. **Mesure référence sans modifier la sémantique.** Tracer chargement, vérification SHA, conversion éventuelle Q4→Q8, mémoire PSS/RSS de pic (inclure les mesures pendant l'inférence), temps prefill, premier jeton, jetons/s, cache KV, entrées/sorties disque et annulation. Instruments sans texte privé. Distinguer démarrage froid/chaud et mesurer sur même Samsung/document.
2. **Fin de génération.** Instrumenter séparément fin EOS authentique, limite de jetons, contexte saturé, callback stop, erreur ; analyser template officiel `<|user|>...` / `<|assistant|>`, tokenizer, EOS ID, paramètres de génération. Ne pas affirmer la cause des 7 jetons avant preuve.
3. **Session persistante sûre.** Vérifier puis supprimer les reconversions et chargements redondants par *réutilisation du même moteur* pendant l'analyse d'un document, avec fermeture/trim mémoire/annulation explicites. Un seul worker natif à la fois, jamais bloquer l'UI. Revalider SHA à l'installation ou quand l'intégrité doit réellement être recontrôlée, sans supprimer la protection contre corruption.
4. **Q4 natif/poids partagés.** Auditer exactement `Engine::load(path,true)` dans le `hobby-rs` épinglé avant de modifier son booléen; déterminer comment le GGUF Q4 est transformé, la part Q8/FP32 et le pic transitoire. Comparer exécution quantifiée native / `mmap` / runtime compatible HobbyLM (dont llama.cpp adapté), en vérifiant Android ARM64 et équivalence de routage/sortie. Attention: les poids des routeurs et biais peuvent devoir rester FP32; ne pas les quantifier arbitrairement.
5. **Français–anglais par segments.** Extraire des segments respectant géométrie Indra/OrderNet, idéalement ligne ou phrase courte (limites configurables, sans couper noms/dates/références). Générer sur chaque segment avec contexte minimal, réutiliser la session mais réinitialiser le KV/contexte de génération entre segments si nécessaire; préserver ordre, blancs et caractères accentués. Montrer source/proposition/diff et permettre acceptation explicite uniquement. Baseline FR, EN et documents mixtes ; langue détectée ou choisie sans exiger de « spécialistes français » inventés.
6. **Experts à la demande — étude puis prototype seulement si justifié.** Enregistrer anonymement les IDs d'experts sélectionnés par couche et jeton, taux de réutilisation sur français et anglais, occupation / lectures disque. Étudier un cache borné et des poids Q4 accessibles à la demande ; mesurer gains RAM contre E/S et latence. Ne jamais supprimer ou forcer un groupe d'experts par langue sans tests comparatifs de qualité et d'identité des sorties.

## Jeu de tests et qualité
Mêmes trois ensembles hors ligne : lettre française manuscrite de 1942 (évaluation après consentement, **pas de texte brut dans logs**), textes français accentués et anglais imprimés synthétiques, document multi-colonnes/mixte. Vérifier strictement chiffres, dates, noms propres, ponctuation, accents, ordre de lecture, non-hallucination ; mesurer CER/WER seulement sur transcription de référence fiable. Inclure les 2 appels VMR-015 comme baseline documentée, pas comme benchmark statistique. Garde-fou actuel maintenu ; préserver l'original et toute proposition rejetée.

## Matrice de décision / jalons
A: Q8 actuel à froid et à chaud, sortie/raison d'arrêt; B: session persistante à algorithme égal; C: Q4 natif compatible; D: segments FR/EN; E: cache experts expérimental. Pour chaque option : mémoire PSS de pic, poids résidents estimés, temps chargement, délai premier jeton, débit, qualité et robustesse. Trancher sur gain mesuré, pas sur taille du GGUF seule. En cas de divergence de sorties, échec RAM ou ralentissement trop fort, conserver variante VMR-015 et documenter.

## Invariants et livraison
Préserver Indra det/rec, OrderNet, XY-cut, LM, PP-OCR, Liquid facultatif, TTS Siwis/Jessica, logs copiables, flux jetons, Stop, texte original, confidentialité sans réseau (aucune permission INTERNET), signature LAB et package. Ne pas retoucher Bonze, CleanOR ni Djipie. Respecter `brain.md`, `brainmap.md`, `debughistorical.md`, `todo.md`, `ordres-de-mission.md` du dépôt et la méthode FAB-Copilot; mise à jour des mémoires à chaque jalon, sans dupliquer la mémoire externe.

Commencer par **un seul changement mesurable**, le valider CI Rust/Java/lint et test Android; ne pas annoncer une preuve Samsung depuis un run hôte. Ne publier un APK direct (nom explicite VMR-016, icône LAB conservée) qu'après build/signature/SHA réels et autorisation de publication. AAB distinct, jamais fourni comme APK installable. **Aucun merge main ni Release sans validation de Fab.**

## Références techniques
- Source projet : `native/moe/src/lib.rs`, `HobbyMoeNative.java`, `HobbyMoeReview.java`, `scripts/build_hobby_moe_android.sh`, `docs/VMR-015-moe-live-token-stream.md`.
- Papier : https://arxiv.org/abs/2610.34582
- Carte modèle : https://huggingface.co/harims95/hobbylm-1B
- Concept expert offloading (non une validation sur Android) : https://docs.vllm.ai/projects/vllm-metal/en/latest/moe_offload/
