import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

const routes = [
  // ── Route publique ───────────────────────────────────────────
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: 'Connexion', public: true }
  },

  // ── Routes protégées (JWT requis) ────────────────────────────
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: 'Dashboard', icon: '📊' }
  },
  {
    path: '/bornes',
    name: 'Bornes',
    component: () => import('@/views/BornesView.vue'),
    meta: { title: 'Bornes', icon: '🏪' }
  },
  {
    path: '/bornes/:id',
    name: 'BorneDetail',
    component: () => import('@/views/BorneDetailView.vue'),
    meta: { title: 'Détail Borne', icon: '🏪' }
  },
  {
    path: '/colis',
    name: 'Colis',
    component: () => import('@/views/ColisView.vue'),
    meta: { title: 'Colis', icon: '📦' }
  },
  {
    path: '/livreurs',
    name: 'Livreurs',
    component: () => import('@/views/LivreursView.vue'),
    meta: { title: 'Livreurs', icon: '🚚' }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('@/views/LogsView.vue'),
    meta: { title: 'Logs', icon: '📋' }
  },

  // ── Catch-all ────────────────────────────────────────────────
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ── Garde de navigation : redirige vers /login si non connecté ──
router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (!to.meta.public && !authStore.estConnecte) {
    return { name: 'Login' }
  }

  // Si déjà connecté et qu'il essaie d'aller sur /login → dashboard
  if (to.name === 'Login' && authStore.estConnecte) {
    return { name: 'Dashboard' }
  }
})

// ── Mise à jour du titre de la page ──────────────────────────────
router.afterEach(to => {
  document.title = `${to.meta.title || 'Admin'} — Bornes`
})

export default router
