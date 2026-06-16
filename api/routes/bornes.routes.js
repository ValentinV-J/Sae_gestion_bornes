const express = require('express');
const router = express.Router();
const { protect, restrictTo } = require('../middleware/auth.middleware');
const {
  getAllBornes,
  getBorneById,
  createBorne,
  updateBorne,
  updateSettings,
  findCasierAlternatif,
  updateCasierEtat
} = require('../controllers/bornes.controller');

// GET /api/bornes est NON PROTÉGÉ (utilisé par le Serveur Java interne)
router.get('/',           getAllBornes);
router.get('/:id',        protect, getBorneById);
router.post('/',          protect, restrictTo('ADMIN'), createBorne);
router.put('/:id',        protect, restrictTo('ADMIN'), updateBorne);

// Routes internes Serveur Java
router.post('/:id/casier-alternatif', findCasierAlternatif);
router.patch('/:id/casiers/:numero', updateCasierEtat);

// Route dédiée pour mettre à jour les paramètres µC (A, B, X, Y) d'une borne
router.put('/:id/settings', protect, updateSettings);

module.exports = router;
