const jwt = require('jsonwebtoken');

/**
 * Middleware de protection des routes.
 * Vérifie que la requête contient un token JWT valide dans le header Authorization.
 * Usage: router.get('/route-protegee', protect, controller)
 */
const protect = (req, res, next) => {
  const authHeader = req.headers.authorization;

  // Le header doit être de la forme : "Bearer <token>"
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Accès refusé : aucun token fourni.' });
  }

  const token = authHeader.split(' ')[1];

  try {
    // Vérifie la signature et la date d'expiration du token
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.utilisateur = decoded; // Injecte les infos du token dans la requête (id, role)
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Token invalide ou expiré.' });
  }
};

/**
 * Middleware de restriction par rôle.
 * À utiliser après 'protect'.
 * Usage: router.delete('/route', protect, restrictTo('ADMIN'), controller)
 */
const restrictTo = (...roles) => {
  return (req, res, next) => {
    if (!roles.includes(req.utilisateur.role)) {
      return res.status(403).json({ error: 'Accès interdit : droits insuffisants.' });
    }
    next();
  };
};

module.exports = { protect, restrictTo };
