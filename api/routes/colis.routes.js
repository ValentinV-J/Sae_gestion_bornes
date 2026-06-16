const express = require('express');
const router = express.Router();
const colisController = require('../controllers/colis.controller');

// L'ordre des routes est important pour éviter que /depot matche /:uuid

// Routes Serveur Java
router.get('/depot', colisController.getCasierPourDepot);
router.get('/retrait', colisController.getCasierPourRetrait);

// Routes Mobile / Java avec paramètre
router.get('/:uuid', colisController.getColisByUuid);
router.patch('/:uuid/scan', colisController.scanColis);
router.patch('/:uuid/depose', colisController.marquerColisDepose);
router.patch('/:uuid/retire', colisController.marquerColisRetire);

module.exports = router;
