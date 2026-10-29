# VMR-013 — Logs à copier sans captures d'écran, véritable MoE à valider

Date : 29 octobre 2026. Branche `feature/vmr013-moe-copy-passes` depuis VMR-012. Mission demandée par Fab : « Il faut le MoE et les boutons pour copier les logs, je ne vais pas faire des imprim-écrans. Liquid n'y arrive pas. »

## Livré côté Android
- Deux **boutons visibles directement sous Passes Indra · rapport** après OCR : **Copier logs techniques** (aucun texte OCR ; statistiques PSS/durées/statuts et journal debug) et **Copier résultats · toutes les passes** (texte OCR + IA, avec confirmation de confidentialité).
- Dans **Passes Indra · rapport**, bouton **Copier une passe** : choix parmi P1, P2, P2B, P3 Liquid, P4A/B audits, P5 arbitre, ou erreurs ; rapport individuel collable dans la conversation. Si Liquid plante avant la fin, il reste possible de copier les traces et l'erreur avec un clic.
- `OcrTrace` conserve des instantanés par passe, sépare clairement texte et données techniques, et n'affiche jamais `status=executed` si la passe n'a pas tourné. La copie de texte privé est volontaire ; relire/anonymiser avant de la transmettre.
- LAB `fr.visionmaxread.labdebug`, versionCode 14, versionName `0.6.1-copy-logs-lab` ; même certificat que VMR-012 ; Qwen toujours absent, Indra + OrderNet/LM, Liquid natif, PP-OCR et TTS préservés.

## État réel du MoE
- **Meta MobileMoE-S-QAT** est un vrai MoE sparse, 1.3 Md paramètres totaux, ~272 M actifs, 60 experts routés top-4, poids QAT INT4 ~710 Mo. Il nécessite code modèle PyTorch custom `trust_remote_code`, non compatible directement avec notre runtime ARM64 `llama.cpp`. Licence FAIR Noncommercial Research. Modèle anglophone, résultats français inconnus.
- L'essai CPU de la véritable architecture Meta sur GitHub Actions est codé (Python `scripts/mobilemoe_cpu_probe.py`, `.github/workflows/mobilemoe-cpu-probe.yml`, environnement CPU isolé, corpus synthétique non sensible, mesures CER/temps et empreinte de révision du modèle), **mais l'exécution initiale `40975194290` a échoué AVANT inférence : Hugging Face retourne 401 `GatedRepoError`**, le modèle exige l'acceptation de sa licence et un accès autorisé. **AUCUN résultat Meta ni MoE Android ne doit être annoncé.**
- Pour retenter après acceptation explicite des conditions Meta par Fab, configurer **dans GitHub (pas dans la discussion)** un secret Actions `HF_TOKEN` d'un compte autorisé, puis déclencher manuellement le workflow `Experimental Meta MobileMoE French OCR (PC only)` depuis la branche. Ne jamais coller un jeton privé dans les logs.
- Candidat **de recherche alternatif non bloqué** : `rootxhacker/HobbyLM-Chat`, réel sparse MoE ~500 M total / ~169 M actifs, 36 experts top-6, Apache-2.0, modèle non francophone. Son fichier GGUF est ~2.16 Go en F32 et nécessite le runtime Rust `hobby-rs` (non pris en charge par `llama.cpp` standard) ; ne pas l'intégrer sans expérience de performance/qualité sur le français et mémoire mesurée. Aucune substitution silencieuse à Meta ni aucun « MoE factice ».
- Les passes `Liquid-task-expert` de VMR-012 sont **plusieurs appels au même Liquid**, pas des experts neuronaux MoE distincts. Le log `P4_MOBILE_MOE` reste `unavailable`. Liquid restant insuffisant pour la lettre manuscrite doit pouvoir être désactivé sans toucher au lecteur Indra.

## Prochain jalon technique
1. Installer l'APK VMR-013 et transmettre avec le bouton **Copier logs techniques**, surtout le message précis de l'échec Liquid ; puis une **passe ciblée** avec le texte après relecture/anonymisation.
2. Sans demander de capture d'écran, tester la même lettre et une page multicolonnes ; conserver les propositions sémantiques séparées et le texte brut.
3. Lever légalement le verrou Meta via compte autorisé ou choisir avec Fab un MoE réellement exécutable et utile en français avant de porter modèle/runtime Android. Tester le vrai réseau, latence, mémoire, licence ; les métriques de PC n'autorisent pas une affirmation Android.
4. Aucun merge dans `main`, aucun GitHub Release sans validation. APK direct sans ZIP uniquement après CI verte vérifiée.

Sources : https://huggingface.co/facebook/MobileMoE-S-QAT ; https://huggingface.co/rootxhacker/HobbyLM-Chat ; https://github.com/harishsg993010/HobbyLM .
