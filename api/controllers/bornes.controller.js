const Borne = require('../models/Borne');
const { sendSuccess, sendError } = require('../utils/response');

// GET /api/bornes
exports.getAllBornes = async (req, res) => {
  try {
    const bornes = await Borne.find();
    return sendSuccess(res, bornes);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// GET /api/bornes/:id
exports.getBorneById = async (req, res) => {
  try {
    const borne = await Borne.findById(req.params.id);
    if (!borne) return sendError(res, 'Borne introuvable.', 404);
    return sendSuccess(res, borne);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// POST /api/bornes
exports.createBorne = async (req, res) => {
  try {
    const borne = new Borne(req.body);
    const saved = await borne.save();
    return sendSuccess(res, saved, 201);
  } catch (err) {
    return sendError(res, `Données invalides : ${err.message}`, 400);
  }
};

// PUT /api/bornes/:id
exports.updateBorne = async (req, res) => {
  try {
    const borne = await Borne.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });
    if (!borne) return sendError(res, 'Borne introuvable.', 404);
    return sendSuccess(res, borne);
  } catch (err) {
    return sendError(res, `Données invalides : ${err.message}`, 400);
  }
};

// PUT /api/bornes/:id/settings — Met à jour A,B,X,Y et active le flag needs_update
exports.updateSettings = async (req, res) => {
  try {
    const { delai_A, delai_B, delai_X, delai_Y } = req.body;
    const borne = await Borne.findByIdAndUpdate(
      req.params.id,
      {
        $set: {
          'parametres_attente.delai_A': delai_A,
          'parametres_attente.delai_B': delai_B,
          'parametres_attente.delai_X': delai_X,
          'parametres_attente.delai_Y': delai_Y,
          'parametres_attente.needs_update': true,
        }
      },
      { new: true, runValidators: true }
    );
    if (!borne) return sendError(res, 'Borne introuvable.', 404);
    return sendSuccess(res, borne);
  } catch (err) {
    return sendError(res, `Données invalides : ${err.message}`, 400);
  }
};
