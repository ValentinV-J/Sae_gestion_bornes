const Colis = require('../models/Colis');
const Borne = require('../models/Borne');
const { sendSuccess, sendError } = require('../utils/response');
const nodemailer = require('nodemailer');

// ==========================================
// ROUTES POUR LE MOBILE (Livreur)
// ==========================================

// GET /api/colis/:uuid
exports.getColisByUuid = async (req, res) => {
  try {
    const colis = await Colis.findOne({ uuid: req.params.uuid });
    if (!colis) return sendError(res, 'Colis introuvable', 404);
    return sendSuccess(res, colis);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// GET /api/colis (Admin Web)
exports.getAllColis = async (req, res) => {
  try {
    const colis = await Colis.find().sort({ createdAt: -1 });
    return sendSuccess(res, colis);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// PATCH /api/colis/:uuid/scan
exports.scanColis = async (req, res) => {
  try {
    const colis = await Colis.findOneAndUpdate(
      { uuid: req.params.uuid, statut: 'ATTENTE_DEPOT' },
      { statut: 'SCAN_MOBILE_OK' },
      { new: true }
    );
    if (!colis) return sendError(res, 'Colis introuvable ou déjà scanné', 404);
    return sendSuccess(res, colis);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};


// ==========================================
// ROUTES POUR LE SERVEUR JAVA
// ==========================================

// GET /api/colis/depot?borne_id=...
// Le livreur badge sur la borne. On cherche un colis en "SCAN_MOBILE_OK" pour cette borne.
// Et on lui attribue un casier vide.
exports.getCasierPourDepot = async (req, res) => {
  try {
    const { borne_id } = req.query;
    if (!borne_id) return sendError(res, 'borne_id manquant', 400);

    // 1. Trouver un colis scanné pour cette borne
    const colis = await Colis.findOne({ borne_id, statut: 'SCAN_MOBILE_OK' });
    if (!colis) return sendError(res, 'Aucun colis scanné en attente pour cette borne', 404);

    // 2. Trouver la borne et un casier vide
    const borne = await Borne.findById(borne_id);
    if (!borne) return sendError(res, 'Borne introuvable', 404);

    const casierLibre = borne.casiers.find(c => c.etat_occupation === 'VIDE' && c.etat_materiel === 'OK');
    if (!casierLibre) return sendError(res, 'Aucun casier vide disponible', 404);

    // 3. Attribuer le casier au colis (en attendant la confirmation physique)
    colis.casier_numero = casierLibre.numero;
    await colis.save();

    // 4. Préparer la réponse (inclure A, B, X, Y si needs_update = true)
    const responseData = {
      casier_numero: casierLibre.numero,
      uuid: colis.uuid
    };

    if (borne.parametres_attente && borne.parametres_attente.needs_update) {
      responseData.parametres = {
        A: borne.parametres_attente.delai_A,
        B: borne.parametres_attente.delai_B,
        X: borne.parametres_attente.delai_X,
        Y: borne.parametres_attente.delai_Y
      };
      // Réinitialiser le flag
      borne.parametres_attente.needs_update = false;
      await borne.save();
    }

    return sendSuccess(res, responseData);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// PATCH /api/colis/:uuid/depose
// La porte s'est refermée, le dépôt physique est confirmé.
exports.marquerColisDepose = async (req, res) => {
  try {
    const { uuid } = req.params;
    const { livreur_id } = req.body; // optionnel pour log
    
    // Générer un code à 4 chiffres aléatoire
    const code_retrait = Math.floor(1000 + Math.random() * 9000).toString();

    const colis = await Colis.findOneAndUpdate(
      { uuid },
      { 
        statut: 'DEPOSE', 
        code_retrait,
        date_depot: new Date()
      },
      { new: true, select: '+code_retrait' } // On force à renvoyer le code
    );

    if (!colis) return sendError(res, 'Colis introuvable', 404);

    // Marquer le casier comme OCCUPE
    await Borne.findOneAndUpdate(
      { _id: colis.borne_id, "casiers.numero": colis.casier_numero },
      { $set: { "casiers.$.etat_occupation": "OCCUPE" } }
    );

    // Envoi de l'e-mail si configuré et si l'e-mail client existe
    if (process.env.EMAIL_USER && process.env.EMAIL_PASS && colis.email_client) {
      try {
        const transporter = nodemailer.createTransport({
          service: 'gmail',
          auth: {
            user: process.env.EMAIL_USER,
            pass: process.env.EMAIL_PASS
          }
        });

        const mailOptions = {
          from: process.env.EMAIL_USER,
          to: colis.email_client,
          subject: '📦 Votre colis a été livré !',
          html: `
            <div style="font-family: sans-serif; text-align: center; padding: 20px;">
              <h2>Bonjour,</h2>
              <p>Votre colis est disponible dans la borne.</p>
              <p>Voici votre code secret de retrait :</p>
              <h1 style="color: #4CAF50; font-size: 48px; letter-spacing: 5px;">${colis.code_retrait}</h1>
              <p>Merci de le récupérer rapidement !</p>
            </div>
          `
        };

        await transporter.sendMail(mailOptions);
        console.log(`📧 E-mail envoyé avec succès à ${colis.email_client}`);
      } catch (mailErr) {
        console.error('❌ Erreur lors de l\'envoi de l\'e-mail :', mailErr);
        // On ne bloque pas la requête si l'email échoue
      }
    }

    return sendSuccess(res, { code_retrait: colis.code_retrait });
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// GET /api/colis/retrait?borne_id=...&code_retrait=...
// Le client a tapé son code sur la télécommande IR
exports.getCasierPourRetrait = async (req, res) => {
  try {
    const { borne_id, code_retrait } = req.query;
    if (!borne_id || !code_retrait) return sendError(res, 'Paramètres manquants', 400);

    const colis = await Colis.findOne({ borne_id, code_retrait, statut: 'DEPOSE' });
    if (!colis) return sendError(res, 'Code invalide ou colis déjà retiré', 404);

    return sendSuccess(res, { casier_numero: colis.casier_numero, uuid: colis.uuid });
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// PATCH /api/colis/:uuid/retire
// La porte s'est refermée, le client a pris son colis.
// Si l'uuid est inconnu (ex: timeout), le serveur Java utilise marquerColisRetire("?borne_id=...&casier_numero=...")
exports.marquerColisRetire = async (req, res) => {
  try {
    let colis;
    
    if (req.params.uuid && req.params.uuid !== 'unknown') {
      colis = await Colis.findOneAndUpdate(
        { uuid: req.params.uuid },
        { statut: 'RETIRE', date_retrait: new Date() },
        { new: true }
      );
    } else {
      // Cas de fallback via querystring
      const { borne_id, casier_numero } = req.query;
      colis = await Colis.findOneAndUpdate(
        { borne_id, casier_numero, statut: 'DEPOSE' },
        { statut: 'RETIRE', date_retrait: new Date() },
        { new: true }
      );
    }

    if (!colis) return sendError(res, 'Colis introuvable', 404);

    // Marquer le casier comme VIDE
    await Borne.findOneAndUpdate(
      { _id: colis.borne_id, "casiers.numero": colis.casier_numero },
      { $set: { "casiers.$.etat_occupation": "VIDE" } }
    );

    return sendSuccess(res, colis);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};
