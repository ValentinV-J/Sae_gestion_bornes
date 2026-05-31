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

// Intercepteur : log des erreurs réseau en console
api.interceptors.response.use(
  res => res,
  err => {
    console.error('[API Error]', err.response?.status, err.message)
    return Promise.reject(err)
  }
)

export default {
  // ── Bornes ──────────────────────────────────────────────────
  getBornes: ()              => api.get('/bornes'),
  getBorne:  id              => api.get(`/bornes/${id}`),
  updateParametres: (id, p)  => api.patch(`/bornes/${id}/parametres`, p),

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
  getStats: ()               => api.get('/stats')
}
