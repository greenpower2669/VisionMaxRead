# VisionMaxRead — carte centrale active (VMR-021, 31 octobre 2026)

**État VMR-021** : branche `feature/vmr021-persistent-ocr-install`, partie service/notifications/transferts modifiée. CI et tests Samsung à qualifier sur le HEAD final ; aucun modèle Nemotron actif.

## Tiroirs (n'ouvrir que celui nécessaire)
- [VMR-021 — état technique et preuves](docs/VMR-021-implementation.md) : service Android foreground, Wi-Fi lié au transport, limites de six heures et pistes Nemotron ONNX/GGUF. Ne confondre source poussée, CI et test Samsung.
- [Architecture et décision VMR-020](docs/VMR-020-auto-models.md) : installation sur Wi-Fi, confidentialité, limites, acteurs.
- [Passation VMR-020](docs/VMR-020-passation.md) : ordre de travail et preuves manquantes.
- `app/src/main/java/fr/visionmaxread/SpecialistOcrAutoInstaller.java` : téléchargement HTTPS, Range, SHA et stockage privé.
- `SpecialistOcrModels.java` et `SpecialistOcrProcess.java` : modèles vérifiés et runtime natif image.
- `MainActivity.java` : interface accessible et concurrence ; `.github/workflows/android.yml` : CI / APK.
- `brain.md` : invariants actifs ; `todo.md` : priorités ; `debughistorical.md` : régressions ; `ordres-de-mission.md` : mandat de Fab.
- [Archives exactes des cinq mémoires précédentes](docs/memory-archives/2026-10-30-pre-vmr020/) : VMR-001 à VMR-019, à consulter seulement pour besoin historique précis.

## Règles de reprise
Preserver Indra, PP-OCR, caméra/photo/texte, Piper/Siwis/Jessica, masquage, copie des traces et certificat LAB ; conserver l'OCR d'origine. Pas d'envoi d'images/texte sur réseau. Seuls les poids publics sont acquis en HTTPS avec accord initial ; OCR hors ligne après installation. **Un seul moteur lourd en RAM à la fois.** Nemotron encore non porté. Ne jamais qualifier CI verte de preuve d'OCR Samsung. Aucun merge main ni Release sans nouvel accord.
