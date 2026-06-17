const express = require('express');
const router = express.Router();
const devController = require('../controllers/dev.controller');

// Route pour réinitialiser la BDD avec des fausses données
// Note : Idéalement, cette route devrait être protégée, 
// mais pour le projet on la laisse accessible pour faciliter les tests.
router.post('/reset-db', devController.resetDatabase);

module.exports = router;
