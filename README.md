# CentralServer — Serveur de Centralisation des Bornes

Serveur TCP multi-threadé en Java pour gérer les communications entre les bornes de dépôt-retrait de colis (µC) et la base de données MongoDB (directement ou via l'API Node.js).

## Architecture

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

## Protocole TCP

Les messages sont des chaînes de texte, **une par ligne**, terminée par `\n`.

### µC → Serveur

| Commande | Paramètres | Description |
|---|---|---|
| `BADGE_RFID` | `borneId idRfid` | Livreur présente son badge |
| `DEMANDE_DEPOT` | `borneId` | Demande le n° de casier à ouvrir |
| `CODE_RETRAIT` | `borneId codeRetrait` | Client tape son code |
| `CASIER_FERME` | `borneId numeroCasier contexte` | Casier refermé (depot\|retrait) |
| `PROBLEME_OUVERTURE` | `borneId numeroCasier contexte` | Problème d'ouverture |
| `BUZZER_TIMEOUT` | `borneId numeroCasier contexte` | Délai buzzer écoulé |

### Serveur → µC

| Réponse | Description |
|---|---|
| `OK RFID <nom> <prenom>` | Badge RFID valide |
| `OK CASIER <numero>` | Numéro de casier à ouvrir |
| `OK` | Accusé de réception générique |
| `ERR <message>` | Erreur avec description |

## Prérequis

- **Java 14+** (pour les switch expressions)
- **MongoDB** démarré localement (`mongod`)
- **API Node.js** démarrée sur le port 3000 (base `bornesapi` initialisée)
- **Bibliothèques MongoDB Java driver** (`.jar`) configurées dans IDEA

## Lancement

### Serveur
```
BornesCentralServer 9000
# ou avec URLs personnalisées :
BornesCentralServer 9000 http://localhost:3000/api mongodb://localhost:27017
```

### Client de test
```
BornesClient localhost 9000
```

### Commandes du client de test

```
BornesClient> rfid borne-01 A1B2C3D4
BornesClient> depot borne-01
BornesClient> retrait borne-01 123456
BornesClient> ferme borne-01 3 depot
BornesClient> probleme borne-01 3 depot
BornesClient> timeout borne-01 3 retrait
BornesClient> quit
```

## Flux typiques

### Dépôt livreur
```
rfid borne-01 A1B2C3D4      → OK RFID Dupont Jean
depot borne-01               → OK CASIER 3
(livreur dépose le colis)
ferme borne-01 3 depot       → OK
```

### Retrait client
```
retrait borne-01 123456      → OK CASIER 5
(client récupère le colis)
ferme borne-01 5 retrait     → OK
```

### Problème d'ouverture
```
depot borne-01               → OK CASIER 3
(problème électronique)
probleme borne-01 3 depot    → OK CASIER 7   (casier alternatif)
ferme borne-01 7 depot       → OK
```

## Choix du driver

Dans `ThreadServer.java`, chaque méthode propose les deux alternatives en commentaire :
```java
// Driver HTTP (API Node) — utilisé par défaut :
String answer = exchanger.getHttpDriver().verifierRfid(idRfid);

// Driver Mongo direct — décommenter pour l'utiliser :
// String answer = exchanger.getMongoDriver().verifierRfid(idRfid);
```
