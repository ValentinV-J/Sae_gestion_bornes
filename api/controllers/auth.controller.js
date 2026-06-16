const jwt = require('jsonwebtoken');
const Utilisateur = require('../models/Utilisateur');
const { sendSuccess, sendError } = require('../utils/response');

// POST /api/auth/login
exports.login = async (req, res) => {
  try {
    const { identifiant, mot_de_passe } = req.body;

    if (!identifiant || !mot_de_passe) {
      return sendError(res, 'Identifiant et mot de passe requis.', 400);
    }

    const utilisateur = await Utilisateur.findOne({ identifiant }).select('+mot_de_passe');
    if (!utilisateur) {
      return sendError(res, 'Identifiant ou mot de passe incorrect.', 401);
    }

    const motDePasseValide = await utilisateur.verifierMotDePasse(mot_de_passe);
    if (!motDePasseValide) {
      return sendError(res, 'Identifiant ou mot de passe incorrect.', 401);
    }

    utilisateur.derniere_connexion = new Date();
    await utilisateur.save();

    const token = jwt.sign(
      { id: utilisateur._id, role: utilisateur.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRE || '8h' }
    );

    return sendSuccess(res, {
      token,
      role: utilisateur.role,
      identifiant: utilisateur.identifiant,
    });
  } catch (err) {
    return sendError(res, `Erreur serveur : ${err.message}`, 500);
  }
};
