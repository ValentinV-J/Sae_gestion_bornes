/**
 * borne.ino — Sketch principal du micro-contrôleur de la borne de dépôt/retrait
 *
 * ESP32 — Arduino Framework
 *
 * Bibliothèques nécessaires (à installer via Arduino Library Manager) :
 *   - TM1637Display  (par Avishay Orpaz)
 *   - MFRC522        (par GithubCommunity)
 *   - IRremoteESP8266 (par crankyoldgit) — fonctionne aussi sur ESP32
 *
 * Architecture :
 *   - Machine à états pour gérer les scénarios dépôt/retrait
 *   - Interruption pour la télécommande IR (non bloquant)
 *   - Paramètres persistés en flash via Preferences
 *   - Communication TCP texte vers le serveur Java central
 */

#include <WiFi.h>
#include <Preferences.h>
#include <SPI.h>
#include <MFRC522.h>
#include <TM1637Display.h>
#include <IRremoteESP8266.h>
#include <IRrecv.h>
#include <IRutils.h>
#include "config.h"

// =============================================
//  OBJETS MATÉRIELS
// =============================================
TM1637Display afficheur(PIN_LCD_CLK, PIN_LCD_DIO);
MFRC522 rfid(PIN_RFID_SS, PIN_RFID_RST);
IRrecv irRecv(PIN_IR_RECV);
decode_results irResultat;
Preferences preferences;
WiFiClient client;

// =============================================
//  ÉTATS DE LA MACHINE À ÉTATS
// =============================================
enum Etat {
  IDLE,                  // En attente (badge ou code IR)
  DEPOT_OUVERTURE,       // Badge OK, attente ouverture porte (3s)
  DEPOT_FERMETURE,       // Porte ouverte, attente fermeture (B secondes)
  DEPOT_BUZZER,          // Porte non fermée, buzzer actif (X secondes)
  RETRAIT_OUVERTURE,     // Code OK, attente ouverture porte (3s)
  RETRAIT_FERMETURE,     // Porte ouverte, attente fermeture (B secondes)
  RETRAIT_BUZZER,        // Porte non fermée, buzzer actif (X secondes)
};

Etat etatCourant = IDLE;

// =============================================
//  VARIABLES GLOBALES
// =============================================
unsigned long timestampEtat = 0;    // Moment du dernier changement d'état
int casierCourant = -1;             // Numéro du casier en cours de traitement
String codeIR = "";                 // Accumulation des chiffres IR (4 chiffres)

// Délais persistés en flash (mis à jour par le serveur Java si besoin)
unsigned long delaiOuverture = DEFAULT_DELAI_OUVERTURE_MS;
unsigned long delaiFermeture = DEFAULT_DELAI_FERMETURE_MS;
unsigned long delaiBuzzer    = DEFAULT_DELAI_BUZZER_MS;

// Flag d'interruption IR (modifié par la routine d'interruption)
volatile bool irRecu = false;

// =============================================
//  PROTOTYPES DE FONCTIONS
// =============================================
void connecterWiFi();
bool connecterServeur();
String envoyerRequete(const String& message);
void chargerParametresFlash();
void sauvegarderParametresFlash();
void parserParametres(const String& reponse);
String lireBadgeRFID();
void afficherTexte(const String& texte);
void afficherNombre(int nombre);
int lireChiffreIR();
void changerEtat(Etat nouvelEtat);
bool switchActionne();
void activerBuzzer(bool actif);

// =============================================
//  SETUP
// =============================================
void setup() {
  Serial.begin(115200);

  // Initialisation des pins
  pinMode(PIN_SWITCH, INPUT_PULLUP);
  pinMode(PIN_BUZZER, OUTPUT);
  digitalWrite(PIN_BUZZER, LOW);

  // Afficheur
  afficheur.setBrightness(5);
  afficheur.showNumberDec(0);

  // SPI + RFID
  SPI.begin();
  rfid.PCD_Init();

  // Récepteur IR (via interruption, non bloquant)
  irRecv.enableIRIn();

  // Chargement des délais depuis la mémoire flash
  chargerParametresFlash();

  // Connexion WiFi
  connecterWiFi();

  Serial.println("✅ Borne prête.");
  afficherTexte("----");
}

// =============================================
//  LOOP — Machine à états principale
// =============================================
void loop() {
  // S'assurer que la connexion TCP est active avant chaque cycle
  if (!client.connected()) {
    Serial.println("🔁 Reconnexion au serveur Java...");
    connecterServeur();
    delay(1000);
    return;
  }

  unsigned long maintenant = millis();

  switch (etatCourant) {

    // ------------------------------------------
    case IDLE: {
      // Vérifier si un badge RFID est présent
      String badge = lireBadgeRFID();
      if (badge.length() > 0) {
        Serial.println("Badge lu : " + badge);
        String requete = String(BORNE_ID) + ":BADGE:" + badge;
        String reponse = envoyerRequete(requete);

        if (reponse.startsWith("OPEN:")) {
          parserParametres(reponse);   // Récupère N° casier + éventuellement A,B,X,Y
          afficherTexte("OPEN");
          changerEtat(DEPOT_OUVERTURE);
        } else {
          Serial.println("Badge refusé : " + reponse);
          afficherTexte("Err");
          delay(DUREE_AFFICHAGE_ERR);
          afficherTexte("----");
        }
      }

      // Vérifier si un chiffre IR a été reçu (accumulation du code client)
      int chiffre = lireChiffreIR();
      if (chiffre >= 0) {
        codeIR += String(chiffre);
        afficherNombre(codeIR.toInt());  // Affiche les chiffres saisis jusqu'ici
        Serial.println("Code IR en cours : " + codeIR);

        if (codeIR.length() == NB_CHIFFRES_CODE) {
          // Code complet, on l'envoie au serveur
          String requete = String(BORNE_ID) + ":CODE:" + codeIR;
          String reponse = envoyerRequete(requete);
          codeIR = "";  // Réinitialiser pour la prochaine saisie

          if (reponse.startsWith("OPEN:")) {
            parserParametres(reponse);
            afficherTexte("OPEN");
            changerEtat(RETRAIT_OUVERTURE);
          } else {
            Serial.println("Code invalide : " + reponse);
            afficherTexte("Err");
            delay(DUREE_AFFICHAGE_ERR);
            afficherTexte("----");
          }
        }
      }
      break;
    }

    // ------------------------------------------
    case DEPOT_OUVERTURE: {
      // Attendre que le livreur ouvre la porte (switch actionné)
      if (switchActionne()) {
        Serial.println("Porte ouverte, attente fermeture...");
        changerEtat(DEPOT_FERMETURE);
      } else if (maintenant - timestampEtat >= delaiOuverture) {
        // Timeout : le livreur n'a pas ouvert dans les temps
        Serial.println("Timeout ouverture → ERR_OPEN");
        String requete = String(BORNE_ID) + ":ERR_OPEN:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("Err");
        delay(DUREE_AFFICHAGE_ERR);
        afficherTexte("----");
        changerEtat(IDLE);
      }
      break;
    }

    // ------------------------------------------
    case DEPOT_FERMETURE: {
      // Attendre que le livreur referme la porte
      if (!switchActionne()) {
        Serial.println("Porte refermée → DEPOT_OK");
        String requete = String(BORNE_ID) + ":DEPOT_OK:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("done");
        delay(2000);
        afficherTexte("----");
        changerEtat(IDLE);
      } else if (maintenant - timestampEtat >= delaiFermeture) {
        // La porte n'est toujours pas fermée → buzzer
        Serial.println("Timeout fermeture → Buzzer ON");
        activerBuzzer(true);
        changerEtat(DEPOT_BUZZER);
      }
      break;
    }

    // ------------------------------------------
    case DEPOT_BUZZER: {
      // Buzzer actif, on attend encore
      if (!switchActionne()) {
        // Le livreur a finalement fermé pendant le buzzer
        activerBuzzer(false);
        Serial.println("Porte refermée (pendant buzzer) → DEPOT_OK");
        String requete = String(BORNE_ID) + ":DEPOT_OK:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("done");
        delay(2000);
        afficherTexte("----");
        changerEtat(IDLE);
      } else if (maintenant - timestampEtat >= delaiBuzzer) {
        // Toujours pas fermée après le buzzer → erreur définitive
        activerBuzzer(false);
        Serial.println("Erreur fermeture définitive → ERR_CLOSE");
        String requete = String(BORNE_ID) + ":ERR_CLOSE:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("Err");
        delay(DUREE_AFFICHAGE_ERR);
        afficherTexte("----");
        changerEtat(IDLE);
      }
      break;
    }

    // ------------------------------------------
    case RETRAIT_OUVERTURE: {
      // Identique à DEPOT_OUVERTURE mais pour le retrait
      if (switchActionne()) {
        changerEtat(RETRAIT_FERMETURE);
      } else if (maintenant - timestampEtat >= delaiOuverture) {
        String requete = String(BORNE_ID) + ":ERR_OPEN:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("Err");
        delay(DUREE_AFFICHAGE_ERR);
        afficherTexte("----");
        changerEtat(IDLE);
      }
      break;
    }

    // ------------------------------------------
    case RETRAIT_FERMETURE: {
      if (!switchActionne()) {
        String requete = String(BORNE_ID) + ":RETRAIT_OK:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("done");
        delay(2000);
        afficherTexte("----");
        changerEtat(IDLE);
      } else if (maintenant - timestampEtat >= delaiFermeture) {
        activerBuzzer(true);
        changerEtat(RETRAIT_BUZZER);
      }
      break;
    }

    // ------------------------------------------
    case RETRAIT_BUZZER: {
      if (!switchActionne()) {
        activerBuzzer(false);
        String requete = String(BORNE_ID) + ":RETRAIT_OK:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("done");
        delay(2000);
        afficherTexte("----");
        changerEtat(IDLE);
      } else if (maintenant - timestampEtat >= delaiBuzzer) {
        activerBuzzer(false);
        String requete = String(BORNE_ID) + ":ERR_CLOSE:" + String(casierCourant);
        envoyerRequete(requete);
        afficherTexte("Err");
        delay(DUREE_AFFICHAGE_ERR);
        afficherTexte("----");
        changerEtat(IDLE);
      }
      break;
    }
  }
}

// =============================================
//  IMPLÉMENTATION DES FONCTIONS
// =============================================

/** Connexion au réseau WiFi */
void connecterWiFi() {
  Serial.print("Connexion WiFi à ");
  Serial.println(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\n✅ WiFi connecté — IP : " + WiFi.localIP().toString());
  connecterServeur();
}

/** Ouverture de la connexion TCP vers le serveur Java */
bool connecterServeur() {
  if (client.connect(SERVER_IP, SERVER_PORT)) {
    Serial.println("✅ Connecté au serveur Java");
    return true;
  }
  Serial.println("❌ Connexion serveur Java échouée");
  return false;
}

/**
 * Envoie une requête texte au serveur Java et retourne la réponse.
 * Le message est terminé par \n (obligatoire pour BufferedReader côté Java).
 */
String envoyerRequete(const String& message) {
  if (!client.connected()) {
    Serial.println("⚠️ Socket non connectée, tentative de reconnexion...");
    if (!connecterServeur()) return "ERR:NO_CONNECTION";
  }

  Serial.println("→ Envoi : " + message);
  client.println(message);  // println ajoute \r\n automatiquement

  // Attendre la réponse (timeout 5s)
  unsigned long debut = millis();
  while (!client.available()) {
    if (millis() - debut > 5000) return "ERR:TIMEOUT";
    delay(10);
  }

  String reponse = client.readStringUntil('\n');
  reponse.trim();
  Serial.println("← Réponse : " + reponse);
  return reponse;
}

/**
 * Parse la réponse du serveur pour extraire :
 * - Le numéro de casier (toujours présent dans OPEN:N)
 * - Les paramètres A,B,X,Y si fournis (OPEN:N:A:B:X:Y)
 * Exemple : "OPEN:3" ou "OPEN:3:3000:10000:10000"
 */
void parserParametres(const String& reponse) {
  // Format : OPEN:N ou OPEN:N:A:B:X
  int idx1 = reponse.indexOf(':');
  int idx2 = reponse.indexOf(':', idx1 + 1);

  if (idx1 == -1) return;

  // Extraction du numéro de casier
  if (idx2 == -1) {
    casierCourant = reponse.substring(idx1 + 1).toInt();
  } else {
    casierCourant = reponse.substring(idx1 + 1, idx2).toInt();

    // Extraction des paramètres A, B, X si présents
    String reste = reponse.substring(idx2 + 1);
    int i1 = reste.indexOf(':');
    int i2 = reste.indexOf(':', i1 + 1);
    int i3 = reste.indexOf(':', i2 + 1);

    if (i1 != -1 && i2 != -1 && i3 != -1) {
      delaiOuverture = reste.substring(0, i1).toInt();
      delaiFermeture = reste.substring(i1 + 1, i2).toInt();
      delaiBuzzer    = reste.substring(i2 + 1, i3).toInt();
      Serial.println("⚙️ Paramètres mis à jour : A=" + String(delaiOuverture) +
                     " B=" + String(delaiFermeture) +
                     " X=" + String(delaiBuzzer));
      sauvegarderParametresFlash();
    }
  }

  Serial.println("📦 Casier cible : " + String(casierCourant));
}

/** Charge les délais depuis la mémoire flash (survivent au redémarrage) */
void chargerParametresFlash() {
  preferences.begin("borne", true); // Mode lecture seule
  delaiOuverture = preferences.getULong("delaiOuv", DEFAULT_DELAI_OUVERTURE_MS);
  delaiFermeture = preferences.getULong("delaiFerm", DEFAULT_DELAI_FERMETURE_MS);
  delaiBuzzer    = preferences.getULong("delaiBuzz", DEFAULT_DELAI_BUZZER_MS);
  preferences.end();
  Serial.println("⚙️ Paramètres chargés : A=" + String(delaiOuverture) +
                 " B=" + String(delaiFermeture) +
                 " X=" + String(delaiBuzzer));
}

/** Sauvegarde les délais en mémoire flash après mise à jour par le serveur */
void sauvegarderParametresFlash() {
  preferences.begin("borne", false); // Mode écriture
  preferences.putULong("delaiOuv",  delaiOuverture);
  preferences.putULong("delaiFerm", delaiFermeture);
  preferences.putULong("delaiBuzz", delaiBuzzer);
  preferences.end();
}

/**
 * Lit un badge RFID si présent.
 * Retourne l'UID sous forme de String en majuscules (ex: "A1B2C3D4")
 * Retourne "" si aucun badge n'est détecté.
 */
String lireBadgeRFID() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) {
    return "";
  }
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  rfid.PICC_HaltA();       // Mise en veille de la carte
  rfid.PCD_StopCrypto1();  // Arrêt du chiffrement
  return uid;
}

/**
 * Lit un chiffre (0-9) depuis la télécommande IR.
 * Retourne -1 si aucune touche n'a été pressée.
 * Utilise irRecv (non bloquant grâce à enableIRIn).
 */
int lireChiffreIR() {
  if (!irRecv.decode(&irResultat)) return -1;

  irRecv.resume(); // Prêt pour la prochaine valeur

  // Mapping des codes IR vers les chiffres 0-9
  // Ces codes dépendent de votre télécommande — à calibrer avec le sketch de test IR
  switch (irResultat.value) {
    case 0xFF6897: return 0;
    case 0xFF30CF: return 1;
    case 0xFF18E7: return 2;
    case 0xFF7A85: return 3;
    case 0xFF10EF: return 4;
    case 0xFF38C7: return 5;
    case 0xFF5AA5: return 6;
    case 0xFF42BD: return 7;
    case 0xFF4AB5: return 8;
    case 0xFF52AD: return 9;
    default:       return -1; // Touche non reconnue (volume, menu, etc.)
  }
}

/**
 * Affiche un texte de 4 caractères sur l'afficheur TM1637.
 * Textes supportés : "OPEN", "Err", "done", "----"
 */
void afficherTexte(const String& texte) {
  if (texte == "OPEN") {
    // O = 0x3F, P = 0x73, E = 0x79, N = 0x37 (encodage 7-seg)
    afficheur.setSegments((uint8_t[]){0x3F, 0x73, 0x79, 0x37});
  } else if (texte == "Err") {
    afficheur.setSegments((uint8_t[]){0x00, 0x79, 0x50, 0x50}); // _Err
  } else if (texte == "done") {
    afficheur.setSegments((uint8_t[]){0x5E, 0x3F, 0x54, 0x79}); // donE
  } else {
    afficheur.setSegments((uint8_t[]){0x40, 0x40, 0x40, 0x40}); // ----
  }
}

/** Affiche un nombre entier sur l'afficheur TM1637 (utile pour la saisie du code IR) */
void afficherNombre(int nombre) {
  afficheur.showNumberDec(nombre, true); // true = afficher les zéros de gauche
}

/** Change l'état de la machine et enregistre le timestamp */
void changerEtat(Etat nouvelEtat) {
  etatCourant = nouvelEtat;
  timestampEtat = millis();
}

/**
 * Retourne true si le switch est actionné (porte ouverte).
 * Le switch est en INPUT_PULLUP donc LOW = actionné.
 */
bool switchActionne() {
  return digitalRead(PIN_SWITCH) == LOW;
}

/** Active ou désactive le buzzer */
void activerBuzzer(bool actif) {
  digitalWrite(PIN_BUZZER, actif ? HIGH : LOW);
}
