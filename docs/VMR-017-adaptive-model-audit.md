# VMR-017 — Audit A et mise en chantier de B/D (30 octobre 2026)

## État de référence vérifié

- Branche source : `feature/vmr016-moe-memory-fr-en`, HEAD `df6661900152b2d4e4dfaf4d42abb3ce995bb048`. Branche VMR-017 isolée, aucun merge `main`, aucune Release.
- `feature/vmr016-q4-bilingual-memory` **diverge** : trois commits de documentation supplémentaires par rapport à l'ancêtre commun, aucun runtime Q4 natif supplémentaire observé dans la comparaison de fichiers. Lire ses consignes, ne pas fusionner aveuglément.
- VMR-016 HobbyLM : le code Java imposait **72 jetons** ; JNI Java et Rust plafonnaient à **128** ; l'écran affichait `/ 72`. Aucun code en amont n'attestait un véritable motif EOS. 
- Faux positif réel Samsung : proposition non documentaire avec marqueurs de citation, autre texte accepté à tort en conservant chiffres/longueur. Ce dernier cas est un défaut avéré de l'arbitrage P5.
- Mesures rapportées sur le Samsung : chargement **4,157 s**, poids internes déclarés **1111 Mio**, pic PSS **1623,7 Mio**, retour après fermeture **276,9 Mio**. Il ne s'agit PAS d'un gain Q4 natif.

## Modèles identifiés, audit de licences et de formats

| Moteur | Référence upstream | Quantification d'étude | Taille approximative du fichier | Licence |
| --- | --- | --- | --- | --- |
| Liquid 1.2B textuel | `LiquidAI/LFM2.5-1.2B-Instruct-GGUF` | `LFM2.5-1.2B-Instruct-QAD-Q4_0.gguf` ou Q4_K_M | 696 / 731 MB | **LFM Open 1.0**, conditions commerciales particulières |
| Qwen 1.7B textuel | `Qwen/Qwen3-1.7B-GGUF` | Q4_K_M, sous réserve d'empreinte et révision bloquées | ~1,28 GB | **Apache-2.0** |
| Liquid ancien | `LFM2.5-230M-QAD-Q4_0.gguf` | Q4_0 | 149 MB | LFM Open 1.0 |
| HobbyLM expérimental | `harims95/hobbylm-1B-gguf` | Q4_K_M converti Q8 dans hobby-rs | 729 MB fichier | notices existantes |

Sources primaires : https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct-GGUF ; https://huggingface.co/Qwen/Qwen3-1.7B-GGUF ; https://www.liquid.ai/lfm-license .

**Blocages avant intégration C :** figer révisions+SHA256 depuis les fichiers upstream, noter tokenizer et templates exacts, confirmer support de l'architecture Qwen3 et LFM2 sur la révision *effective* de llama.cpp Android ARM64, contrôle qualité et RAM/PSS. Ne pas utiliser la taille GGUF comme RAM prédite. Qwen3 prend en charge un mode sans réflexion ; le templating spécifique du modèle et les marqueurs de fin doivent être respectés. La révision actuelle du runtime Liquid 230M ne prouve pas à elle seule le support des deux grands modèles.

## Modification B vérifiable dans le source

- `AdaptiveTokenBudget.java`: estimation initiale *par caractères Unicode*, marge de génération, plafond de sécurité 128..512; **l'estimation n'est pas un comptage de tokens du tokenizer**. L'ancien appel à 72 est supprimé.
- `HobbyMoeNative.java` et `native/moe/src/lib.rs` : intervalle autorisé de génération monté jusqu'à 512; aucun changement silencieux de modèle ni du moteur de poids Q8.
- `HobbyMoeReview.java`: premier jeton et débit observés, compte réel des jetons, budget effectif, raison d'arrêt **UPSTREAM_END_REASON_UNKNOWN** ou **TOKEN_BUDGET_REACHED_OR_EOS_UNKNOWN**. Une sortie qui consomme tout son budget ne passe jamais le filtre comme correction complète.
- `OcrProposalGuard.java`: garde-fous lexicaux, nombres/références, noms propres intérieurs, répétitions et marqueurs de citations/consignes. Les faux positifs de citations doivent être rejetés. Le filtre est conservateur, peut refuser une vraie correction et ne valide jamais la précision sans la photo ou une transcription de référence.
- `MoeStreamDialog.java`: affiche le budget de **la phrase courante** ; stop/copie/stream maintenus.
- `scripts/test_vmr017_contract.py` : compilation+exécution de cas Java sans Android, dont l'hallucination « Cite this article as » et des corrections FR/EN, contrats source.

**Non achevé en B :** aucun EOS sûr instrumenté dans hobby-rs, pas de continuation incrémentale conservant le KV-cache à budget épuisé, pas de mesure exacte des jetons du prompt, pas de détection de contexte saturé complète ; marquer les causes d'arrêt comme inconnues lorsqu'elles le sont. Les exceptions et arrêts utilisateur restent remontés séparément par les chemins existants.

## Début de D (expérimental)

- `DocumentContextMemory.java` : stockage **RAM seulement** par document, observations OCR non confirmées distinctes des hypothèses et informations explicitement validées ; maximum 150 caractères pour le précédent, 300 pour un sujet confirmé ; reset sans sérialisation.
- Le banc MoE courant dispose de quatre essais FR/EN × avec/sans l'extrait OCR précédent. Le contexte ajouté est étiqueté « incertain », ne doit jamais être considéré comme fait et ne reçoit pas la proposition du modèle précédent.
- À ce stade, les **3 premières lignes de ≤150 caractères** restent la fenêtre expérimentale de VMR-016. **Aucune segmentation complète du document ni évaluation exhaustive sur plusieurs dizaines de phrases n'est encore livrée.**
- A/B : chaque exécution recrée son contexte initial ; aucune mémoire de Liquid/Qwen n'est réutilisée puisque ces moteurs ne sont pas encore intégrés.

## Prochain travail bloquant

1. Finir les tests/CI et vérifier APK **signed LAB** code 18, ID `fr.visionmaxread.labdebug`, certificat constant, sans INTERNET, APK direct sans ZIP. Ne pas annoncer un build vert avant son résultat.
2. Confirmer/implémenter EOS natif, extensions de budget sans réinitialiser la génération, répétitions détectées pendant le stream et une comparaison de qualité sur un corpus.
3. Intégrer Liquid 1.2B et Qwen 1.7B séparément, modèles authentifiés importés hors ligne, un seul moteur chargé, templating, sélecteur à trois modes dont OCR seul et HobbyLM séparé.
4. Développer segmentation FR/EN + vraie mémoire structurée, versionnement/effacement, comparaisons reproductibles avec/sans mémoire, métriques CER/WER de référence.
5. Benchmark PSS/RSS/mmap Q4 et cache experts, tests de charge, essais Samsung ; **ne pas fusionner main, ne pas créer Release sans accord**.

## Jalons C/D supplémentaires — import Q4 vérifié dans le source

- Deux GGUF Q4 sont désormais identifiés *avec SHA256 précis* dans `OfflineCorrectionModels.java` : `LiquidAI/LFM2.5-1.2B-Instruct-GGUF` fichier `LFM2.5-1.2B-Instruct-QAD-Q4_0.gguf` SHA `bb741ebb106d543e9de114b843a3d3d73d51c74b5801e69da2abde821a0cb3e1` ; `ggml-org/Qwen3-1.7B-GGUF` fichier `Qwen3-1.7B-Q4_K_M.gguf` SHA `d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5`. Pages sources : https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct-GGUF/blob/main/LFM2.5-1.2B-Instruct-QAD-Q4_0.gguf et https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/blob/main/Qwen3-1.7B-Q4_K_M.gguf .
- Import par Android SAF depuis les fichiers locaux, sans permission INTERNET, SHA256 calculé à la copie et **à chaque chargement**. Pas de modèles volumineux intégrés à l'APK ; l'utilisateur installe ses GGUF indépendamment. Choix explicite dans Réglages et dans le test après OCR.
- Le pont JNI llama.cpp Liquid 230M devient un runtime commun séquentiel pour ces fichiers, via vérification avant load, un seul contexte modèle natif à la fois, nouvelle cause EOS/limite accessible en JNI. La génération des modèles 1.2/1.7B n'a **pas encore été mesurée sur Samsung** ni prouvée en CI au moment de ce commit ; la compatibilité du modèle QAD et du template Qwen /no_think reste à confirmer. Aucun poids ou licence tiers copiés dans les assets.
- `DocumentSentenceChunks.java` conserve les caractères et abréviations en découpant des segments ; `TextualDocumentReview.java` traite 40 segments max et expose les corrections seulement si EOS et gardes de fidélité valides. Le streaming de HobbyLM est intact ; **le streaming jeton par jeton des nouveaux modèles reste absent**, ainsi que la conservation de session prolongée entre clics, l'extension de budget en cours de génération et le benchmark complet.

## Complément : génération en flux sur les deux correcteurs

`native/liquid/vmr_liquid.cpp` envoie maintenant la sortie UTF-8 **cumulative réelle à chaque jeton** vers `LiquidNative.inferStreaming` JNI, sans `NewStringUTF` pour le texte OCR potentiellement non-BMP. `TextualDocumentReview` remonte premier jeton et débit ; `TextModelStreamDialog` expose arrêt et copie sensible avec consentement. Les appels restent sérialisés sur `semanticWorker` et le moteur natif est libéré à la fin du lot. Le décodeur natif expose explicitement le motif `eos` ou `token_limit` et le nombre de jetons générés. **La CI et l'essai Samsung du dernier HEAD doivent encore attester l'exécution des deux nouveaux poids** ; les limites ne sont pas des benchmarks. Pas de continuation à budget extensible sans perdre le KV-cache dans cette version.
