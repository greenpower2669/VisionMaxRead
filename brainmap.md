# VisionMaxRead — carte centrale active (VMR-020, 30 octobre 2026)

**État** : branche `feature/vmr020-auto-provision-ocr` ; version cible 0.7.0 / code 23 ; CI du nouveau HEAD à vérifier ; ni `main` ni Release modifiés. Avant VMR-020, VMR-019 0.6.9/code22 avait une CI verte (run 41117112603), sans essai de reconnaissance Surya/LightOn sur Samsung.

## Tiroirs (n'ouvrir que celui nécessaire)
- [Architecture et décision VMR-020](docs/VMR-020-auto-models.md) : installation sur Wi-Fi, confidentialité, limites, acteurs.
- [Passation VMR-020](docs/VMR-020-passation.md) : ordre de travail et preuves manquantes.
- `app/src/main/java/fr/visionmaxread/SpecialistOcrAutoInstaller.java` : téléchargement HTTPS, Range, SHA et stockage privé.
- `SpecialistOcrModels.java` et `SpecialistOcrProcess.java` : modèles vérifiés et runtime natif image.
- `MainActivity.java` : interface accessible et concurrence ; `.github/workflows/android.yml` : CI / APK.
- `brain.md` : invariants actifs ; `todo.md` : priorités ; `debughistorical.md` : régressions ; `ordres-de-mission.md` : mandat de Fab.
- [Archives exactes des cinq mémoires précédentes](docs/memory-archives/2026-10-30-pre-vmr020/) : VMR-001 à VMR-019, à consulter seulement pour besoin historique précis.

## Règles de reprise
Preserver Indra, PP-OCR, caméra/photo/texte, Piper/Siwis/Jessica, masquage, copie des traces et certificat LAB ; conserver l'OCR d'origine. Pas d'envoi d'images/texte sur réseau. Seuls les poids publics sont acquis en HTTPS avec accord initial ; OCR hors ligne après installation. **Un seul moteur lourd en RAM à la fois.** Nemotron encore non porté. Ne jamais qualifier CI verte de preuve d'OCR Samsung. Aucun merge main ni Release sans nouvel accord.
