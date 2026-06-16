/**
 * borne.ino — Sketch principal ESP32
 *
 * Protocole TCP (correspond exactement au ThreadServer.java de Valentin) :
 *
 *  µC → Serveur Java :
 *    BADGE_RFID <nomBorne> <idRfid>
 *    DEMANDE_DEPOT <nomBorne>
 *    CODE_RETRAIT <nomBorne> <code>
 *    CASIER_FERME <nomBorne> <numeroCasier> depot|retrait
 *    PROBLEME_OUVERTURE <nomBorne> <numeroCasier> <taille> depot|retrait
 *    BUZZER_TIMEOUT <nomBorne> <numeroCasier> depot|retrait
 *
 *  Serveur Java → µC :
 *    OK RFID <nom> <prenom>    (badge livreur accepté)
 *    OK CASIER <N>             (numéro de casier à ouvrir)
 *    OK                        (acquittement générique)
 *    ERR <message>             (erreur)
 *
 * Bibliothèques (Arduino Library Manager) :
 *   - TM1637Display (Avishay Orpaz)
 *   - MFRC522 (GithubCommunity)
 *   - IRremoteESP8266 (crankyoldgit)
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
MFRC522       rfid(PIN_RFID_SS, PIN_RFID_RST);
IRrecv        irRecv(PIN_IR_RECV);
decode_results irResultat;
Preferences   preferences;
WiFiClient    client;

// =============================================
//  MACHINE À ÉTATS
// =============================================
enum Etat {
  IDLE,
  DEPOT_OUVERTURE,   // Badge OK + casier attribué → attend ouverture switch (A ms)
  DEPOT_FERMETURE,   // Switch ouvert → attend fermeture (B ms)
  DEPOT_BUZZER,      // Buzzer actif → attend fermeture (X ms) avant BUZZER_TIMEOUT
  RETRAIT_OUVERTURE, // Code OK + casier attribué → attend ouverture switch (A ms)
  RETRAIT_FERMETURE, // Switch ouvert → attend fermeture (B ms)
  RETRAIT_BUZZER,    // Buzzer actif → attend fermeture (X ms) avant BUZZER_TIMEOUT
};

Etat etatCourant    = IDLE;
unsigned long tsEtat = 0;  // Timestamp du dernier changement d'état

// =============================================
//  VARIABLES GLOBALES
// =============================================
int    casierCourant = -1;  // Numéro de casier en cours de traitement
String codeIR        = "";  // Accumulation des chiffres IR

// Délais persistés en flash
unsigned long delaiOuverture = DEFAULT_DELAI_OUVERTURE_MS;
unsigned long delaiFermeture = DEFAULT_DELAI_FERMETURE_MS;
unsigned long delaiBuzzer    = DEFAULT_DELAI_BUZZER_MS;

// =============================================
//  SETUP
// =============================================
void setup() {
  Serial.begin(115200);

  pinMode(PIN_SWITCH, INPUT_PULLUP);
  pinMode(PIN_BUZZER, OUTPUT);
  digitalWrite(PIN_BUZZER, LOW);

  afficheur.setBrightness(5);
  afficherTexte("----");

 // Forcer les pins SPI selon la configuration du prof (SCK, MISO, MOSI, SS)
  SPI.begin(18, 23, 19, PIN_RFID_SS);
  rfid.PCD_Init();              // 
  rfid.PCD_DumpVersionToSerial(); // Affiche la version du chip dans le Serial Monitor

  irRecv.enableIRIn();

  chargerParametresFlash();
  connecterWiFi();

  Serial.println("✅ Borne prête — en attente badge ou code IR");
}

// =============================================
//  LOOP — Machine à états
// =============================================
void loop() {
  // TEST DEBUG : Affiche quand on bouge le switch
  static bool lastSwitch = false;
  bool currentSwitch = switchActionne();
  if (currentSwitch != lastSwitch) {
    Serial.println(currentSwitch ? "🔘 DEBUG : Switch actionné (porte ouverte)" : "🔘 DEBUG : Switch relâché (porte fermée)");
    lastSwitch = currentSwitch;
  }

  // Reconnexion TCP si perdue (tentative silencieuse, ne bloque pas la lecture badge)
  if (!client.connected()) {
    connecterServeur();  // ✅ FIX #2 — on tente de reconnecter mais on continue quand même
  }

  unsigned long now = millis();

  switch (etatCourant) {

    // ──────────────────────────────────────────
    case IDLE: {
      // --- CAS LIVREUR : badge RFID ---
      String badge = lireBadgeRFID();
      if (badge.length() > 0) {
        Serial.println("🏷️  Badge lu : " + badge);

        // Étape 1 : vérifier le badge
        String rep1 = envoyerRequete("BADGE_RFID " + String(BORNE_NOM) + " " + badge);

        if (!rep1.startsWith("OK")) {
          Serial.println("Badge refusé : " + rep1);
          afficherTexte("Err");
          delay(DUREE_AFFICHAGE_ERR);
          afficherTexte("----");
          break;
        }

        // Étape 2 : demander un casier de dépôt
        String rep2 = envoyerRequete("DEMANDE_DEPOT " + String(BORNE_NOM));

        if (!rep2.startsWith("OK CASIER")) {
          Serial.println("Pas de casier disponible : " + rep2);
          afficherTexte("Err");
          delay(DUREE_AFFICHAGE_ERR);
          afficherTexte("----");
          break;
        }

        // Extraire le numéro de casier : "OK CASIER 3" → 3
        casierCourant = rep2.substring(10).toInt();  // après "OK CASIER "
        Serial.println("📦 Casier attribué : " + String(casierCourant));
        afficherTexte("OPEN");
        changerEtat(DEPOT_OUVERTURE);
      }

      // --- CAS CLIENT : code IR ---
      int chiffre = lireChiffreIR();
      if (chiffre >= 0) {
        codeIR += String(chiffre);
        afficherNombre(codeIR.toInt());
        Serial.println("🔢 Code IR : " + codeIR);

        if ((int)codeIR.length() == NB_CHIFFRES_CODE) {
          String rep = envoyerRequete("CODE_RETRAIT " + String(BORNE_NOM) + " " + codeIR);
          codeIR = "";

          if (!rep.startsWith("OK CASIER")) {
            Serial.println("Code invalide : " + rep);
            afficherTexte("Err");
            delay(DUREE_AFFICHAGE_ERR);
            afficherTexte("----");
            break;
          }

          casierCourant = rep.substring(10).toInt();
          Serial.println("🔓 Casier retrait : " + String(casierCourant));
          afficherTexte("OPEN");
          changerEtat(RETRAIT_OUVERTURE);
        }
      }
      break;
    }

    // ──────────────────────────────────────────
    case DEPOT_OUVERTURE: {
      if (switchActionne()) {
        Serial.println("✅ Switch ouvert → attente fermeture");
        changerEtat(DEPOT_FERMETURE);
      } else if (now - tsEtat >= delaiOuverture) {
        Serial.println("⏱️  Timeout ouverture → PROBLEME_OUVERTURE");
        envoyerRequete("PROBLEME_OUVERTURE " + String(BORNE_NOM) + " "
                       + String(casierCourant) + " " + String(TAILLE_COLIS) + " depot");
        afficherTexte("Err");
        delay(DUREE_AFFICHAGE_ERR);
        afficherTexte("----");
        changerEtat(IDLE);
      }
      break;
    }

    // ──────────────────────────────────────────
     case DEPOT_FERMETURE: {
       if (!switchActionne()) {
         Serial.println("✅ Porte refermée → CASIER_FERME depot");
         envoyerRequete("CASIER_FERME " + String(BORNE_NOM) + " "
                        + String(casierCourant) + " depot");
         afficherTexte("donE");
         delay(2000);
         afficherTexte("----");
         changerEtat(IDLE);
       } else if (now - tsEtat >= delaiFermeture) {
         Serial.println("⏱️  Timeout fermeture → Buzzer ON");
         activerBuzzer(true);
         changerEtat(DEPOT_BUZZER);
       }
       break;
    }

    // ──────────────────────────────────────────
    case DEPOT_BUZZER: {
      if (!switchActionne()) {
        activerBuzzer(false);
        Serial.println("✅ Fermé pendant buzzer → CASIER_FERME depot");
        envoyerRequete("CASIER_FERME " + String(BORNE_NOM) + " "
                       + String(casierCourant) + " depot");
        afficherTexte("donE");
        delay(2000);
        afficherTexte("----");
        changerEtat(IDLE);
      } else if (now - tsEtat >= delaiBuzzer) {
        activerBuzzer(false);
        Serial.println("🚨 Buzzer timeout → BUZZER_TIMEOUT depot");
        envoyerRequete("BUZZER_TIMEOUT " + String(BORNE_NOM) + " "
                       + String(casierCourant) + " depot");
        afficherTexte("Err");
        delay(DUREE_AFFICHAGE_ERR);
        afficherTexte("----");
        changerEtat(IDLE);
      }
      break;
    }

    // ──────────────────────────────────────────
    case RETRAIT_OUVERTURE: {
      if (switchActionne()) {
        changerEtat(RETRAIT_FERMETURE);
      } else if (now - tsEtat >= delaiOuverture) {
        envoyerRequete("PROBLEME_OUVERTURE " + String(BORNE_NOM) + " "
                       + String(casierCourant) + " " + String(TAILLE_COLIS) + " retrait");
        afficherTexte("Err");
        delay(DUREE_AFFICHAGE_ERR);
        afficherTexte("----");
        changerEtat(IDLE);
      }
      break;
    }

    // ──────────────────────────────────────────
    case RETRAIT_FERMETURE: {
      if (!switchActionne()) {
        Serial.println("✅ Porte refermée → CASIER_FERME retrait");
        envoyerRequete("CASIER_FERME " + String(BORNE_NOM) + " "
                       + String(casierCourant) + " retrait");
        afficherTexte("donE");
        delay(2000);
        afficherTexte("----");
        changerEtat(IDLE);
      } else if (now - tsEtat >= delaiFermeture) {
        activerBuzzer(true);
        changerEtat(RETRAIT_BUZZER);
      }
      break;
    }

    // ──────────────────────────────────────────
    case RETRAIT_BUZZER: {
      if (!switchActionne()) {
        activerBuzzer(false);
        envoyerRequete("CASIER_FERME " + String(BORNE_NOM) + " "
                       + String(casierCourant) + " retrait");
        afficherTexte("donE");
        delay(2000);
        afficherTexte("----");
        changerEtat(IDLE);
      } else if (now - tsEtat >= delaiBuzzer) {
        activerBuzzer(false);
        Serial.println("🚨 Buzzer timeout → BUZZER_TIMEOUT retrait");
        envoyerRequete("BUZZER_TIMEOUT " + String(BORNE_NOM) + " "
                       + String(casierCourant) + " retrait");
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
//  RÉSEAU
// =============================================
void connecterWiFi() {
  Serial.print("📶 Connexion WiFi : ");
  Serial.println(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500); Serial.print(".");
  }
  Serial.println("\n✅ WiFi OK — IP : " + WiFi.localIP().toString());
  connecterServeur();
}

bool connecterServeur() {
  if (client.connect(SERVER_IP, SERVER_PORT)) {
    Serial.println("✅ Connecté au serveur Java (" + String(SERVER_IP) + ":" + String(SERVER_PORT) + ")");
    return true;
  }
  Serial.println("❌ Connexion serveur échouée");
  return false;
}

/**
 * Envoie une commande texte et retourne la réponse.
 * Le \n final est requis par le BufferedReader Java.
 */
String envoyerRequete(const String& cmd) {
  if (!client.connected() && !connecterServeur()) return "ERR:NO_CONNECTION";

  Serial.println("→ " + cmd);
  client.println(cmd);  // println = cmd + \r\n

  unsigned long debut = millis();
  while (!client.available()) {
    if (millis() - debut > 5000) { Serial.println("⏱️  Timeout réponse"); return "ERR:TIMEOUT"; }
    delay(10);
  }

  String rep = client.readStringUntil('\n');
  rep.trim();
  Serial.println("← " + rep);
  return rep;
}

// =============================================
//  RFID
// =============================================
String lireBadgeRFID() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) return "";
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  return uid;
}

// =============================================
//  INFRAROUGE
//  ⚠️  Les codes HEX ci-dessous sont des exemples génériques.
//  Flashe le sketch de calibration IR pour obtenir les vrais codes de ta télécommande,
//  puis remplace chaque valeur ici.
// =============================================
int lireChiffreIR() {
  if (!irRecv.decode(&irResultat)) return -1;

  uint64_t valeur = irResultat.value;
  irRecv.resume();

  // ✅ DEBUG — Affiche le code brut reçu dans le Serial Monitor
  //    Appuie sur chaque touche 0-9 et note les valeurs affichées,
  //    puis remplace les case ci-dessous avec TES vraies valeurs.
  Serial.print("📡 IR reçu — valeur HEX : 0x");
  Serial.print((uint32_t)(valeur >> 32), HEX);  // partie haute (si 64 bits)
  Serial.print((uint32_t)(valeur & 0xFFFFFFFF), HEX);
  Serial.print("  protocole : ");
  Serial.println(irResultat.decode_type);

  switch (valeur) {
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
    default:
      Serial.println("   ⚠️  Code non mappé — mets à jour le switch avec cette valeur !");
      return -1;
  }
}

// =============================================
//  AFFICHEUR TM1637
// =============================================
void afficherTexte(const String& t) {
  if      (t == "OPEN") afficheur.setSegments((uint8_t[]){0x3F, 0x73, 0x79, 0x37});
  else if (t == "Err")  afficheur.setSegments((uint8_t[]){0x00, 0x79, 0x50, 0x50});
  else if (t == "donE") afficheur.setSegments((uint8_t[]){0x5E, 0x3F, 0x54, 0x79});
  else                  afficheur.setSegments((uint8_t[]){0x40, 0x40, 0x40, 0x40}); // ----
}

void afficherNombre(int n) {
  afficheur.showNumberDec(n, true);
}

// =============================================
//  SWITCH & BUZZER
// =============================================
bool switchActionne() { return digitalRead(PIN_SWITCH) == LOW; }
void activerBuzzer(bool on) { digitalWrite(PIN_BUZZER, on ? HIGH : LOW); }

// =============================================
//  MACHINE À ÉTATS
// =============================================
void changerEtat(Etat e) { etatCourant = e; tsEtat = millis(); }

// =============================================
//  FLASH (Preferences)
// =============================================
void chargerParametresFlash() {
  preferences.begin("borne", true);
  delaiOuverture = preferences.getULong("ouv",  DEFAULT_DELAI_OUVERTURE_MS);
  delaiFermeture = preferences.getULong("ferm", DEFAULT_DELAI_FERMETURE_MS);
  delaiBuzzer    = preferences.getULong("buzz", DEFAULT_DELAI_BUZZER_MS);
  preferences.end();
  Serial.printf("⚙️  Délais — A:%lums B:%lums X:%lums\n", delaiOuverture, delaiFermeture, delaiBuzzer);
}

void sauvegarderParametresFlash() {
  preferences.begin("borne", false);
  preferences.putULong("ouv",  delaiOuverture);
  preferences.putULong("ferm", delaiFermeture);
  preferences.putULong("buzz", delaiBuzzer);
  preferences.end();
}
