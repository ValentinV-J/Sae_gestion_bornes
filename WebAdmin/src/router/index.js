import { createRouter, createWebHistory } from 'vue-router'

const routes = [
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
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: 'Connexion', requiresAuth: false }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('@/views/LogsView.vue'),
    meta: { title: 'Logs', icon: '📋' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Protection des routes
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

// Mise à jour du titre de la page
router.afterEach(to => {
  document.title = `${to.meta.title || 'Admin'} — Bornes`
})

export default router
