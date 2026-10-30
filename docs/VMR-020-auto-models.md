# VMR-020 — installation automatique des OCR spécialisés (30 octobre 2026)

## Décision utilisateur
Fab ne veut **ni télécharger lui-même les poids GGUF ni importer des fichiers manuellement**. VisionMaxRead doit gérer le stockage privé, le téléchargement initial, l'intégrité et les reprises. « En mémoire » signifie modèles conservés sur stockage local et **chargement RAM d'un seul moteur à la fois**, pas 2 à 4 Go de poids simultanément en RAM.

## Décision technique / compromis
La base APK embarquant Indra/Paddle et le runtime natif pèse environ 396 Mo. Surya (GGUF Q5 + projecteur) représente environ 1,5 Go ; LightOn Q4 + projecteur environ 0,75 Go. Ajouter les deux au même APK créerait un livrable difficile à distribuer, installer et mettre à jour. Nemotron (~631 M paramètres) a un format et runtime C-RADIO/NaViT/mBART encore incompatibles avec le moteur mtmd du projet.

**VMR-020 intègre une installation automatique et volontaire, contrôlée par l'application sur Wi-Fi non facturé, sans aucune manipulation de fichiers.** Les poids authentifiés restent dans \`files/ocr-specialists/ENGINE/\` ; inférence locale après installation. Internet sert uniquement à acquérir les poids. L'ancienne promesse « zéro permission INTERNET » ne s'applique donc plus à cette branche. Aucun document OCR, photo, résultat, historique ou masquage n'est envoyé.

L'APK contient le moteur d'inférence, **pas** les poids de Surya et LightOn. Un bouton prépare les deux à la fois ; tenter une analyse avec un modèle absent propose son installation automatique. L'utilisateur confirme avant le trafic initial (Wi-Fi et espace de stockage). Les téléchargements sont séquentiels et reprennent des fichiers .part interrompus avec Range + Content-Range, en gardant une réserve de 300 Mio de stockage. Digest SHA-256 entier et magic GGUF vérifiés avant renommage atomique, puis digest revalidé avant exécution. Liens HTTPS vers HuggingFace et CDN HF/CloudFront, jamais de POST de document.

### Code modifié
- \`SpecialistOcrAutoInstaller.java\` : téléchargements automatiques, Wi-Fi non facturé, taille maximale, contrôles réseau, interruptions, redirections HTTPS limitées, SHA-256 et installation privée.
- \`MainActivity.java\` : commande « Préparer mes OCR automatiquement », suppression de l'interface navigateur/import manuel, confirmation, progression et arrêt, réutilisation des poids installés, effacement volontaire par modèle.
- \`SpecialistOcrModels.java\` : références exactes des deux modèles et projecteurs, SHA256, stockage privé.
- \`AndroidManifest.xml\` : INTERNET et ACCESS_NETWORK_STATE pour modèles uniquement, cleartext interdit.
- CI : tests statiques, inspection du manifeste APK, version LAB 0.7.0/code 23 et certificat continu.

### Périmètre réel et limites
- Portages natifs Surya 2 / LightOnOCR : code image + CLI mtmd compilable, mais sans **preuve d'inférence sur le Samsung** ; téléchargement de plusieurs Go et OCR sur manuscrits FR à mesurer.
- Nemotron 3.5 Parse : **non intégré ni téléchargé**, car modèle C-RADIO/NaViT + décodeur mBART à porter. Il ne faut pas le présenter comme prêt.
- La tâche de téléchargement vit dans un exécuteur de l'Activity. **Elle ne survit pas à la destruction du processus.** La reprise des fichiers partiels est prévue, mais une notification / service foreground ou un WorkManager fiable reste à créer et à tester.
- Détection et interruption des transferts, permissions / politique de vie privée, Wi-Fi avec VPN, HTTP Range ignoré par serveur, stockage saturé, checksum mauvais et install après redémarrage restent à tester.
- Contraintes Google Play : base 500 Mo compressés, packs install-time, cumul 4 Go pour base + packs install-time. Étudier Play Asset Delivery pour offre Play Store sans étape de téléchargement séparée ; pour l'APK direct utiliser l'autoinstallateur. Source : https://support.google.com/googleplay/android-developer/answer/9859372?hl=en

### Sécurité / conservation
Les documents peuvent être anciens et sensibles : confidentialité strictement locale, logs sans texte, exports confirmés, mode avion garanti **après** installation, aucun téléchargement sur données mobiles par défaut. Conserver Indra/PP-OCR, voix Piper, masque, historique et signature stable. Ne pas fusionner main, ne pas publier de Release sans accord.

### Prochain ordre
1. Valider CI **sur le HEAD exact** puis tester le flux de téléchargement réel sur le Samsung.
2. Corriger tout échec HTTPS/Xet/Range/SHA et assurer téléchargements persistants en arrière-plan via service foreground / WorkManager avec notification et reprise.
3. Confirmer une **vraie** reconnaissance de la lettre manuscrite 1942 pour les deux moteurs, sorties propres sans bruit CLI, révision/template de modèle et mesures PSS, latence, RAM, chauffe et CER/WER.
4. Porter Nemotron (source/fichiers licenciés, tokenizer, NaViT et mBART, ONNX ARM64 / runtime compatible), avec tests réels et estimation stockage / RAM.
5. Pour une distribution sans installation initiale dans l'app, concevoir des packs install-time Play Asset Delivery AAB ; vérifier poids, licences et contraintes, ne pas prétendre que l'APK direct inclut les poids.
6. Mettre à jour preuve CI + tests dans les mémoires vivantes et livrer APK direct uniquement quand compilé et signé.
