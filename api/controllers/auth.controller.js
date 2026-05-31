const jwt = require('jsonwebtoken');
const Utilisateur = require('../models/Utilisateur');

// POST /api/auth/login
// Vérifie les identifiants et retourne un token JWT si valide.
exports.login = async (req, res) => {
  try {
    const { identifiant, mot_de_passe } = req.body;

    if (!identifiant || !mot_de_passe) {
      return res.status(400).json({ error: 'Identifiant et mot de passe requis.' });
    }

    // On sélectionne explicitement mot_de_passe car il est masqué par défaut (select: false)
    const utilisateur = await Utilisateur.findOne({ identifiant }).select('+mot_de_passe');

    if (!utilisateur) {
      return res.status(401).json({ error: 'Identifiant ou mot de passe incorrect.' });
    }

    const motDePasseValide = await utilisateur.verifierMotDePasse(mot_de_passe);
    if (!motDePasseValide) {
      return res.status(401).json({ error: 'Identifiant ou mot de passe incorrect.' });
    }

    // Mise à jour de la dernière connexion
    utilisateur.derniere_connexion = new Date();
    await utilisateur.save();

    // Génération du token JWT
    const token = jwt.sign(
      { id: utilisateur._id, role: utilisateur.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRE || '8h' }
    );

    res.status(200).json({
      token,
      role: utilisateur.role,
      identifiant: utilisateur.identifiant,
    });
  } catch (err) {
    res.status(500).json({ error: 'Erreur serveur.', details: err.message });
  }
};
