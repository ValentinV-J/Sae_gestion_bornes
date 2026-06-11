#ifndef CONFIG_H
#define CONFIG_H

// =============================================
//  CONFIGURATION RÉSEAU
// =============================================
#define WIFI_SSID       "NOM_DU_WIFI"          // À changer selon l'AP utilisé
#define WIFI_PASSWORD   "MOT_DE_PASSE_WIFI"    // À changer selon l'AP utilisé
#define SERVER_IP       "192.168.1.XX"         // IP de la machine où tourne le serveur Java
#define SERVER_PORT     9000                   // Port TCP du serveur Java

// =============================================
//  IDENTIFIANT DE CETTE BORNE
// =============================================
#define BORNE_ID        "B01"                  // Identifiant unique de cette borne physique

// =============================================
//  PINS — Afficheur LCD 4 digits (TM1637)
// =============================================
#define PIN_LCD_CLK     33
#define PIN_LCD_DIO     32

// =============================================
//  PINS — Buzzer
// =============================================
#define PIN_BUZZER      25

// =============================================
//  PINS — Switch à glissière (simulation porte)
// =============================================
#define PIN_SWITCH      27

// =============================================
//  PINS — Récepteur infrarouge (télécommande)
// =============================================
#define PIN_IR_RECV     26

// =============================================
//  PINS — Lecteur RFID (MFRC522 via SPI)
// =============================================
#define PIN_RFID_SS     5    // NSS / SDA (Slave Select)
#define PIN_RFID_RST    22   // Reset
// SCK=18, MISO=23, MOSI=19 sont les pins SPI hardware de l'ESP32 (fixes, pas besoin de #define)

// =============================================
//  DÉLAIS PAR DÉFAUT (en millisecondes)
//  Ces valeurs peuvent être mises à jour à distance par le serveur Java.
//  Elles sont stockées en mémoire flash (Preferences) pour survivre aux redémarrages.
// =============================================
#define DEFAULT_DELAI_OUVERTURE_MS   3000   // Délai A : temps pour ouvrir la porte
#define DEFAULT_DELAI_FERMETURE_MS  10000   // Délai B : temps pour fermer avant buzzer
#define DEFAULT_DELAI_BUZZER_MS     10000   // Délai X : durée du buzzer avant erreur définitive

// =============================================
//  CONSTANTES APPLICATIVES
// =============================================
#define NB_CHIFFRES_CODE    4    // Nombre de chiffres du code de retrait client
#define DUREE_AFFICHAGE_ERR 3000 // Durée d'affichage de "Err" en ms avant retour idle

#endif // CONFIG_H
