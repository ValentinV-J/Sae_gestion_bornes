const Livreur = require('../models/Livreur');
const { sendSuccess, sendError } = require('../utils/response');

// GET /api/livreurs
exports.getAllLivreurs = async (req, res) => {
  try {
    const livreurs = await Livreur.find();
    return sendSuccess(res, livreurs);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// GET /api/livreurs/rfid/:rfid
exports.getLivreurByRfid = async (req, res) => {
  try {
    const livreur = await Livreur.findOne({ id_badge_rfid: req.params.rfid });
    if (!livreur) return sendError(res, 'Badge RFID inconnu', 404);
    return sendSuccess(res, livreur);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// POST /api/livreurs
exports.createLivreur = async (req, res) => {
  try {
    const livreur = new Livreur(req.body);
    const saved = await livreur.save();
    return sendSuccess(res, saved, 201);
  } catch (err) {
    return sendError(res, `Données invalides : ${err.message}`, 400);
  }
};

// PUT /api/livreurs/:id
exports.updateLivreur = async (req, res) => {
  try {
    const livreur = await Livreur.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });
    if (!livreur) return sendError(res, 'Livreur introuvable.', 404);
    return sendSuccess(res, livreur);
  } catch (err) {
    return sendError(res, `Données invalides : ${err.message}`, 400);
  }
};

// DELETE /api/livreurs/:id
exports.deleteLivreur = async (req, res) => {
  try {
    const livreur = await Livreur.findByIdAndDelete(req.params.id);
    if (!livreur) return sendError(res, 'Livreur introuvable.', 404);
    return sendSuccess(res, 'Livreur supprimé avec succès.', 200);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};
