const Borne = require('../models/Borne');
const { sendSuccess, sendError } = require('../utils/response');

// GET /api/bornes (accepte ?nom=...)
exports.getAllBornes = async (req, res) => {
  try {
    const filter = req.query.nom ? { nom: req.query.nom } : {};
    // Si ?nom est fourni, on renvoie uniquement la borne (objet unique pour compatibilité Java) ou null
    if (req.query.nom) {
      const borne = await Borne.findOne(filter);
      if (!borne) return sendError(res, 'Borne introuvable', 404);
      return sendSuccess(res, borne);
    }
    
    const bornes = await Borne.find(filter);
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
    const params = req.body.parametres_attente || req.body;
    const { delai_A, delai_B, delai_X, delai_Y } = params;
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

// POST /api/bornes/:id/casier-alternatif
exports.findCasierAlternatif = async (req, res) => {
  try {
    const { casier_defaillant, taille } = req.body;
    const borne = await Borne.findById(req.params.id);
    if (!borne) return sendError(res, 'Borne introuvable', 404);

    // Marquer le defaillant comme en erreur d'ouverture
    const defaillant = borne.casiers.find(c => c.numero === casier_defaillant);
    if (defaillant) {
      defaillant.etat_materiel = 'ERREUR_OUVERTURE';
      defaillant.etat_occupation = 'VIDE';
    }

    // Trouver un alternatif de taille >= taille demandée
    const ordreTaille = { S: 1, M: 2, L: 3 };
    const niveauRequis = ordreTaille[taille] || 1;

    const alternatif = borne.casiers.find(c => 
      c.etat_occupation === 'VIDE' && 
      c.etat_materiel === 'OK' && 
      ordreTaille[c.taille] >= niveauRequis
    );

    await borne.save();

    if (!alternatif) return sendError(res, 'Aucun casier alternatif disponible', 404);
    
    return sendSuccess(res, { casier_numero: alternatif.numero });
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};

// PATCH /api/bornes/:id/casiers/:numero
exports.updateCasierEtat = async (req, res) => {
  try {
    const { etat_occupation } = req.body;
    const borne = await Borne.findById(req.params.id);
    if (!borne) return sendError(res, 'Borne introuvable', 404);

    const casier = borne.casiers.find(c => c.numero === parseInt(req.params.numero));
    if (!casier) return sendError(res, 'Casier introuvable', 404);

    casier.etat_occupation = etat_occupation;
    await borne.save();

    return sendSuccess(res, borne);
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};
