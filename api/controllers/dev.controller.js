const mongoose = require('mongoose');
const Borne = require('../models/Borne');
const Colis = require('../models/Colis');
const Livreur = require('../models/Livreur');
const Log = require('../models/Log');
const { sendSuccess, sendError } = require('../utils/response');

exports.resetDatabase = async (req, res) => {
  try {
    // 1. Nettoyer les collections (sauf Utilisateur pour garder l'admin)
    await Borne.deleteMany({});
    await Colis.deleteMany({});
    await Livreur.deleteMany({});
    await Log.deleteMany({});

    // 2. Recréer le livreur de test obligatoire
    const rfidTest = 'CCFC82B9';
    const livreurTest = await Livreur.create({
      nom: 'Dupont',
      prenom: 'Jean',
      societe: 'Amazon',
      id_badge_rfid: rfidTest
    });

    // 3. Créer quelques faux livreurs pour les stats
    const fakeLivreurs = [];
    const societes = ['Chronopost', 'Colissimo', 'UPS', 'DHL', 'FedEx'];
    for (let i = 0; i < 5; i++) {
      const l = await Livreur.create({
        nom: `Nom${i}`,
        prenom: `Prenom${i}`,
        societe: societes[i % societes.length],
        id_badge_rfid: `FAKERFID${i}`
      });
      fakeLivreurs.push(l);
    }

    // 4. Recréer la Borne1 obligatoire pour la maquette
    const borneTest = await Borne.create({
      identifiant: 'B01',
      nom: 'Borne1',
      adresse: 'IUT Belfort',
      casiers: [
        { numero: 1, taille: 'S', etat_occupation: 'VIDE', etat_materiel: 'OK' },
        { numero: 2, taille: 'M', etat_occupation: 'VIDE', etat_materiel: 'OK' },
        { numero: 3, taille: 'L', etat_occupation: 'VIDE', etat_materiel: 'OK' }
      ]
    });

    // 5. Créer des bornes fictives
    const fakeBornes = [];
    const villes = ['Paris', 'Lyon', 'Marseille', 'Strasbourg', 'Lille'];
    for (let i = 0; i < 5; i++) {
      const b = await Borne.create({
        identifiant: `B0${i+2}`,
        nom: `Borne-${villes[i]}`,
        adresse: `Gare de ${villes[i]}`,
        casiers: [
          { numero: 1, taille: 'M', etat_occupation: 'VIDE', etat_materiel: 'OK' },
          { numero: 2, taille: 'L', etat_occupation: 'VIDE', etat_materiel: 'OK' }
        ]
      });
      fakeBornes.push(b);
    }

    // 6. Créer le colis de TEST prêt à être scanné
    await Colis.create({
      uuid: 'test-colis-uuid-123',
      email_client: process.env.EMAIL_USER || 'ton.email@gmail.com',
      statut: 'ATTENTE_DEPOT',
      livreur_id: livreurTest._id,
      borne_id: borneTest._id
    });

    // 7. Générer 50 colis réalistes dans le passé (pour les graphiques)
    const allBornes = [borneTest, ...fakeBornes];
    const allLivreurs = [livreurTest, ...fakeLivreurs];

    for (let i = 0; i < 50; i++) {
      // Date aléatoire dans les 30 derniers jours
      const randomDaysAgo = Math.floor(Math.random() * 30);
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - randomDaysAgo);

      // Statut aléatoire avec un biais pour RETIRE (le plus fréquent)
      const randStatut = Math.random();
      let statut = 'RETIRE';
      if (randStatut > 0.8) statut = 'DEPOSE';
      else if (randStatut > 0.9) statut = 'SCAN_MOBILE_OK';
      else if (randStatut > 0.95) statut = 'ATTENTE_DEPOT';

      const randomBorne = allBornes[Math.floor(Math.random() * allBornes.length)];
      const randomLivreur = allLivreurs[Math.floor(Math.random() * allLivreurs.length)];

      await Colis.create({
        uuid: `fake-uuid-${i}`,
        email_client: `client${i}@example.com`,
        statut: statut,
        livreur_id: randomLivreur._id,
        borne_id: randomBorne._id,
        date_depot: (statut === 'DEPOSE' || statut === 'RETIRE') ? pastDate : null,
        date_retrait: (statut === 'RETIRE') ? new Date(pastDate.getTime() + 86400000) : null,
        createdAt: new Date(pastDate.getTime() - 86400000)
      });
    }

    // Générer quelques logs pour montrer de l'activité
    await Log.create({
      niveau: 'INFO',
      action: 'SYSTEME_RESET',
      details: 'La base de données a été réinitialisée avec des données générées aléatoirement.'
    });

    return sendSuccess(res, { message: 'Base de données réinitialisée avec succès !' });
  } catch (err) {
    console.error(err);
    return sendError(res, `Erreur lors de la réinitialisation : ${err.message}`, 500);
  }
};
