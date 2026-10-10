# Historique de diagnostic

## VMR-D001 — Initialisation, 11 octobre 2026
Dépôt vierge de code, seul LICENSE présent. Aucun bug applicatif préexistant à corriger. La taille annoncée de 31 Mo concerne seulement les modèles OCR Small, sans runtime ni voix ni NER. Reconnaissance du manuscrit français non validée. L’OCR photographique et la reconnaissance des traits du stylet sont des fonctions différentes.

Ne jamais annoncer une APK ou une reconnaissance téléphone validée sans résultat de construction ou test réel. Ne jamais sauvegarder le texte brut après un échec du masquage.

## VMR-D002 — Construction locale
SDK/Gradle Java ne reprennent pas les variables proxy de cette session. Téléchargements Python officiels réussis. SDK installé depuis repository2-3.xml ; Gradle utilise les propriétés JVM du proxy local, hors dépôt.
