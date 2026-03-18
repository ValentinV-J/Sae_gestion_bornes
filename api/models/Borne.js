const mongoose = require('mongoose');

// --- Sous-schéma : Un Casier (imbriqué dans la Borne) ---
// Un casier ne peut pas exister sans borne, donc on l'intègre directement.
const CasierSchema = new mongoose.Schema({
  numero: {
    type: Number,
    required: true,
  },
  taille: {
    type: String,
    enum: ['S', 'M', 'L'],
    required: true,
  },
  etat_occupation: {
    type: String,
    enum: ['VIDE', 'OCCUPE'],
    default: 'VIDE',
  },
  etat_materiel: {
    type: String,
    enum: ['OK', 'ERREUR_OUVERTURE', 'ERREUR_FERMETURE'],
    default: 'OK',
  },
}, { _id: false }); // Pas besoin d'un _id séparé pour chaque casier


// --- Sous-schéma : Paramètres d'attente envoyés au µC ---
// A, B, X, Y sont les délais en secondes pour le buzzer et les attentes d'ouverture/fermeture.
const ParametresAttenteSchema = new mongoose.Schema({
  delai_A: { type: Number, default: 3 },  // Temps pour ouvrir la porte (secondes)
  delai_B: { type: Number, default: 10 }, // Temps pour fermer la porte (secondes)
  delai_X: { type: Number, default: 10 }, // Temps avant déclenchement du buzzer (secondes)
  delai_Y: { type: Number, default: 10 }, // Durée du buzzer avant erreur définitive (secondes)
  needs_update: {
    type: Boolean,
    default: false, // Passe à true quand l'admin modifie les délais via le front-end.
                    // Le serveur Java le remet à false après avoir transmis les nouveaux réglages au µC.
  },
}, { _id: false });


// --- Schéma Principal : Borne ---
const BorneSchema = new mongoose.Schema({
  // Identifiant lisible par les humains (ex: "B01"), utilisé dans les communications µC <-> Java
  identifiant: {
    type: String,
    required: true,
    unique: true,
    trim: true,
    uppercase: true,
  },
  nom: {
    type: String,
    required: true,
    trim: true,
  },
  adresse: {
    type: String,
    required: true,
    trim: true,
  },
  parametres_attente: {
    type: ParametresAttenteSchema,
    default: () => ({}), // Utilise les valeurs par défaut du sous-schéma
  },
  // Les casiers sont imbriqués directement : cohérent avec la philosophie NoSQL MongoDB.
  // Une seule requête suffit pour lire la borne et tous ses casiers.
  casiers: {
    type: [CasierSchema],
    default: [],
  },
}, {
  timestamps: true, // Ajoute automatiquement createdAt et updatedAt
});

module.exports = mongoose.model('Borne', BorneSchema);
