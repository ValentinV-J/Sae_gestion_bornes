# SAE Gestion Bornes

Ce dépôt contient deux composants du projet de gestion de bornes de dépôt-retrait de colis :
- `CentralServer/` : serveur TCP Java pour la logique borne/colis.
- `MobileApp/` : application Android Kotlin pour les livreurs.

## CentralServer — Serveur de Centralisation des Bornes

Serveur TCP multi-threadé en Java pour gérer les communications entre les bornes de dépôt-retrait de colis (µC) et la base de données MongoDB (directement ou via l'API Node.js).

### Architecture

```
CentralServer/
├── server/src/
│   ├── BornesCentralServer.java   # Point d'entrée (main)
│   ├── MainServer.java            # ServerSocket, boucle d'acceptation
│   ├── ThreadServer.java          # Thread par connexion µC (logique métier)
│   ├── DataExchanger.java         # Conteneur des deux drivers
│   ├── DataDriver.java            # Interface d'accès BdD
│   ├── MongoDataDriver.java       # Accès direct MongoDB (Java driver)
│   ├── HttpDataDriver.java        # Accès via API Node.js (HttpClient)
│   ├── Colis.java                 # POJO collection 'colis'
│   ├── Livreur.java               # POJO collection 'livreurs'
│   └── Evenement.java             # POJO collection 'evenements'
└── client/src/
    ├── BornesClient.java          # Point d'entrée client de test
    └── MainClient.java            # Client TCP interactif (simule un µC)
```

### Protocole TCP

Les messages sont des chaînes de texte, **une par ligne**, terminée par `\n`.

#### µC → Serveur

| Commande | Paramètres | Description |
|---|---|---|
| `BADGE_RFID` | `borneId idRfid` | Livreur présente son badge |
| `DEMANDE_DEPOT` | `borneId` | Demande le n° de casier à ouvrir |
| `CODE_RETRAIT` | `borneId codeRetrait` | Client tape son code |
| `CASIER_FERME` | `borneId numeroCasier contexte` | Casier refermé (depot\|retrait) |
| `PROBLEME_OUVERTURE` | `borneId numeroCasier contexte` | Problème d'ouverture |
| `BUZZER_TIMEOUT` | `borneId numeroCasier contexte` | Délai buzzer écoulé |

#### Serveur → µC

| Réponse | Description |
|---|---|
| `OK RFID <nom> <prenom>` | Badge RFID valide |
| `OK CASIER <numero>` | Numéro de casier à ouvrir |
| `OK` | Accusé de réception générique |
| `ERR <message>` | Erreur avec description |

### Lancement rapide

```bash
# Serveur
BornesCentralServer 9000
# ou avec URLs personnalisées :
BornesCentralServer 9000 http://localhost:3000/api mongodb://localhost:27017

# Client de test
BornesClient localhost 9000
```

## MobileApp — Application Mobile Android

Application Android native en **Kotlin** pour les livreurs. Elle permet de scanner le QR code d'un colis et de confirmer son arrivée à une borne.

### Architecture

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

### Configurer et lancer

1. Ouvrir `MobileApp/` dans Android Studio.
2. Laisser Gradle télécharger les dépendances.
3. Lancer sur émulateur (API 26+) ou appareil physique.

URL API par défaut sur émulateur : `http://10.0.2.2:3000/api/`.
