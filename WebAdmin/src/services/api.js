import axios from 'axios'

/**
 * Instance Axios centralisée.
 * En dev, Vite proxifie /api → http://localhost:3000 (voir vite.config.js).
 * En prod, adapter la baseURL selon le déploiement.
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// ── Intercepteur requête : injecte le token JWT si présent ──────
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Intercepteur réponse : gestion des erreurs globales ─────────
http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      // Token expiré ou invalide → déconnexion automatique
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('identifiant')
      window.location.href = '/login'
    }
    console.error('[API Error]', err.response?.status, err.message)
    return Promise.reject(err)
  }
)

// ── Routes de l'API ─────────────────────────────────────────────
export default {
  // Auth
  login: (identifiant, mot_de_passe) => http.post('/auth/login', { identifiant, mot_de_passe }),

  // Bornes
  getBornes:         ()         => http.get('/bornes'),
  getBorne:          id         => http.get(`/bornes/${id}`),
  createBorne:       data       => http.post('/bornes', data),
  updateBorne:       (id, data) => http.put(`/bornes/${id}`, data),
  updateParametres:  (id, data) => http.put(`/bornes/${id}/settings`, data),

  // Colis
  getColis:          (params)   => http.get('/colis', { params }),
  getColisByUuid:    uuid       => http.get(`/colis/${uuid}`),

  // Livreurs
  getLivreurs:       ()         => http.get('/livreurs'),
  getLivreur:        id         => http.get(`/livreurs/${id}`),
  createLivreur:     data       => http.post('/livreurs', data),
  updateLivreur:     (id, data) => http.put(`/livreurs/${id}`, data),
  deleteLivreur:     id         => http.delete(`/livreurs/${id}`),

  // Logs
  getLogs:           (params)   => http.get('/logs', { params }),
}
