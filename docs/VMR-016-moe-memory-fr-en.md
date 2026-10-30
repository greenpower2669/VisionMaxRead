# VMR-016 — Première expérimentation MoE mémoire et correction FR/EN

Branche `feature/vmr016-moe-memory-fr-en` depuis `feature/vmr015-moe-live-tokens`, le 30 octobre 2026. FAB-Copilot / cinq mémoires vivantes du projet. Aucune fusion `main`, aucune Release.

## Objectif et réalisation du premier jalon

- Garder **un seul modèle HobbyLM-1B Q8** chargé pour un groupe de **trois lignes** OCR maximum, chacune de 150 caractères au maximum, sans partager de KV cache entre les lignes.
- Nouvelle API JNI : `nativeOpen(path)`, `nativeWeightBytes()`, `nativeGenerateLoaded(prompt,...)`, `nativeClose()` ; `OnceLock<Mutex<Option<Loaded>>>` garde l'Engine chargé durant le groupe, puis le libère explicitement. `HobbyMoeNative.Session` Java est `AutoCloseable` et ferme le modèle sur succès/échec/arrêt. Les callbacks de jetons et la demande d'arrêt restent opérationnels.
- **Français / English** : choix explicite dans la boîte de dialogue. Prompts courts dédiés à l'orthographe de chaque langue ; aucune prétendue spécialisation « experts français » ni apprentissage ajouté.
- Lignes invalides trop longues ignorées sans troncation ; toutes les autres lignes du document conservées dans leur ordre initial. Chaque proposition est comparée à la ligne Indra d'origine, surtout les nombres. Rien ne remplace le texte du document sans choix explicite.
- Fenêtre stream avec séparation visible des lignes et compteur jetons par ligne. Boutons Arrêter, Copier flux, Copier une passe et Copier logs techniques conservés.
- `P4_MOE_SESSION` consigne la durée d'ouverture, une mesure PSS après chargement et les octets de poids internes. `P4_MOE_SEG_1..3` sépare les générations réelles par langue et ligne ; `P5_MOE_GUARD` résume les propositions retenues/rejetées.
- Poids HobbyLM Q4_K_M authentifiés au build, convertis en Q8 par le moteur actuel. **Ce jalon ne prétend pas avoir supprimé la conversion Q4 → Q8 ni diminué le pic de RAM.** Il évite en revanche les chargements répétés entre les trois passages ; l'effet en secondes et sur le système reste à mesurer sur Samsung.

## Points techniques non résolus

- Version Native Rust actuelle : `Engine::load(path,true)`, le GGUF Q4 est déquantifié et les matrices Q8 sont conservées en mémoire ; `Engine::generate` crée un nouveau `KvCache` à chaque ligne. L'architecture comporte 64 experts par couche routée et top8 ; un cache à la demande n'est pas implémenté.
- Viser à terme **le runtime officiel llama.cpp patché HobbyLM-1B**, en particulier `bailingmoe2_decoupled_head_dim.patch`, pour essayer de rester en Q4 mmap. La carte officielle indique que l'outillage est validé sous Windows x64 CPU et non Android ARM64 ; ne pas supposer la portabilité sans test CPU ARM64 et tests de fidélité.
- Pour un tel portage, comparer coûts réels RSS/PSS et page faults, base Q8 actuelle vs Q4, nombre d'experts chargés, temps du premier jeton, ECC des chiffres, qualité sur FR/EN. Les essais d'anglais/français ne suffisent pas à prouver la spécialisation interne d'un expert.
- L'Android OS peut tuer le processus pendant le pic de chargement Q8. La session est donc **temporaire uniquement**, libérée à la fin du lot ; ce jalon ne cherche pas à conserver le modèle pendant des heures.

## Protocole test Samsung et décision

1. Reprendre exactement la même lettre manuscrite 1942 et sélectionner **Français** : enregistrer `P4_MOE_SESSION`, `P4_MOE_SEG_1..3`, P5, PSS et temps ; comparer VMR-015.
2. Reproduire ensuite sur trois lignes simples d'anglais et sélectionner **English**, copier les sorties. Faire une troisième vérification des chiffres et références.
3. Comparer temps par ligne, coût d'ouverture unique, mémoire pendant le chargement et mémoire après fermeture. En cas de crash mémoire, copier le journal persistant sans captures.
4. Ne pas conclure à un gain mémoire Q4 tant que le runtime Q4 n'existe pas. **Deuxième jalon** : portage Q4 natif ou cache sélectionné, uniquement après analyse des mesures et compatibilité sur CPU.
5. Tests automatisés : ancien contrat de streaming/annulation, `scripts/test_vmr016_memory_contract.py`, Gradle/lint/arm64/sha/offline/signature. Une CI verte ne constitue pas une preuve de précision des corrections.

Sources :
- https://huggingface.co/harims95/hobbylm-1B-gguf
- https://huggingface.co/harims95/hobbylm-1B
- https://github.com/harishsg993010/HobbyLM
