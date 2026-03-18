const express = require('express');
const router = express.Router();
const { protect, restrictTo } = require('../middleware/auth.middleware');
const {
  getAllBornes,
  getBorneById,
  createBorne,
  updateBorne,
  updateSettings,
} = require('../controllers/bornes.controller');

// Toutes les routes bornes sont protégées (token JWT requis)
router.get('/',           protect, getAllBornes);
router.get('/:id',        protect, getBorneById);
router.post('/',          protect, restrictTo('ADMIN'), createBorne);
router.put('/:id',        protect, restrictTo('ADMIN'), updateBorne);

// Route dédiée pour mettre à jour les paramètres µC (A, B, X, Y) d'une borne
// Accessible à l'ADMIN et au TECHNICIEN
router.put('/:id/settings', protect, updateSettings);

module.exports = router;
