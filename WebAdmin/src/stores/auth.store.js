import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'

/**
 * Store d'authentification (Pinia).
 * Gère le token JWT, le rôle et l'état de connexion de l'admin.
 * Le token est persisté en localStorage pour survivre aux rechargements de page.
 */
export const useAuthStore = defineStore('auth', () => {
  // ── État ──────────────────────────────────────────────────────
  const token      = ref(localStorage.getItem('token') || null)
  const role       = ref(localStorage.getItem('role')  || null)
  const identifiant = ref(localStorage.getItem('identifiant') || null)
  const loading    = ref(false)
  const erreur     = ref(null)

  // ── Getters ───────────────────────────────────────────────────
  const estConnecte = computed(() => !!token.value)
  const estAdmin    = computed(() => role.value === 'ADMIN')

  // ── Actions ───────────────────────────────────────────────────

  /**
   * Tente de connecter l'utilisateur.
   * Si succès : stocke le token en mémoire ET en localStorage.
   * Si échec : expose le message d'erreur.
   */
  async function login(id, motDePasse) {
    loading.value = true
    erreur.value  = null
    try {
      const res = await api.login(id, motDePasse)
      const data = res.data?.data || res.data

      token.value       = data.token
      role.value        = data.role
      identifiant.value = data.identifiant

      localStorage.setItem('token',       data.token)
      localStorage.setItem('role',        data.role)
      localStorage.setItem('identifiant', data.identifiant)

      return true
    } catch (e) {
      erreur.value = e.response?.data?.data || 'Identifiant ou mot de passe incorrect.'
      return false
    } finally {
      loading.value = false
    }
  }

  /** Déconnexion : vide l'état et le localStorage. */
  function logout() {
    token.value       = null
    role.value        = null
    identifiant.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('identifiant')
  }

  return { token, role, identifiant, loading, erreur, estConnecte, estAdmin, login, logout }
})
