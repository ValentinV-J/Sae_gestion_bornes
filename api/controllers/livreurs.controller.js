const Livreur = require('../models/Livreur');

// GET /api/livreurs
// Retourne la liste de tous les livreurs autorisés.
exports.getAllLivreurs = async (req, res) => {
  try {
    const livreurs = await Livreur.find();
    res.status(200).json(livreurs);
  } catch (err) {
    res.status(500).json({ error: 'Erreur serveur.', details: err.message });
  }
};

// POST /api/livreurs
// Enregistre un nouveau livreur avec son badge RFID.
exports.createLivreur = async (req, res) => {
  try {
    const livreur = new Livreur(req.body);
    const saved = await livreur.save();
    res.status(201).json(saved);
  } catch (err) {
    res.status(400).json({ error: 'Données invalides.', details: err.message });
  }
};

// PUT /api/livreurs/:id
// Modifie les informations d'un livreur (nom, badge, statut actif).
exports.updateLivreur = async (req, res) => {
  try {
    const livreur = await Livreur.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });
    if (!livreur) return res.status(404).json({ error: 'Livreur introuvable.' });
    res.status(200).json(livreur);
  } catch (err) {
    res.status(400).json({ error: 'Données invalides.', details: err.message });
  }
};

// DELETE /api/livreurs/:id
// Supprime un livreur (son badge RFID ne sera plus accepté par les bornes).
exports.deleteLivreur = async (req, res) => {
  try {
    const livreur = await Livreur.findByIdAndDelete(req.params.id);
    if (!livreur) return res.status(404).json({ error: 'Livreur introuvable.' });
    res.status(204).send(); // 204 No Content = succès sans corps de réponse
  } catch (err) {
    res.status(500).json({ error: 'Erreur serveur.', details: err.message });
  }
};
