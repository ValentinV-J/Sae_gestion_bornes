# WebAdmin — Site d'administration des bornes

Dashboard Vue.js 3 pour administrer les bornes de dépôt-retrait de colis.  
Communique avec l'API Node.js via Axios.

## Structure

```
WebAdmin/
├── index.html
├── vite.config.js        ← proxy /api → localhost:3000
├── package.json
└── src/
    ├── main.js           ← bootstrap Vue + Pinia + Router
    ├── App.vue           ← shell (sidebar + RouterView)
    ├── router/index.js   ← 6 routes lazy-loaded
    ├── services/api.js   ← instance Axios centralisée
    ├── style/main.css    ← design system complet
    └── views/
        ├── DashboardView.vue    ← KPIs + résumé bornes + logs récents
        ├── BornesView.vue       ← grille toutes les bornes + casiers
        ├── BorneDetailView.vue  ← détail borne + modifier paramètres
        ├── ColisView.vue        ← liste filtrée par statut / UUID
        ├── LivreursView.vue     ← CRUD livreurs (ajout/suppression)
        └── LogsView.vue         ← historique filtrable (niveau, action)
```

## Installation et démarrage

```bash
cd WebAdmin
npm install
npm run dev        # → http://localhost:8080
```

> **Prérequis** : API Node.js lancée sur le port 3000.  
> Vite proxifie automatiquement `/api` → `http://localhost:3000`.

## Pages

| Page | Route | Données |
|---|---|---|
| Dashboard | `/` | Bornes, Colis (tous), Livreurs, Logs |
| Bornes | `/bornes` | GET /api/bornes |
| Détail borne | `/bornes/:id` | GET /api/bornes/:id + PATCH paramètres |
| Colis | `/colis` | GET /api/colis (filtre statut) |
| Livreurs | `/livreurs` | GET/POST/DELETE /api/livreurs |
| Logs | `/logs` | GET /api/logs (filtre niveau/action) |
