# VMR-017 — Correctif de continuité après le test Samsung (30 octobre 2026)

## Diagnostic vérifié
La LAB 0.6.5/code18 déclarait un mode de contexte, mais le constructeur cherchait le texte littéral antislash+n au lieu du saut de ligne placé avant le marqueur assistant. En conséquence, il renvoyait le prompt initial sans rappel de la phrase précédente.

Le test utilisateur a montré 43, 22 puis 96 jetons (budget 96 par ligne). Le troisième passage a atteint son plafond ; la cause des deux autres arrêts n'est pas connue. Chargement HobbyLM Q8 : 4 802 ms. PSS après chargement : 1 683 Mio ; après déchargement : 275,7 Mio. Les poids sont conservés au cours du lot, mais le cache KV reste neuf à chaque ligne.

## Modification
MoePromptBuilder.java transmet effectivement, avant la phrase actuelle, la précédente ligne OCR (maximum 150 caractères) et une ancre du début du document (120 caractères). Ces deux extraits sont explicitement présentés comme incertains : ils ne proviennent pas des hypothèses du modèle. Les logs enregistrent ctx_sent et ctx_chars, sans texte brut. Une régression Java en CI vérifie les vrais prompts, le premier passage sans mémoire, les passages ultérieurs et le mode sans contexte.

Les sorties rejetées restent visibles avec une comparaison ligne par ligne en lecture seule et une copie après confirmation. Le garde-fou n'applique rien automatiquement. Maintien du streaming et des modèles historiques.

## Livrable et limites
LAB 0.6.6/code19 (installation incrémentale, package et signature inchangés), seulement après CI verte du HEAD. Pas encore de validation matérielle nouvelle. Les trois premières lignes au maximum restent la fenêtre HobbyLM expérimentale ; traitement de nombreuses phrases, EOS certain et continuation avec cache KV conservé sont encore à développer. Aucun merge sur main ni Release autorisés.
