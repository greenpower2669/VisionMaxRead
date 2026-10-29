# VisionMaxRead VMR-009 — Essai natif Indra OCR Latin

Date : 29 octobre 2026. Branche : feature/vmr009-indra-ocr-latin-test, dérivée de VMR-008, sans fusion main.

## Intention
Sur la même image figée, conserver Qwen3.5 comme moteur OCR automatique et les boutons PP-OCRv6-Small et PaddleOCR-VL-1.6, et ajouter un bouton « Indra OCR (latin) / Comparer ». Comparer la sortie, la durée et la PSS totale du processus. Le texte reste en mémoire jusqu'au choix explicite de le lire ou le corriger ; aucune sortie brute en stockage/log ou réseau.

## Réalisation expérimentale
Portage Java/ONNX Runtime Android (pas de Python sur l'appareil) des blocs Indra :
- det.onnx : image gris, encodage des pixels en carte d'encre, long côté 1024, padding à 32 ; composantes connexes et boîtes de lignes.
- rec.onnx : fenêtres de lignes (base 32 px, hauteur de fenêtre depuis meta.json, 3 par défaut), padding latéral, CTC avec sorties ids/conf/lengths, seuil 0,5.
- meta.json : vocabulaire de décodage chargé dynamiquement.
- Poids issus du dépôt officiel datalab-to/indra-ocr, revision Hugging Face bd02167fdf2005a19962cc15188f8496a2c87d3b. Téléchargement pendant le build seulement ; empreintes SHA256 en manifeste embarqué contrôlées à l'extraction.
- Licence code Apache 2.0 ; poids modifiés OpenRAIL-M, restrictions commerciales spécifiques (consulter la licence officielle incluse).

Limites : ce portage n'est PAS le pipeline Indra complet ; il ne couvre ni l'orientation, ni le redressement, ni les scripts autres que latin, ni modèle de langage, ni reconstruction de colonnes, de tableaux, de titres ou de formules. Le redimensionnement simplifié peut réduire la précision ; une validation terrain est indispensable. Les SHA de référence indépendants du téléchargement doivent encore être épinglés.

## Protocole sur Samsung
Après une CI Android verte vérifiée, installer l'APK LAB signé. En mode avion, importer successivement 3 images non sensibles : imprimé avec accents, page à colonnes, écriture manuscrite. Appuyer successivement sur PP-OCR et Indra sur la même image après Qwen. Lire les métriques (ms, caractères, PSS processus) et comparer les textes ; la longueur ne démontre pas l'exactitude. Tester la lecture audio et le changement de document pendant les calculs. Si Indra échoue, le lecteur et les autres OCR doivent rester utilisables.

## À finir/valider
- [ ] CI Android Gradle+lint, vérifier tous assets et APK installable ARM64 sans INTERNET.
- [ ] Test Samsung : vraie reconnaissance, rotation, mode avion et absence de fuite mémoire.
- [ ] Fixer SHA256 upstream immuables en amont de la construction.
- [ ] Évaluer le pipeline complet (orientation, ordre, LM) si l'essai apporte un bénéfice.
- [ ] Aucune Release, aucun merge main sans accord distinct.
