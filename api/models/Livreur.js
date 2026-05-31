const mongoose = require('mongoose');

// --- Schéma : Livreur ---
// Un livreur est un professionnel autorisé à déposer des colis sur les bornes.
// Son accès physique est contrôlé par son badge RFID.
const LivreurSchema = new mongoose.Schema({
  nom: {
    type: String,
    required: true,
    trim: true,
  },
  prenom: {
    type: String,
    trim: true,
    default: '',
  },
  societe: {
    type: String,
    trim: true,
    default: '',
  },
  // L'ID lu physiquement par le lecteur RFID de la borne.
  // C'est l'identifiant clé qui fait le lien entre le badge et cette entrée en BDD.
  id_badge_rfid: {
    type: String,
    required: true,
    unique: true,
    trim: true,
    uppercase: true, // On normalise en majuscules pour éviter les doublons "a1b2" vs "A1B2"
  },
  actif: {
    type: Boolean,
    default: true, // Permet de désactiver un badge sans le supprimer (meilleure pratique)
  },
}, {
  timestamps: true,
});

module.exports = mongoose.model('Livreur', LivreurSchema);
