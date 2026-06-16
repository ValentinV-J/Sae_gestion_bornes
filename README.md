# 📦 SAÉ - Système de Gestion de Bornes Connectées

![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)
![Express.js](https://img.shields.io/badge/Express.js-000000?style=for-the-badge&logo=express&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Vue.js](https://img.shields.io/badge/Vue.js-35495E?style=for-the-badge&logo=vuedotjs&logoColor=4FC08D)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![C++](https://img.shields.io/badge/C++-00599C?style=for-the-badge&logo=c%2B%2B&logoColor=white)

> **Projet universitaire réalisé dans le cadre de la SAÉ Développement et Déploiement Logiciel.**
> **Auteurs :** Semih & Valentin

---

## 📑 Sommaire
1. [Contexte et Objectifs](#-contexte-et-objectifs)
2. [Architecture du Système](#-architecture-du-système)
3. [Détail des Composants](#-détail-des-composants)
4. [Protocoles de Communication](#-protocoles-de-communication)
5. [Câblage Matériel (ESP32)](#-câblage-matériel-esp32)
6. [Installation et Lancement](#-installation-et-lancement)

---

## 🎯 Contexte et Objectifs

L'objectif de cette SAÉ est de concevoir de A à Z le système logiciel complet permettant d'opérer des **consignes connectées** (similaires aux Amazon Lockers). Le système doit gérer deux flux utilisateurs principaux :

1. **Le flux Livreur (Dépôt) :**
   - Le livreur s'identifie sur la borne avec un **badge RFID**.
   - Il utilise une **application mobile** pour scanner le QR Code du colis à livrer.
   - La borne lui ouvre automatiquement un casier vide de la bonne taille.
   - La fermeture de la porte confirme le dépôt et déclenche l'envoi d'un code secret au client final.

2. **Le flux Client (Retrait) :**
   - Le client se présente devant la borne.
   - Il saisit son code secret à 4 chiffres à l'aide d'une **télécommande infrarouge**.
   - Si le code est valide, le casier contenant son colis s'ouvre.
   - La fermeture de la porte confirme le retrait définitif.

L'enjeu majeur du projet réside dans l'intégration de technologies très variées (embarqué, socket TCP, API REST, Web, Mobile) au sein d'une architecture distribuée.

---

## 🏗️ Architecture du Système

Le projet est divisé en **5 sous-projets** distincts collaborant en temps réel.

```mermaid
flowchart TD
    %% Noeuds matériels et terminaux
    subgraph "Terrain (Physique)"
        ESP32[("📟 Microcontrôleur (ESP32)")]
        AppMobile[("📱 Appli Mobile (Livreur)")]
    end

    %% Serveurs Backend
    subgraph "Backend (Serveur)"
        JavaServer[["☕ Serveur Central Java"]]
        NodeAPI[["⚙️ API Node.js (REST)"]]
        DB[(("🗄️ MongoDB"))]
    end

    %% Dashboard Web
    subgraph "Administration"
        WebAdmin[("🖥️ Interface Web Vue.js")]
    end

    %% Relations
    ESP32 <-->|Sockets TCP| JavaServer
    JavaServer <-->|HTTP REST| NodeAPI
    AppMobile -->|HTTP REST| NodeAPI
    WebAdmin <-->|HTTP REST| NodeAPI
    NodeAPI <-->|Mongoose| DB
```

---

## 🧩 Détail des Composants

### 1. ⚙️ API Node.js (`/api`)
Le cœur central de toute la logique métier et l'accès exclusif à la base de données.
- **Technologies :** Node.js, Express, Mongoose.
- **Sécurité :** Authentification par JWT, mots de passe hashés avec bcrypt, Middlewares de protection des routes (Rôle Admin / Technicien).
- **Fonctionnalités :** 
  - CRUD complet (Bornes, Colis, Livreurs, Logs).
  - Algorithme d'attribution des casiers (recherche du casier optimal selon la taille du colis).
  - Génération des codes secrets aléatoires pour les retraits.

### 2. ☕ Serveur Central Java (`/CentralServer`)
Le "traducteur" temps réel entre le matériel physique et l'API web.
- **Technologies :** Java SE.
- **Architecture :** Serveur TCP Multithread (un thread `ThreadServer` dédié par borne connectée).
- **Rôle :** 
  - Écoute les signaux bruts envoyés par l'ESP32.
  - Interroge l'API Node.js (`HttpDataDriver`) pour prendre des décisions (Le badge est-il valide ? Quel casier ouvrir ?).
  - Renvoie des instructions simples et formatées à l'ESP32.

### 3. 📟 Microcontrôleur C++ (`/microcontroleur`)
L'intelligence de la borne physique.
- **Technologies :** C++ (Arduino), ESP32.
- **Bibliothèques :** `MFRC522` (RFID), `TM1637Display` (Afficheur), `IRremoteESP8266` (Infrarouge), `Preferences` (EEPROM Flash).
- **Rôle :**
  - Gère les capteurs et actionneurs.
  - Implémente une **Machine à États** robuste (IDLE, DEPOT_OUVERTURE, DEPOT_FERMETURE, etc.).
  - Gère des timeouts dynamiques (sauvegardés en mémoire Flash) pour détecter les pannes matérielles (porte qui ne se ferme pas, électro-aimant bloqué) et remonter des alertes (`PROBLEME_OUVERTURE`, `BUZZER_TIMEOUT`).

### 4. 📱 Application Mobile (`/MobileApp`)
L'outil métier exclusif des livreurs.
- **Technologies :** Android natif (Kotlin), CameraX, Google ML Kit (Barcode Scanning), Retrofit.
- **Rôle :**
  - Scanner le QR code imprimé sur les colis.
  - Interroger l'API Node.js pour vérifier la validité de l'UUID du colis.
  - Confirmer la prise en charge du colis par le livreur (changement de statut en `SCAN_MOBILE_OK`).

### 5. 🖥️ WebAdmin (`/WebAdmin`)
Le tableau de bord pour les techniciens et administrateurs du réseau de consignes.
- **Technologies :** Vue.js 3 (Composition API), Vite, Vue Router, Pinia (State Management), TailwindCSS.
- **Rôle :**
  - Supervision de l'état en temps réel des casiers (Vide, Occupé, En Erreur).
  - Visualisation des Logs matériels envoyés par les ESP32.
  - Gestion des profils livreurs (ajout/suppression de badges RFID).

---

## 📡 Protocoles de Communication

### Protocole TCP (ESP32 ↔ Serveur Java)

Pour garantir une communication fluide et de bas niveau, un protocole texte sur-mesure a été implémenté sur le socket TCP. Chaque message se termine par un saut de ligne `\n`.

**Requêtes (ESP32 vers Java) :**
- `BADGE_RFID <nomBorne> <idBadge>` : Demande d'authentification livreur.
- `DEMANDE_DEPOT <nomBorne>` : Le livreur a scanné, demande de casier.
- `CODE_RETRAIT <nomBorne> <codeSecret>` : Le client a saisi son code à la télécommande.
- `CASIER_FERME <nomBorne> <numeroCasier> <depot|retrait>` : Le capteur indique que la porte a été fermée.
- `PROBLEME_OUVERTURE <nomBorne> <numeroCasier> <taille> <contexte>` : Le timeout est dépassé, la porte ne s'est pas ouverte.
- `BUZZER_TIMEOUT <nomBorne> <numeroCasier> <contexte>` : La porte a été laissée ouverte trop longtemps.

**Réponses (Java vers ESP32) :**
- `OK RFID <societe>` : Authentification réussie.
- `OK CASIER <numero>` : Autorisation d'ouvrir le casier N.
- `OK` : Accusé de réception simple.
- `ERR <message>` : Refus ou erreur (affiche `Err` sur la borne).

---

## 🔌 Câblage Matériel (ESP32)

Afin de reproduire le prototype physique, voici la carte des branchements (Pins définis selon les contraintes du cahier des charges).

| Composant | Pin ESP32 | Description |
| :--- | :---: | :--- |
| **Afficheur TM1637** | `33` / `32` | CLK sur Pin 33, DIO sur Pin 32 |
| **Buzzer** | `25` | Alarme d'oubli de fermeture |
| **Récepteur IR** | `26` | Clavier client simulé (Télécommande) |
| **Switch (Porte)** | `27` | Doit être connecté au **GND** lorsqu'actionné (utilise l'internal `PULLUP`) |
| **Lecteur RFID (MFRC522)** | | Câblage SPI matériel |
| ↳ *VCC* | `3V3` | Alimentation (Jamais 5V !) |
| ↳ *GND* | `GND` | Masse |
| ↳ *RST* | `22` | Reset hardware |
| ↳ *IRQ* | `4` | Interruption (optionnel) |
| ↳ *NSS / SDA* | `5` | Chip Select (SS) |
| ↳ *SCK* | `18` | Horloge SPI |
| ↳ *MISO* | `19` | Master In Slave Out (Fil Jaune) |
| ↳ *MOSI / POSI* | `21` | Master Out Slave In (Fil Vert) |

---

## 🚀 Installation et Lancement

Ce projet nécessite d'avoir Node.js, Java (JDK 17+) et Android Studio d'installés sur votre machine.

### Étape 1 : Base de données et API Node.js
```bash
# Se placer dans le dossier de l'API
cd api

# Installer les dépendances
npm install

# Créer la base de données de test et l'administrateur
node seed.js

# Lancer le serveur (Port 3000 par défaut)
npm run dev
```

### Étape 2 : L'interface d'Administration (WebAdmin)
Dans un nouveau terminal :
```bash
# Se placer dans le dossier WebAdmin
cd WebAdmin

# Installer les dépendances Frontend
npm install

# Lancer Vite
npm run dev
```
> L'interface sera accessible sur `http://localhost:5173`. L'identifiant par défaut est `admin` et le mot de passe `Admin1234!`.

### Étape 3 : Le Serveur Central Java
1. Ouvrir le dossier `/CentralServer` dans **IntelliJ IDEA** (ou Eclipse).
2. S'assurer que le SDK Java est configuré.
3. Exécuter la classe principale `BornesCentralServer`.
> Le serveur va démarrer et écouter les connexions TCP entrantes sur le port `9000`.

### Étape 4 : Le Microcontrôleur
1. Ouvrir `/microcontroleur/borne/borne.ino` avec l'**Arduino IDE**.
2. Dans l'onglet `config.h`, modifier impérativement les lignes :
   - `#define WIFI_SSID` (Votre réseau WiFi 2.4GHz)
   - `#define WIFI_PASSWORD` (Votre mot de passe WiFi)
   - `#define SERVER_IP` (L'adresse IP locale `192.168.x.x` de l'ordinateur qui fait tourner le serveur Java).
3. Connecter l'ESP32 et cliquer sur **Téléverser**.

### Étape 5 : L'application Mobile
1. Ouvrir le dossier `/MobileApp` avec **Android Studio**.
2. Lancer l'application sur un smartphone physique ou un émulateur.
3. Si vous utilisez l'émulateur, l'IP par défaut vers l'API Node locale est déjà pré-configurée (`http://10.0.2.2:3000`). Si vous utilisez un smartphone réel, allez dans les paramètres de l'appli mobile pour saisir l'IP de votre PC.

---
> 💡 *Soutenance finale - IUT - Département Informatique*
