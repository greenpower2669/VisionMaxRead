# VMR-015 — HobbyLM : véritable diffusion des jetons, format de conversation corrigé

Suite au test réel de Fab sur Samsung, VMR-014 a démontré des appels authentiques HobbyLM-1B mais des réponses hors sujet : 46,6 s/82 caractères puis 122,4 s/156 caractères, avec détection et rejet de modifications de chiffres. **Le moteur fonctionnait, la fidélité OCR était insuffisante.** Fab demande explicitement « corrige ça en montrant la chaîne en temps réel ».

## Changements de source, sans régression de VMR-014
- Branche `feature/vmr015-moe-live-tokens`, version LAB `0.6.3-moe-live-lab` (code16), signature LAB identique, `main` inchangée.
- **Format officiel de dialogue HobbyLM-1B Instruct** : `<|user|> ... <|assistant|>`. L'ancien `USER: ... ASSISTANT:` était transmis littéralement au runtime `hobby-rs::Engine::generate`, qui est un générateur de continuation et **n'applique aucun chat template** tout seul.
- Pour isoler le problème, le prompt est raccourci et ne contient plus les hypothèses Liquid ; il traite au maximum les 260 premiers caractères de l'OCR Indra corrigé, avec une limite de 110 jetons. Qualité française encore inconnue ; le rendu ne doit pas être présenté comme une correction vérifiée.
- **Streaming natif réel**, pas une animation : `native/moe/src/lib.rs` prend un callback Java et invoque `onToken(delta,tokenCount)` **depuis le closure de génération Rust**, à chaque jeton, y compris ceux dont le delta UTF-8 est temporairement vide. Les deltas sont générés par `hobby-rs` sans fragmentation illégale de caractères. Les chaînes restent en mémoire de l'application. Aucune fuite de texte OCR dans les logs systèmes.
- `MoeStreamDialog.java` affiche les morceaux au fur et à mesure, défile vers le bas, indique le compteur de jetons et le temps écoulé, offre **Arrêter**, **Copier flux** (consentement sur données sensibles) et **Fermer**. Les jetons visibles ne sont **pas** une chaîne de raisonnement privée : le runtime n'expose qu'une continuation textuelle publique.
- Les annulations demandées pendant la copie/vérification du modèle (~729 Mo) restent valides jusqu'au démarrage effectif de `Engine::generate` grâce à `HobbyMoeNative.reset/nativeStop` et un flag Java ; fermer le document ou l'activité stoppe la génération et ferme sa fenêtre.
- En cas de stop/error, `OcrTrace` conserve `P4_REAL_MOE_HOBBY` avec `interrupted` et résultat partiel lorsqu'il existe. Toujours conserver les boutons **Copier logs techniques**, **Copier toutes les passes**, **Copier une passe**. Pas de flux privé vers DebugLog.
- Une sortie HobbyLM **rejetée** pour non-conservation des chiffres ou trop courte apparaît dans le flux mais **ne propose plus « Utiliser ce texte »**. Seuls les candidats passant le filtre déterministe peuvent ouvrir « Comparer » et demander explicitement à l'utilisateur de sélectionner le texte. Même un candidat validé par ce filtre n'est pas prouvé exact ; vérifier contre l'image.

## Tests et limites
- `scripts/test_vmr015_stream_contract.py` vérifie interop JNI, marqueurs, callback, stop/copie et rejet sécurisé ; `scripts/test_vmr010_contract.py` accepte le statut `interrupted` explicite.
- `.github/workflows/android.yml` met à jour le test CPU HobbyLM avec les vrais marqueurs, compile Rust ARM64 et livre uniquement un APK LAB direct si la CI est verte.
- Le fait d'afficher les jetons ne crée pas de réponses « internes » cachées ; cela permet de diagnostiquer le décodage et le rythme réel.
- Aucun nouveau modèle téléchargé côté téléphone, aucune permission INTERNET ; pas de merge main ni Release sans autorisation. Essai Samsung requis pour confirmer le flux réellement visible, la stabilité, les chiffres, les erreurs et la qualité.
