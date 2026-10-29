# VMR-011 — Corriger l'ordre des colonnes et activer le LM officiel Indra

Date : 29/10/2026. Branche : `feature/vmr011-indra-layout-lm`, dérivée de la LAB VMR-010. Le résultat du test Samsung fourni par Fab (96 lignes, 2025 caractères, P1 574 ms, P2 94 ms, 341 Mo PSS) a montré un ordre imparfait sur une page de magazine avec colonnes/encadrés et un OCR imparfait.

## Corrections de lecture
- `IndraReadingOrder.rank` implémente maintenant le décodeur de l'ordre de lecture officiel `decode_order` : scores `logsigmoid` et quatre balayages d'insertion pour déplacer un bloc entier avant/après les autres, plutôt qu'un tri Borda naïf.
- Conserver **deux reconstructions** indépendantes : OrderNet (mise en page de Datalab) et XY-cut (géométrie). Quatre versions consultables et sélectionnables dans l'interface : OrderNet + LM, XY-cut + LM, OrderNet brut, XY-cut brut. Aucun ordre n'est présenté comme forcément meilleur avant vérification manuelle.
- Export volontaire du rapport avec coordonnées normalisées (0–1000) de chaque boîte de ligne et permutations d'indices de lecture. Le journal standard n'enregistre pas le texte original et tronque les détails de l'étape pour préserver sa taille.

## Correcteur linguistique réel
- `lm.npz` original (modèle n-gramme latin d'Indra, **15,7 Mo**) épinglé au commit upstream `bd02167f...`, SHA256 `6c0fb618a363a0acdd05d4f5b0edd9e8146d2d990541b87f6130bbfe17cea58d` ; téléchargé uniquement au build, absence de permission INTERNET sur l'APK.
- `IndraNgramLM.java` parse les archives `.npz` et tableaux NPY (versions/dtypes encadrés) et applique le backoff original. `IndraCtcBeam.java` implémente le CTC préfixe avec un faisceau de 8, `alpha=0.5`, `beta=1.5`, seuil candidats 0.003 (sauf réglages officiels `lm_params.base`).
- Les lignes incertaines (confiance 0,5 à 0,9) utilisent les sorties `blank/topv/topi` du reconnaisseur `rec.onnx`; l'original est conservé si la correction échoue, et le journal affiche nombre de lignes retraitées, modifiées et erreurs de repli.
- Le résultat final avant TTS est Indra + LM avec ordre OrderNet par défaut, mais on peut choisir XY-cut + LM manuellement. Le P5 est toujours un arbitrage **non neuronal**, pas une inférence Liquid ni MobileMoE.
- Tests : `scripts/test_indra_lm_java.py` fabrique un modèle NPY/NPZ synthétique et teste le scoring bigramme/backoff ainsi qu'une mini transcription beam ; `scripts/verify_apk.py` vérifie l'intégrité SHA du LM dans l'APK.

## Validation attendue
- Aucun benchmark de précision Samsung nouveau tant que Fab n'a pas installé la nouvelle LAB ; les essais Android sur la **même page complexe** permettront de comparer P2, P2A et P2B (qualité, CER/WER si référence humaine, continuité des colonnes).
- Le modèle n-gramme peut améliorer une hésitation OCR, mais ne peut pas restaurer un mot non proposé ni reconstruire une phrase trop abîmée. Liquid/MobileMoE restent non portés.
- En mode avion : image, bouton « Passes Indra · rapport », « Comparer les ordres », voix et option d'export détaillé. Mesurer PSS/latence, ne pas confondre latence totale OCR et portion LM.
- Garder la signature LAB stable et package `fr.visionmaxread.labdebug` code 12. APK direct non ZIP uniquement après CI verte. Pas de merge main ni de Release sans accord.
