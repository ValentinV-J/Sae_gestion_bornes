const express = require('express');
const router = express.Router();
const { protect, restrictTo } = require('../middleware/auth.middleware');
const {
  getAllLivreurs,
  createLivreur,
  updateLivreur,
  deleteLivreur,
  getLivreurByRfid
} = require('../controllers/livreurs.controller');

// GET /api/livreurs/rfid/:rfid — Pour le serveur Java
router.get('/rfid/:rfid', getLivreurByRfid);

// GET — Lecture accessible à tous les admins connectés
router.get('/',     protect, getAllLivreurs);

// POST/PUT/DELETE — Réservés à l'ADMIN uniquement
router.post('/',         protect, restrictTo('ADMIN'), createLivreur);
router.put('/:id',       protect, restrictTo('ADMIN'), updateLivreur);
router.delete('/:id',    protect, restrictTo('ADMIN'), deleteLivreur);

module.exports = router;
