# VisionMaxRead — contrat

Application Android native d’aide à la lecture française, version initiale 0.1.0. Android 8+ ; ARM64 ; cible Android 16. La caméra aide à viser ; le bouton rouge prend une photo JPEG distincte de meilleure résolution puis gèle l’affichage. Import d’image par le sélecteur Android ; texte éditable et collable ; bouton Lire/Arrêter explicite.

Reconnaissance locale : PaddleOCR PP-OCRv6 Small, SDK Android ONNX officiel adapté. Le manuscrit français reste à évaluer sur téléphone. Lecture locale : Supertonic 3, français, voix F1 par défaut, seed 2026, choix F1/M1 et vitesse dans les réglages. Aucun remplacement silencieux par une autre voix.

Historique : seulement le texte masqué par NER multilingue (PER/LOC/ORG) et règles (mail, téléphone, IBAN, identifiants, adresses et dates), dans le stockage privé sans sauvegarde cloud. Originaux photo/texte non persistés. Si l’IA de masquage échoue, aucune entrée brute n’est sauvegardée. L’utilisateur peut relire, supprimer l’historique et le vider. Aucun corpus réel dans Git, logs ou tests.

L’application ne demande pas INTERNET. Modèles téléchargés uniquement par la chaîne de construction, identifiés par révisions et SHA-256. Icône vectorielle propre au projet. Modèles et binaires générés non commis dans Git.

État initial : seul LICENSE existait au commit main 3b0b10a9dc16ef0d2e8ff3e1f9bf4b8e1e34d05e. Implémentation et compilation en cours ; aucune validation téléphone annoncée.

## Réalisation 0.1.0
Code caméra/OCR/voix/NER/historique créé sur feature/android-offline-reader-v1. 38 contrôles du noyau passent après observation de l’échec initial. Modèles verrouillés : 609 645 027 octets au total. Cette taille concerne tous les modèles ; prévoir environ 2 Go libres. Aucune validation sur téléphone pour l’instant.
