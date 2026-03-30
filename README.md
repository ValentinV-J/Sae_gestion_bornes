# Application Mobile Android — Bornes Dépôt-Retrait de Colis

Application Android native en **Kotlin** pour les livreurs. Permet de scanner le QR code d'un colis et de confirmer son arrivée à une borne.

## Architecture

```
MobileApp/
├── app/src/main/java/fr/iutbm/bornes/mobile/
│   ├── MainActivity.kt          # Accueil (scan + paramètres)
│   ├── ScanActivity.kt          # Scanner QR (CameraX + ML Kit)
│   ├── DepotActivity.kt         # Vérification UUID + confirmation dépôt
│   ├── SettingsActivity.kt      # Config URL serveur
│   ├── api/
│   │   ├── ApiClient.kt         # Singleton Retrofit (URL configurable)
│   │   ├── ApiService.kt        # Interface Retrofit (2 routes)
│   │   └── model/ColisResponse.kt
│   └── scanner/
│       └── QrCodeAnalyzer.kt    # CameraX ImageAnalysis.Analyzer
└── app/src/main/res/
    ├── layout/ (4 layouts XML)
    ├── values/ (strings, colors, themes)
    ├── drawable/scan_frame_border.xml
    └── xml/network_security_config.xml
```

## Stack Technique

| Composant | Bibliothèque | Version |
|---|---|---|
| QR Scan | CameraX + Google ML Kit Barcode | 1.3.1 / 17.2.0 |
| HTTP | Retrofit + Gson Converter | 2.9.0 |
| Async | Kotlin Coroutines | 1.7.3 |
| UI | XML Layouts + ViewBinding | — |

## Flux livreur

```
[Accueil] → "Scanner un colis"
    ↓
[Scanner] — CameraX + ML Kit → QR détecté → UUID extrait
    ↓
[Dépôt]  — GET /api/colis/:uuid → Colis trouvé (ATTENTE_DEPOT)
         → Bouton "Confirmer arrivée"
         — PATCH /api/colis/:uuid/scan → statut = SCAN_MOBILE_OK
         → ✅ "Présentez votre badge RFID à la borne"
```

## Configuration et lancement

### Prérequis
- Android Studio Hedgehog (2023.1+)
- SDK Android 26+

### Ouvrir le projet
1. **File → Open** → sélectionner `MobileApp/`
2. Android Studio télécharge automatiquement Gradle + dépendances
3. **Run** sur émulateur (API 26+) ou appareil physique

### Configurer l'URL du serveur
- Sur **émulateur Android** : `http://10.0.2.2:3000/api/` (défaut)
- Sur **appareil physique** : remplacer `10.0.2.2` par l'IP locale du PC serveur
- Modifier via bouton **Paramètres** dans l'application

## Notes techniques

- **Permissions** : `CAMERA` demandée au runtime, `INTERNET` dans le manifest
- **HTTP local** : autorisé via `network_security_config.xml` (dev only)
- **Verrou scan** : `QrCodeAnalyzer` bloque les doublons avec flag `processing`
- **Coroutines** : tous les appels API via `lifecycleScope.launch { }`, thread-safe