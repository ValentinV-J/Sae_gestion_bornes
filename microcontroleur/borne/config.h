#ifndef CONFIG_H
#define CONFIG_H

// =============================================
//  CONFIGURATION RÉSEAU
// =============================================
#define WIFI_SSID "Xiaomi 13 Pro"
#define WIFI_PASSWORD "valentin1234!"
#define SERVER_IP                                                              \
  "10.25.11.72"          // IP de la machine qui fait tourner le serveur Java
#define SERVER_PORT 9000 // Port TCP du serveur Java (MainServer.java)

// =============================================
//  IDENTIFIANT DE CETTE BORNE
//  Doit correspondre EXACTEMENT au champ "nom" de la borne dans MongoDB
// =============================================
#define BORNE_NOM "Borne_Belfort"

// =============================================
//  TAILLE PAR DÉFAUT DES COLIS
//  Utilisée pour PROBLEME_OUVERTURE si l'ouverture du casier échoue
// =============================================
#define TAILLE_COLIS "M"

// =============================================
//  PINS — Afficheur LCD 4 digits (TM1637)
// =============================================
#define PIN_LCD_CLK 33
#define PIN_LCD_DIO 32

// =============================================
//  PINS — Buzzer
// =============================================
#define PIN_BUZZER 25

// =============================================
//  PINS — Switch à glissière (simulation porte)
// =============================================
#define PIN_SWITCH 27

// =============================================
//  PINS — Récepteur infrarouge (télécommande)
// =============================================
#define PIN_IR_RECV 26

// =============================================
//  PINS — Lecteur RFID (MFRC522 via SPI)
// =============================================
#define PIN_RFID_SS 5
#define PIN_RFID_RST 22
// SCK=18, MISO=23, MOSI=19 → pins SPI hardware ESP32 (fixes)

// =============================================
//  DÉLAIS PAR DÉFAUT (millisecondes)
//  Stockés en flash (Preferences), mis à jour à distance si besoin.
// =============================================
#define DEFAULT_DELAI_OUVERTURE_MS 3000 // A : temps max pour ouvrir après OPEN
#define DEFAULT_DELAI_FERMETURE_MS                                             \
  10000                               // B : temps max pour fermer avant buzzer
#define DEFAULT_DELAI_BUZZER_MS 10000 // X : durée buzzer avant BUZZER_TIMEOUT

// =============================================
//  CONSTANTES APPLICATIVES
// =============================================
#define NB_CHIFFRES_CODE 4       // Code de retrait = 4 chiffres
#define DUREE_AFFICHAGE_ERR 3000 // Durée affichage "Err" avant retour idle (ms)

#endif // CONFIG_H
