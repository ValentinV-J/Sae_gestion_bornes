import axios from 'axios'

/**
 * Instance Axios centralisée.
 * En dev, Vite proxifie /api → http://localhost:3000
 * En prod, adapter la baseURL selon le déploiement.
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// Intercepteur : ajout du token JWT et log des erreurs
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    console.error('[API Error]', err.response?.status, err.message)
    // Si 401, on pourrait rediriger vers /login ici, mais le router s'en charge.
    return Promise.reject(err)
  }
)

export default {
  // ── Authentification ─────────────────────────────────────────
  login: (identifiant, mot_de_passe) => api.post('/auth/login', { identifiant, mot_de_passe }),

  // ── Bornes ──────────────────────────────────────────────────
  getBornes: ()              => api.get('/bornes'),
  getBorne:  id              => api.get(`/bornes/${id}`),
  updateParametres: (id, p)  => api.put(`/bornes/${id}/settings`, p),

  // ── Colis ───────────────────────────────────────────────────
  getColis:      (params)    => api.get('/colis', { params }),
  getColisByUuid: uuid       => api.get(`/colis/${uuid}`),

  // ── Livreurs ────────────────────────────────────────────────
  getLivreurs:   ()          => api.get('/livreurs'),
  getLivreur:    id          => api.get(`/livreurs/${id}`),
  createLivreur: data        => api.post('/livreurs', data),
  deleteLivreur: id          => api.delete(`/livreurs/${id}`),

  // ── Logs ────────────────────────────────────────────────────
  getLogs: (params)          => api.get('/logs', { params }),

  // ── Statistiques (agrégats côté API) ────────────────────────
  getStats: ()               => api.get('/stats'),

  // ── Développement ───────────────────────────────────────────
  resetDB: ()                => api.post('/dev/reset-db')
}
