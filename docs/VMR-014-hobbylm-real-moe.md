# VMR-014 — Vrai MoE indépendant, HobbyLM-1B, Android hors ligne

Date : 30/10/2026. Branche `feature/vmr014-hobbylm-moe-android`, créée depuis VMR-013 (copie de logs). Aucun merge main ni publication Release sans feu vert.

## Motivation
Retour Fab : « Il faut le MoE et les boutons pour copier les logs ; Liquid n'y arrive pas. » VMR-013 fournit déjà un APK stable compilé avec les boutons de copie sans capture d'écran. MobileMoE-S-QAT de Meta est un modèle anglophone sous licence FAIR NC, qui est **gated** sur Hugging Face : le vrai test `40975194290` a échoué 401 AVANT toute inférence. Ne pas contourner l'accès ni inventer les résultats. Alternative expérimentale proposée : **HobbyLM-1B Instruct**, vrai réseau sparse MoE, **64 experts routés, 8 actifs**, environ 1,037 Md paramètres totaux / 305 M actifs, poids Apache-2.0, 729 Mo en Q4_K_M. Son français et sa fidélité OCR **ne sont pas établis**, ne pas le substituer silencieusement à la vérité source.

## Vrai portage natif expérimental
- Source du moteur `hobby-rs` : `harishsg993010/HobbyLM` à SHA `817878f52bf054a4d9aabfdd5360341dd7ce3fa6`, code source README Apache-2.0, compilé comme une `cdylib` Rust `libvmr_moe.so` pour `aarch64-linux-android` via NDK27.2.
- `native/moe/src/lib.rs` : pont JNI vers `Engine::load`, `Engine::generate` (vrai routage sparse), contrôle de fin et limites de taille/temps indirectes (jetons max 128). `HobbyMoeNative.java` : vérification SHA, copie locale du fichier depuis les assets seulement au premier appel, aucune permission INTERNET, sortie UTF-8.
- Modèle : https://huggingface.co/harims95/hobbylm-1B-gguf ; révision immuable `f8bc034403026b2706f5275702d3801753b87c34`; fichier `hobbylm-1b-sft-3450-Q4_K_M.gguf` SHA256 `0a1d1d4a7e545c51e8fdaa61707b5bf7d09cb4e948fc9b09c3ec2789c493cdbb` tiré du `SHA256SUMS.txt` officiel. Téléchargé uniquement en CI. `LICENSE` et `NOTICE` du modèle embarqués.
- `HobbyMoeReview.java` : le MoE analyse seulement les **460 premiers caractères** du texte Indra LM, avec proposition Liquid facultative comme seconde hypothèse ; il produit une véritable sortie neuronale indépendante P4_REAL_MOE_HOBBY et une vérification déterministe de conservation des chiffres P5_MOE_GUARD. La transcription originale reste intacte ; sélection explicite dans l'interface.
- Bouton indépendant **« Tester vrai MoE · HobbyLM (hors ligne) »**, optionnel, visible après OCR Indra. Si erreur, journal texte/metrics copiable depuis VMR-013 ; ne pas présenter une erreur comme une passe exécutée. Unique worker sémantique pour ne pas charger Liquid et MoE simultanément.
- L'APK expérimental VMR-014 utilisera code 15, version `0.6.2-real-moe-lab`, signature LAB persistante, Android ARM64, pas de Qwen, plus volumineux que l'APK VMR-013. **Prévoir plus de 1 Go supplémentaire de stockage**, sans estimation garantie de RAM ; le runtime Rust peut décoder les Q4 en Q8 et dépasser la taille des poids.

## Validation bloquante
1. CI GitHub Actions : téléchargement checksum/quantité, compilation Rust sur ARM64, build Gradle, signature LAB SHA256, absence INTERNET et intégrité du modèle dans l'APK, libvmr_moe.so et droits/licences.
2. **Aucun succès Android ne doit être annoncé avant CI verte**. Même CI verte ne garantit pas qu'un modèle 1B Instruct soit lisible par le runtime 500M hobby-rs au premier chargement : erreur éventuelle rapportée sans effacer Indra.
3. Essai Samsung sur manuscrit 1942 + page multicolonnes : 1) OCR Indra ; 2) comparer la correction Liquid si souhaité ; 3) lancer vrai MoE ; 4) copier logs techniques, proposition P4, verdict P5, durée et PSS directement (sans capture d'écran). Comparer chiffres/noms aux images, noter blocages.
4. Pas de publication générale : modèle anglophone, qualité encore hypothétique ; lancer un test CER/WER et ne jamais confondre fluence et fidélité. Respecter les licences et licences de données.
5. Aucune modification de Bonze pour l'instant ; si validé, on pourra réutiliser le même composant de test.

## Limites et état
Cette branche est un **essai de portage**, pas une promesse d'amélioration de la reconnaissance. Meta reste inaccessible sans autorisation Hugging Face et licence non commerciale ; HobbyLM est une alternative réelle mais différente.

## Sécurité de récupération après plantage
Dans la LAB VMR-014, « Copier logs techniques » reste actif immédiatement au démarrage même si aucun OCR n'est chargé, en copiant le journal DebugLog persistant sans texte OCR. Début de tentative de chargement MoE inscrit explicitement avec le nombre de caractères et la PSS, avant toute inférence. Cela permet d'investiguer une fermeture brutale du processus Android. La confirmation de copie de tout résultat avec texte reste obligatoire. Le test d'inférence *sur CPU GitHub* de la vraie GGUF HobbyLM-1B est passé dans le run 40976686174 (étape « Real sparse MoE CPU smoke test ») ; ceci ne remplace PAS un test de l'inférence ARM64 sur le Samsung.

## Résultat final CI et livraison
Source `d88c5002b0321d5c622c5975155fdc3bf7a3b559`, run [40977108326](https://github.com/greenpower2669/VisionMaxRead/actions/runs/40977108326) **SUCCESS** : exécution effective du modèle HobbyLM-1B en Rust sur CPU GitHub, bibliothèque Rust Android ARM64 construite, Java/lint/Gradle vérifiés, empreintes et licences présents, absence d'Internet et signature LAB. APK direct [12942629751](https://github.com/greenpower2669/VisionMaxRead/actions/runs/40977108326/artifacts/12942629751), `VisionMaxRead-0.6.2-REAL-MOE-LAB.apk`, 1252630529 octets, SHA256 `4cb627a8c7065f3f45e643d196faf4a5b4091b2566ac471e824e25bc8f2e31e9`. Version code 15, même certificat LAB, pas d'AAB généré. **Ces contrôles ne prouvent ni l'inférence ARM64 réelle sur Samsung ni la qualité du français.** Premier essai sur court fragment de manuscrit, en mode avion, sauvegarde logs techniques sans texte ou passe MoE avec relecture/anonymisation.
