const Log = require('../models/Log');

// GET /api/logs
// Retourne les logs du système avec filtres optionnels via query params.
// Exemples :
//   GET /api/logs?niveau=CRITICAL
//   GET /api/logs?borne_id=ABC123
//   GET /api/logs?limit=50
exports.getLogs = async (req, res) => {
  try {
    const { niveau, borne_id, limit = 100 } = req.query;

    // Construction du filtre dynamique
    const filtre = {};
    if (niveau) filtre.niveau = niveau.toUpperCase();
    if (borne_id) filtre.borne_id = borne_id;

    const logs = await Log.find(filtre)
      .sort({ horodatage: -1 })       // Plus récents d'abord
      .limit(parseInt(limit))
      .populate('borne_id', 'nom identifiant'); // Enrichit avec le nom de la borne

    res.status(200).json(logs);
  } catch (err) {
    res.status(500).json({ error: 'Erreur serveur.', details: err.message });
  }
};

// POST /api/logs
// Permet au Serveur Java de créer un log via l'API (erreur µC, action, etc.)
exports.createLog = async (req, res) => {
  try {
    const log = new Log(req.body);
    const saved = await log.save();
    res.status(201).json(saved);
  } catch (err) {
    res.status(400).json({ error: 'Données invalides.', details: err.message });
  }
};
