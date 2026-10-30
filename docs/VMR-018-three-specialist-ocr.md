# VMR-018 — Les trois OCR ciblés, sans LLM textuel (30/10/2026)

Source : branche VMR-017, HEAD `96d241d64b0fc94dc118bdf7d273adbfe0abc451`, isolée de main.

## Objectif
Remplacer le parcours génératif Liquid / Qwen3 textuel / HobbyLM par trois OCR spécialisés :
- Surya OCR 2 — OmniParseBench 83,1, ~670 M paramètres.
- LightOnOCR-3 0.8B — 81,6, ~850 M.
- Nemotron 3.5 Parse — 80,0, ~631 M.

Référence commune (texte, tableaux, mise en page ; **pas** test Android ni de manuscrit français) :
https://app.datalab.to/blog/open-ocr-models-omniparsebench (23 octobre 2026).

## Migration réalisée
- UI et handlers de correction générative retirés de MainActivity, ainsi que sources Java des moteurs LLM non nécessaires.
- CI supprime le téléchargement des GGUF Liquid 230M et HobbyLM 1B, la compilation de leurs JNI et les tests liés à ces moteurs.
- Le contrôle APK rejette la présence d'assets ou binaires génératifs hérités.
- Sont préservés : OCR Indra et correction n-gramme CTC, ordre OrderNet/XY-cut, secours PP-OCRv6-Small, caméra, texte, synthèse vocale Piper, anonymisation/historique, journaux techniques.
- Le catalogue des trois moteurs est **une préparation**, ils ne sont pas présentés comme exécutables et ne produisent aucun faux OCR.

## Portages encore nécessaires
1. Surya 2 : son exécution de référence utilise une entrée image et llama.cpp/vLLM ; adapter au multimodal Android ARM64, à sa sortie HTML, à sa licence de poids OpenRAIL-M modifiée. https://github.com/datalab-to/surya
2. LightOnOCR-3 : importer les poids et projecteur vision, verrouiller SHA, construire un runtime llama.cpp multimodal effectivement compatible et vérifier résolution et mémoire. https://github.com/lightonai/LightOnOCR
3. Nemotron : architecture C-RADIO/NaViT + décodeur mBART ; vérifier export et portage Android et conditions OpenMDW-1.1. https://huggingface.co/nvidia/Nemotron-3.5-Parse

## Validation impérative
- [ ] Adapter et compiler chaque moteur OCR réel sur ARM64 ; pas de serveur ni INTERNET, un moteur chargé à la fois.
- [ ] Mesures sur le même bitmap : CER/WER sur documents français manuscrits/anciens, lignes, tableaux, chiffres, références.
- [ ] RAM PSS/RSS, délais, chauffe, annulation/cancellation, vie privée et reprise.
- [ ] Tester CI (APK signé, vérification manifeste, AAB si demandé) et Samsung avant toute annonce de disponibilité.
- [ ] **Ne pas merger main ni publier Release sans accord explicite.**
- [ ] Anciennes copies GGUF privées : ne jamais supprimer sans confirmation de Fab.

État : retrait de la chaîne générative et catalogue en source, **pas** trois OCR Android fonctionnels, **pas** d'APK publié.
