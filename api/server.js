require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');

const app = express();

// --- Middlewares globaux ---
app.use(express.json()); // Parse le body des requêtes en JSON

// --- Connexion MongoDB ---
mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log('✅ Connecté à MongoDB'))
  .catch((err) => {
    console.error('❌ Erreur de connexion MongoDB :', err.message);
    process.exit(1);
  });

// --- Routes ---
app.use('/api/auth',      require('./routes/auth.routes'));
app.use('/api/bornes',    require('./routes/bornes.routes'));
app.use('/api/colis',     require('./routes/colis.routes'));
app.use('/api/livreurs',  require('./routes/livreurs.routes'));
app.use('/api/logs',      require('./routes/logs.routes'));

// --- Route par défaut (test rapide que l'API tourne) ---
app.get('/', (req, res) => {
  res.json({ message: 'API SAÉ Gestion de Bornes — Opérationnelle ✅' });
});

// --- Gestion des routes inconnues ---
app.use((req, res) => {
  res.status(404).json({ error: 'Route introuvable' });
});

// --- Démarrage du serveur ---
const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || '0.0.0.0';
app.listen(PORT, HOST, () => {
  console.log(`Serveur demarre sur http://${HOST}:${PORT}`);
});
