const Borne = require('../models/Borne');

// GET /api/bornes
// Retourne toutes les bornes avec leurs casiers et leurs états.
exports.getAllBornes = async (req, res) => {
  try {
    const bornes = await Borne.find();
    res.status(200).json(bornes);
  } catch (err) {
    res.status(500).json({ error: 'Erreur serveur.', details: err.message });
  }
};

// GET /api/bornes/:id
// Retourne une borne spécifique par son ObjectId MongoDB.
exports.getBorneById = async (req, res) => {
  try {
    const borne = await Borne.findById(req.params.id);
    if (!borne) return res.status(404).json({ error: 'Borne introuvable.' });
    res.status(200).json(borne);
  } catch (err) {
    res.status(500).json({ error: 'Erreur serveur.', details: err.message });
  }
};

// POST /api/bornes
// Crée une nouvelle borne. Les casiers doivent être envoyés dans le body.
exports.createBorne = async (req, res) => {
  try {
    const borne = new Borne(req.body);
    const saved = await borne.save();
    res.status(201).json(saved);
  } catch (err) {
    res.status(400).json({ error: 'Données invalides.', details: err.message });
  }
};

// PUT /api/bornes/:id/settings
// Met à jour les paramètres A, B, X, Y d'une borne.
// Passe needs_update à true pour que le serveur Java transmette les nouveaux réglages au µC.
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
          'parametres_attente.needs_update': true, // Signal pour le serveur Java
        }
      },
      { new: true, runValidators: true }
    );

    if (!borne) return res.status(404).json({ error: 'Borne introuvable.' });
    res.status(200).json(borne);
  } catch (err) {
    res.status(400).json({ error: 'Données invalides.', details: err.message });
  }
};

// PUT /api/bornes/:id
// Modifie les infos générales d'une borne (nom, adresse).
exports.updateBorne = async (req, res) => {
  try {
    const borne = await Borne.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });
    if (!borne) return res.status(404).json({ error: 'Borne introuvable.' });
    res.status(200).json(borne);
  } catch (err) {
    res.status(400).json({ error: 'Données invalides.', details: err.message });
  }
};
