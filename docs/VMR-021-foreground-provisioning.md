# VMR-021 — service Android de préparation des OCR (31 octobre 2026)

## Portée et preuves

Source de départ vérifiée : branche feature/vmr020-auto-provision-ocr, HEAD b715440c16fc8b667db3b1abca390cdece0c4fc9. CI de ce commit : https://github.com/greenpower2669/VisionMaxRead/actions/runs/41135591282 — success. La CI VMR-020 ne prouve ni téléchargements réels ni OCR Samsung.

Branche de travail : feature/vmr021-background-model-installer, sans fusion main, sans Release ni Play. LAB 0.7.0, versionCode 23, versionName 0.7.0-vmr021-bg-installer-lab.

### Développé en source — CI VMR-021 à vérifier

- SpecialistOcrDownloadService.java : service non exporté de premier plan dataSync, notification de progression/annulation, installation après confirmation dans MainActivity. Un exécuteur unique découple l'acquisition de la destruction de l'Activity ; état, choix des moteurs, progrès et statut final conservés dans des préférences privées. START_REDELIVER_INTENT permet une reprise lorsque le système autorise la redélivrance ; un force-stop utilisateur n'est jamais une garantie de reprise.
- Aucune image, URI de document ou transcription transmise au service. Poids conservés dans files/ocr-specialists ; un seul moteur téléchargé à la fois.
- Wi-Fi non facturé vérifié au début, sur les redirections et pendant le transfert. Network.openConnection borne la connexion au réseau contrôlé. Conservation des fragments .part, reprise HTTP Range et HTTP 416, limite de taille/espace libre et SHA-256 avant validation locale.
- Cinq réessais temporisés des exceptions I/O, aucun basculement de transfert sur réseau mobile. Annulation garde les fragments ; Service.onTimeout gère le quota Android 15+.
- MainActivity : autorisation facultative des notifications Android 13+, suivi du statut, bouton d'arrêt, OCR après installation seulement si l'image précédente est encore le document courant. La destruction de l'Activity n'arrête plus volontairement le téléchargement. Indra, PP-OCR, Piper, caméra, copie, historique masqué et signature LAB préservés.
- Scripts test_vmr020_contract.py actualisé et test_vmr021_contract.py créé. CI doit lancer le nouveau contrat, Gradle, lint, assembleDebug, puis vérifications APK existantes.

**Ne jamais confondre : code soumis, compilation verte, poids réellement installés sur Samsung, OCR réellement exécuté sur Samsung.**

## Matrice de validation physique

1. Vérifier versionCode 23, identité fr.visionmaxread.labdebug, certificat LAB et hash APK.
2. Installer depuis APK direct ; autoriser notification ; télécharger Surya et LightOn sans transfert manuel de GGUF. Contrôler HTTPS HF/Xet/CloudFront, Range/206/416, taille, empreinte, état et espace libre.
3. Tester écran éteint, fermeture de l'Activity, rotation, Wi-Fi coupé/rétabli, bascule en mobile, reprise, annulation, volume, stockage presque plein et kill du processus. Les restrictions Android et force-stop restent explicites.
4. Tester sur un même cliché manuscrit français de janvier 1942. Mesurer transcription de référence relue, CER/WER, erreurs de noms/dates/numéros, latence, PSS/RSS et chauffe. La comparaison des propositions ne remplace jamais silencieusement le texte d'origine.
5. Tester mode avion après l'installation, la voix et l'absence d'envoi des documents/texte sur réseau ; anonymiser tout export de journal.

## Nemotron 3.5 Parse — portage non effectué

NVIDIA décrit C-RADIOv4-SO400M, prétraitement NaViT rectangulaire, adaptateur convolutif, décodeur causal mBART 10 couches à préfixe visuel, tokenizer multilingue de 72 505 tokens et sorties de zones structurées. Le fichier model.safetensors publié est d'environ 2,52 Go avant quantification. L'ancien mtmd GGUF ne prend pas en charge cette architecture telle quelle.

Projet de portage distinct : épingler révision source/licence/empreintes, exporter prétraitement NaViT et encoder+adapter+decoder, étudier ONNX ARM64 et cache KV, quantifier avec tests numériques, gérer tokenizer et sortie structurée, mesurer mémoire/temps/qualité sur Android. Licence modèles OpenMDW 1.1 ; consulter notices précises du tokenizer pour la révision choisie. Ne rien marquer « fonctionnel » sans reconnaissance physique.

Sources : https://huggingface.co/nvidia/Nemotron-3.5-Parse et https://build.nvidia.com/nvidia/nemotron-3.5-parse/modelcard

## Play Asset Delivery — étude seulement

Les packs install-time sont livrés avec l'application Play comme split APK et lisibles via AssetManager dès l'installation ; ils ne sont pas effaçables isolément. Analyser contraintes de taille actuelles, stockage temporaire et couverture APK versus AAB. L'APK direct continue à préparer les poids par téléchargement contrôlé.

Sources : https://developer.android.com/guide/playcore/asset-delivery et https://developer.android.com/guide/playcore/asset-delivery/integrate-java

## Limites Android

Depuis Android 15, le service foreground dataSync est limité à six heures cumulées sur 24 heures en arrière-plan, avec callback onTimeout. Une impossibilité de redémarrage est possible après épuisement du quota. Une stratégie WorkManager/DownloadManager durable sera à comparer selon les essais Samsung.

Source : https://developer.android.com/develop/background-work/services/fgs/timeout

## Livraison

Pas d'APK VMR-021 annoncé avant CI du HEAD exacte verte, artefact APK direct et signature vérifiée. Pas de fusion main, Release ou publication Play sans nouvel accord. Les résultats Samsung demeurent non vérifiés tant qu'aucun essai physique n'a été fourni.
