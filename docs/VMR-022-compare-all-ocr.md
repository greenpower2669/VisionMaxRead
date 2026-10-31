# VMR-022 — Comparer tous les moteurs OCR testables (31/10/2026)

## Pourquoi cette correction ?

Fab souhaite **essayer tous les moteurs OCR disponibles sur la même photo**, sans manipuler les GGUF, sans redémarrer chaque test à la main et sans chargement simultané de deux moteurs lourds. L'interface précédente imposait plusieurs sous-menus et ne disposait pas d'un test global.

## Ce qui est codé

Branche : `feature/vmr021-background-model-installer` (suite non fusionnée de VMR-020/021).
Version LAB de validation : 0.7.1, **code 24** ; certificat LAB historique inchangé.

- Bouton **« Tester tous les OCR · même photo »** + entrée correspondante dans la liste OCR.
- Moteur Indra + OrderNet/LM : résultat déjà produit sur le même bitmap quand la photo a été préparée.
- Si les poids de LightOnOCR-3 0.8B et/ou Surya OCR 2 manquent, demande unique d'installation automatique. Le service de téléchargement VMR-021 est réutilisé, sans import de fichier.
- Après installation, test séquentiel sur **le même bitmap** : Indra de référence, PP-OCRv6-Small, LightOn, Surya. Les modèles lourds ne sont jamais lancés simultanément par cette comparaison.
- Liste des résultats : nom, nombre de caractères, temps d'inférence lorsque disponible ou erreur. Choisir un résultat ouvre la fenêtre de comparaison existante avec **« Utiliser ce texte »** explicite. Bouton **« Voir les résultats de comparaison »** tant que le document est courant.
- Gestion des annulations, changement de photo, confidentialité des logs et état. Historique masqué et voix conservés. Les résultats OCR sont en RAM de l'application, jamais téléchargés ni envoyés.
- Nemotron est indiqué honnêtement **« portage Android encore nécessaire »** dans la liste : pas de faux texte et aucun gros téléchargement préventif.

### Limites exactes

**Le code et les tests statiques ne constituent pas une preuve de fonctionnement des OCR sur le téléphone.** Le pipeline GitHub compile le binaire et l'APK ; sans essai physique on ne peut annoncer ni texte fidèle ni température/RAM acceptable.

- La comparaison attend la fin d'Indra après sélection d'une photo ; réappuyer si Indra travaille encore.
- Si la préparation a été interrompue par le système, il suffit de relancer « Tester tous » dans la photo encore ouverte pour réutiliser les poids téléchargés.
- LightOn et Surya sont susceptibles d'échouer lors de l'inférence ARM64, notamment à cause de la mémoire et des prompts ; ces échecs seront affichés et non remplacés par un texte inventé.
- Avec le téléphone en mode avion, les poids doivent être installés et authentifiés préalablement.
- En cas de RAM insuffisante, analyser les logs techniques et privilégier les modèles un par un, sans dégrader Indra.

### Nemotron 3.5 Parse — état scientifique actualisé

La publication tierce `XDcobra/Nemotron-3.5-Parse-ONNX` décrit un pack mobile INT8 (~753 Mo) avec sous-graphes ONNX (`patch_embed`, `vision_encoder`, `embed_tokens`, `decoder_model_merged`), positions NaViT, tokenizer et cache KV. C'est une piste concrète permettant peut-être un portage raisonnable. L'auteur indique des écarts de tokens sur les profils de mise en page, et un runtime ONNX **1.31**, alors que VisionMaxRead dépend d'ONNX Runtime Android **1.27.0**. Il faut confirmer l'origine, la licence OpenMDW, les SHA, les entrées/sorties et la compatibilité binaire avant téléchargement automatique et inférence locale.

Ce pack **n'est pas intégré** dans VMR-022. Son existence sur Hugging Face ne prouve pas son fonctionnement sur le Samsung SM-A576B.

Sources :
- https://huggingface.co/XDcobra/Nemotron-3.5-Parse-ONNX
- https://huggingface.co/nvidia/Nemotron-3.5-Parse
- https://developer.android.com/develop/background-work/services/fgs/timeout

## Vérifications à faire après compilation

1. CI du HEAD VMR-022 verte, `test_vmr022_contract.py` + contrats VMR-018/019/020/021 verts, lint, APK signé, SHA et package `fr.visionmaxread.labdebug` / code24.
2. Installation sur Samsung, photo de la lettre de janvier 1942, Indra terminée ; touche « Tester tous » ; autorise notification, prépare automatiquement les poids sur Wi-Fi, contrôle reprise si interruption.
3. Sur même image, vérifier *réellement* les sorties de PP-OCR, LightOn et Surya, limites de durée et disponibilité de la liste. Vérifier que l'ancien texte reste intact jusqu'à acceptation explicite.
4. Référence humaine : calculer CER/WER, erreurs de dates/noms/numéros, chronométrage, PSS/RSS du processus natif, chauffe, usage en mode avion, arrêt/changement de document.
5. Étudier et porter Nemotron ONNX en tâche distincte avant de proposer « tester les cinq ».

Aucune fusion main, Release ni publication Play Store sans accord de Fab. APK direct seulement après compilation et contrôles.
