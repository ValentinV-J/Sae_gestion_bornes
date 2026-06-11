const Log = require('../models/Log');
const { sendSuccess, sendError } = require('../utils/response');

// GET /api/logs?niveau=CRITICAL&borne_id=xxx&limit=50
exports.getLogs = async (req, res) => {
  try {
    const { niveau, borne_id, limit = 100 } = req.query;
    const filtre = {};
    if (niveau) filtre.niveau = niveau.toUpperCase();
    if (borne_id) filtre.borne_id = borne_id;

    const logs = await Log.find(filtre)
      .sort({ horodatage: -1 })
      .limit(parseInt(limit))
      .populate('borne_id', 'nom identifiant');

    return sendSuccess(res, logs);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// POST /api/logs — Utilisé par le serveur Java pour signaler des erreurs µC
exports.createLog = async (req, res) => {
  try {
    const log = new Log(req.body);
    const saved = await log.save();
    return sendSuccess(res, saved, 201);
  } catch (err) {
    return sendError(res, `Données invalides : ${err.message}`, 400);
  }
};
