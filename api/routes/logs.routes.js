const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/auth.middleware');
const { getLogs, createLog } = require('../controllers/logs.controller');

// GET /api/logs — Consultation des logs (accept query params: niveau, borne_id, limit)
router.get('/',   protect, getLogs);

// POST /api/logs — Création d'un log (utilisé par le Serveur Java pour signaler des erreurs µC)
// Pas de protect ici pour que le serveur Java interne puisse écrire sans token
// En production, il faudrait sécuriser avec une clé API interne
router.post('/',  createLog);

module.exports = router;
