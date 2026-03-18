const mongoose = require('mongoose');

// --- Schéma : Log ---
// Trace toutes les actions et erreurs du système.
// Alimenté principalement par le Serveur Java (erreurs µC) et les actions admin.
// Utilisé par le front-end pour le tableau de bord de surveillance.
const LogSchema = new mongoose.Schema({
  horodatage: {
    type: Date,
    default: Date.now,
    index: true, // Indexé pour trier et filtrer rapidement par date
  },
  niveau: {
    type: String,
    enum: ['INFO', 'WARNING', 'CRITICAL'],
    default: 'INFO',
  },
  // Code court identifiant l'action (ex: 'OUVERTURE_CASIER', 'ERR_FERMETURE', 'BADGE_INCONNU')
  action: {
    type: String,
    required: true,
    trim: true,
    uppercase: true,
  },
  // Références optionnelles (null si l'action n'est pas liée à une borne ou un casier spécifique)
  borne_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Borne',
    default: null,
  },
  casier_numero: {
    type: Number,
    default: null,
  },
  // Texte libre pour ajouter du contexte (ex: "Badge inconnu lu: A1B2C3")
  details: {
    type: String,
    default: '',
    trim: true,
  },
});

// Pas de timestamps: true ici car on gère l'horodatage manuellement avec le champ 'horodatage'
module.exports = mongoose.model('Log', LogSchema);
