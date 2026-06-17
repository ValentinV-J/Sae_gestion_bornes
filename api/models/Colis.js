const mongoose = require('mongoose');
const { v4: uuidv4 } = require('uuid');

// --- Schéma : Colis ---
// Représente un colis en transit dans le système.
// C'est l'entité qui évolue le plus (changements de statut).
const ColisSchema = new mongoose.Schema({
  // UUID issu du QR Code scanné par l'application mobile du livreur.
  // Sert d'identifiant métier unique à l'échelle mondiale.
  uuid: {
    type: String,
    required: true,
    unique: true,
    default: uuidv4,
  },

  // E-mail du client (obligatoire pour envoyer le code de retrait)
  email_client: {
    type: String,
    required: true,
  },

  // Cycle de vie complet du colis (voir Phase 2 pour le protocole des transitions)
  // ATTENTE_DEPOT   -> état initial, colis déclaré mais pas encore scanné
  // SCAN_MOBILE_OK  -> le livreur a scanné le QR code, en attente du badge sur la borne
  // DEPOSE          -> colis physiquement dans le casier, code de retrait généré
  // RETIRE          -> client a récupéré son colis, casier libéré
  statut: {
    type: String,
    enum: ['ATTENTE_DEPOT', 'SCAN_MOBILE_OK', 'DEPOSE', 'RETIRE'],
    default: 'ATTENTE_DEPOT',
  },

  // Généré par le serveur Java UNIQUEMENT quand le statut passe à DEPOSE.
  // Non exposé publiquement dans les réponses API (sécurité).
  code_retrait: {
    type: String,
    default: null,
    select: false, // N'est pas retourné par défaut dans les requêtes GET
  },

  // Références (on stocke l'ID ObjectId de MongoDB pour les jointures)
  livreur_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Livreur',
    required: true,
  },
  borne_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Borne',
    required: true,
  },

  // Numéro du casier dans la borne cible (référence logique, pas un ObjectId)
  casier_numero: {
    type: Number,
    default: null,
  },

  // Horodatages métier (distincts de createdAt/updatedAt)
  date_depot: {
    type: Date,
    default: null,
  },
  date_retrait: {
    type: Date,
    default: null,
  },
}, {
  timestamps: true, // createdAt = date de création du document (réservation initiale)
});

module.exports = mongoose.model('Colis', ColisSchema);
