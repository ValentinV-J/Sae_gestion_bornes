/**
 * Script de seed — Crée un premier utilisateur ADMIN en base de données.
 * Le mot de passe est automatiquement haché par le middleware Mongoose (pre-save).
 *
 * Usage : node seed.js
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Utilisateur = require('./models/Utilisateur');

const seed = async () => {
  try {
    console.log('🔗 Connexion à MongoDB...');
    await mongoose.connect(process.env.MONGO_URI);
    console.log('✅ Connecté à MongoDB');

    // Vérifie si un admin existe déjà
    const existant = await Utilisateur.findOne({ identifiant: 'admin' });
    if (existant) {
      console.log('ℹ️  Un admin existe déjà, seed ignoré.');
      process.exit(0);
    }

    // Crée l'admin — le hook pre-save de Mongoose hache le mot de passe automatiquement
    const admin = new Utilisateur({
      identifiant: 'admin',
      mot_de_passe: 'Admin1234!',
      role: 'ADMIN',
    });

    await admin.save();
    console.log('🎉 Admin créé avec succès !');
    console.log('   Identifiant : admin');
    console.log('   Mot de passe : Admin1234!');
    console.log('   ⚠️  Changez ce mot de passe en production !');

    process.exit(0);
  } catch (err) {
    console.error('❌ Erreur seed :', err.message);
    process.exit(1);
  }
};

seed();
