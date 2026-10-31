# VMR-021 — téléchargement OCR durable / audit des portages

État au 31 octobre 2026. Branche dédiée `feature/vmr021-persistent-ocr-install` issue de `b715440c`.
Cible conservée : 0.7.0 / versionCode 23, package LAB et certificat LAB constant.
**Ce document sépare strictement code, CI et tests sur appareil.**

## Implémentation dans cette branche
- Nouveau `SpecialistOcrInstallService` Android (foreground `dataSync`) distinct de l'Activity.
- Notification de progression, bouton Annuler, canal de notification, état technique dans les préférences locales.
- Redélivrance de l'intention `START_REDELIVER_INTENT` après mort de processus lorsque le système l'autorise.
- Reprise de fichiers `.part` et contrôle SHA-256 toujours dans l'installateur VMR-020.
- Transferts liés explicitement au réseau Wi-Fi/Ethernet non facturé vérifié grâce à `Network.openConnection`.
- Pause sur perte Wi-Fi (jusqu'à 2 h dans le service), nouvelles tentatives de connexion, sans basculer en données mobiles.
- `MainActivity` observe l'état sans contrôler la vie du transfert. Une photo déjà à l'écran peut être analysée après installation si la session est toujours valide. **Aucune photo n'est conservée pour reprendre l'OCR après destruction de l'Activity.**
- Installation déclenchée uniquement après confirmation de l'utilisateur. Indra, PP-OCR, OrderNet, Piper, copies et masquage restent sur leur chaîne existante.
- Manifest : `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, service non exporté.
- Pas de modèles embarqués dans l'APK ; poids privés après installation. Un seul processus OCR spécialisé lancé à la fois via l'exécuteur spécialisé existant.

## Limites Android à tester
Sur Android 15+ les services dataSync de premier plan ont une limite cumulée de six heures par vingt-quatre heures lorsque l'application est en arrière-plan. Un arrêt manuel de l'application dans le gestionnaire système n'est **pas** équivalent à fermer l'Activity et peut tuer le service. Les notifications peuvent ne pas figurer dans le volet si l'utilisateur refuse la permission Android 13+. Les restrictions constructeur sur l'énergie et la redélivrance ne sont pas garanties. Pour les transferts sur plusieurs heures, étudier le remplacement par les `User-Initiated Data Transfer` Jobs Android 14+, mieux adaptés et dotés de contraintes système réseau, avec repli pré-34.

Sources :
- https://developer.android.com/develop/background-work/services/fgs/timeout
- https://developer.android.com/develop/background-work/background-tasks/uidt
- https://developer.android.com/develop/ui/compose/notifications/notification-permission

## Surya 2 / LightOn 3 : preuves encore manquantes
1. Installer le nouvel APK sur Samsung SM-A576B, puis acquérir chaque poids modèle et projecteur via Wi-Fi (sans gestion de fichier manuelle).
2. Interrompre puis reprendre après veille, Activity fermée, Wi-Fi coupé, redémarrage processus ; vérifier intégrité SHA, espace et consommation réseau mobile = zéro.
3. Reprendre la **même lettre manuscrite de janvier 1942** et la même transcription humaine de référence, idéalement double vérification humaine des mots incertains.
4. Relever CER et WER avec Levenshtein, puis audit indépendant des noms, dates et numéros, aucune correction silencieuse.
5. Enregistrer durée, RSS/PSS pic, éventuel arrêt OOM, CPU/échauffement et comportement avec écran éteint. Respecter le consentement pour tout partage de logs contenant du texte.

**Aucun essai Samsung n'a été effectué par cette mission.** CI ne valide ni la vitesse ni la qualité ni le bon fonctionnement des fichiers GGUF/mmproj réellement téléchargés.

## Nemotron 3.5 Parse — examen technique
Le modèle NVIDIA officiel (~630,8 M paramètres) associe un encodeur visuel C-RADIOv4-SO400M avec NaViT rectangulaire, un adaptateur et un décodeur mBART 10 couches. Il requiert son propre pré/post-traitement et tokenizer ; il n'entre pas dans le runtime mtmd actuel de Surya/LightOn par simple copie de poids.

Nouveaux portages tiers à auditer avant toute intégration :
- `XDcobra/Nemotron-3.5-Parse-ONNX` : pack INT8 annoncé 753 Mo (vision + decoder + tokenizer + préprocesseur), ORT 1.31 et opset 18. VisionMaxRead est sur ORT Android 1.27 : **compatibilité des opérateurs à vérifier**, sans mettre à jour tout ORT à l'aveugle.
- `SkyContact/Nemotron-3.5-Parse-GGUF` : Q8 annoncé 866 Mo et runtime spécifique `SkyVision.cpp`, **pas** l'exécutable llama-mtmd installé. Auditer prise en charge Linux/Android ARM64, ggml, NDK, coût mémoire et licence.

Sources :
- https://build.nvidia.com/nvidia/nemotron-3.5-parse/modelcard
- https://huggingface.co/XDcobra/Nemotron-3.5-Parse-ONNX
- https://huggingface.co/SkyContact/Nemotron-3.5-Parse-GGUF

Vérifier la version précise des licences : modèle OpenMDW-1.1, tokenizer et éventuelles dépendances tierces ; les cartes NVIDIA et dérivées peuvent différer selon révision. Aucun téléchargement automatique Nemotron ni bouton « prêt » sans exécutable Android vérifié.

## Distribution et contrôles
La CI compile l'APK direct LAB avec `upload-artifact@v7` / `archive: false`; ne pas livrer avant réussite du **HEAD exact** et contrôle de signature constant. La publication Play demandera une étude AAB / Play Asset Delivery install-time, distincte du paquet APK LAB. Pas de Release, pas de merge main.

## Tableau des preuves
| Critère | État |
| --- | --- |
| Code service/notification/réseau | Ajouté en branche VMR-021 |
| VMR-020 CI sur son HEAD | Confirmé vert : run 41135591282 |
| CI HEAD final VMR-021 | À relever après derniers commits |
| Téléchargement réel des poids | Non testé |
| Surya OCR Samsung | Non testé |
| LightOn OCR Samsung | Non testé |
| Nemotron 3.5 Parse Android | Non porté / non testé |
| CER/WER sur manuscrit 1942 | Non mesuré |
| Play Asset Delivery | Étude en attente |
