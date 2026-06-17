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

    // 2. Créer la borne "Borne_Belfort" avec 3 casiers vides
    await Borne.deleteMany({ nom: 'Borne_Belfort' });
    const borne = new Borne({
      identifiant: 'B01',
      nom: 'Borne_Belfort',
      adresse: 'IUT Belfort',
      parametres_attente: {
        delai_A: 10,
        delai_B: 10,
        delai_X: 10,
        delai_Y: 10,
        needs_update: true
      },
      casiers: [
        { numero: 1, taille: 'S', etat_occupation: 'VIDE', etat_materiel: 'OK' },
        { numero: 2, taille: 'M', etat_occupation: 'VIDE', etat_materiel: 'OK' },
        { numero: 3, taille: 'L', etat_occupation: 'VIDE', etat_materiel: 'OK' }
      ]
    });
    const savedBorne = await borne.save();
    console.log('🏗️ Borne de test "Borne Belfort" créée avec 3 casiers vides !');

    // 3. Créer des colis virtuels (comme s'ils avaient été scannés par l'app mobile)
    const Colis = require('./models/Colis');
    await Colis.deleteMany({ uuid: { $in: ['test-colis-uuid-1', 'test-colis-uuid-2'] } });
    
    await Colis.create([
      {
        uuid: 'test-colis-uuid-1',
        statut: 'ATTENTE_DEPOT',
        livreur_id: livreur._id,
        borne_id: borne._id,
        email_client: 'xpbot5695@gmail.com'
      },
      {
        uuid: 'test-colis-uuid-2',
        statut: 'ATTENTE_DEPOT',
        livreur_id: livreur._id,
        borne_id: borne._id,
        email_client: 'xpbot8477@gmail.com'
      }
    ]);
    console.log('📦 2 Colis de test "ATTENTE_DEPOT" créés !');

    console.log('🎉 Tout est prêt pour le test physique !');
    process.exit(0);
  } catch (err) {
    console.error('❌ Erreur :', err);
    process.exit(1);
  }
};

seedData();
