require('dotenv').config();
const mongoose = require('mongoose');
const Livreur = require('./models/Livreur');
const Borne = require('./models/Borne');

const seedData = async () => {
  try {
    console.log('🔗 Connexion à MongoDB...');
    await mongoose.connect(process.env.MONGO_URI);
    console.log('✅ Connecté !');

    // 1. Créer le livreur de test avec le badge de l'utilisateur
    const rfid = 'CCFC82B9';
    await Livreur.deleteMany({ id_badge_rfid: rfid });
    const livreur = new Livreur({
      nom: 'Dupont',
      prenom: 'Jean',
      societe: 'Amazon',
      id_badge_rfid: rfid
    });
    await livreur.save();
    console.log(`📦 Livreur de test créé (Badge: ${rfid})`);

    // 2. Créer la borne "Borne1" avec 3 casiers vides
    await Borne.deleteMany({ nom: 'Borne1' });
    const borne = new Borne({
      identifiant: 'B01',
      nom: 'Borne1',
      adresse: 'IUT Belfort',
      casiers: [
        { numero: 1, taille: 'S', etat_occupation: 'VIDE', etat_materiel: 'OK' },
        { numero: 2, taille: 'M', etat_occupation: 'VIDE', etat_materiel: 'OK' },
        { numero: 3, taille: 'L', etat_occupation: 'VIDE', etat_materiel: 'OK' }
      ]
    });
    const savedBorne = await borne.save();
    console.log('🏗️ Borne de test "Borne1" créée avec 3 casiers vides !');

    // 3. Créer un colis virtuel (comme s'il avait été scanné par l'app mobile)
    const Colis = require('./models/Colis');
    await Colis.deleteMany({});
    const colis = new Colis({
      uuid: 'test-colis-uuid-123',
      email_client: 'ton.email@gmail.com', // Remplacer par ton vrai e-mail
      statut: 'ATTENTE_DEPOT',
      livreur_id: livreur._id,
      borne_id: savedBorne._id
    });
    await colis.save();
    console.log('📦 Colis de test créé et prêt à être déposé !');

    console.log('🎉 Tout est prêt pour le test physique !');
    process.exit(0);
  } catch (err) {
    console.error('❌ Erreur :', err);
    process.exit(1);
  }
};

seedData();
