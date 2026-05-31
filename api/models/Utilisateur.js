const mongoose = require('mongoose');
const bcrypt = require('bcrypt');

// --- Schéma : Utilisateur (Admin du site web) ---
// Distinct du Livreur : l'admin s'identifie avec login/mot de passe classique.
const UtilisateurSchema = new mongoose.Schema({
  identifiant: {
    type: String,
    required: true,
    unique: true,
    trim: true,
    lowercase: true, // Normalisation pour éviter "Admin" vs "admin"
  },
  // Le mot de passe en clair n'est JAMAIS stocké. On stocke uniquement le hash bcrypt.
  mot_de_passe: {
    type: String,
    required: true,
    select: false, // N'est jamais retourné dans les réponses API
  },
  role: {
    type: String,
    enum: ['ADMIN', 'TECHNICIEN'],
    default: 'TECHNICIEN',
    // ADMIN : accès complet (create/edit/delete bornes, livreurs, utilisateurs)
    // TECHNICIEN : accès en lecture + modification des paramètres µC uniquement
  },
  derniere_connexion: {
    type: Date,
    default: null,
  },
}, {
  timestamps: true,
});

// --- Middleware Mongoose : Hachage automatique avant sauvegarde ---
// Ce hook s'exécute avant chaque .save(). Il hache le mot de passe si et seulement
// s'il a été modifié, pour éviter de re-hacher un hash existant.
UtilisateurSchema.pre('save', async function (next) {
  if (!this.isModified('mot_de_passe')) return next();
  const salt = await bcrypt.genSalt(10);
  this.mot_de_passe = await bcrypt.hash(this.mot_de_passe, salt);
  next();
});

// --- Méthode d'instance : Vérification du mot de passe ---
// Utilisée dans le controller d'auth pour comparer le mot de passe saisi avec le hash stocké.
UtilisateurSchema.methods.verifierMotDePasse = async function (motDePasseSaisi) {
  return bcrypt.compare(motDePasseSaisi, this.mot_de_passe);
};

module.exports = mongoose.model('Utilisateur', UtilisateurSchema);
