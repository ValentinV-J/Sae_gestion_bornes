<template>
  <!-- Si non connecté : juste la vue router (page login) -->
  <RouterView v-if="!authStore.estConnecte" />

  <!-- Si connecté : layout complet avec sidebar -->
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-icon">📦</span>
        <div>
          <div class="brand-title">Bornes Admin</div>
          <div class="brand-sub">{{ authStore.identifiant }}</div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="item.path"
          class="nav-item"
          active-class="nav-item--active"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="api-status">
          <span :class="['status-dot', apiOk ? 'status-dot--ok' : 'status-dot--err']"></span>
          <span class="status-label">{{ apiOk ? 'API connectée' : 'API hors ligne' }}</span>
        </div>
        <button class="btn btn--outline btn--sm logout-btn" @click="deconnecter">
          🚪 Déconnexion
        </button>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import api from '@/services/api'

const router    = useRouter()
const authStore = useAuthStore()
const apiOk     = ref(false)

const navItems = [
  { name: 'dashboard', path: '/',         icon: '📊', label: 'Dashboard'  },
  { name: 'bornes',    path: '/bornes',   icon: '🏪', label: 'Bornes'     },
  { name: 'colis',     path: '/colis',    icon: '📦', label: 'Colis'      },
  { name: 'livreurs',  path: '/livreurs', icon: '🚚', label: 'Livreurs'   },
  { name: 'logs',      path: '/logs',     icon: '📋', label: 'Logs'       },
]

function deconnecter() {
  authStore.logout()
  router.push('/login')
}

onMounted(async () => {
  if (!authStore.estConnecte) return
  try {
    await api.getBornes()
    apiOk.value = true
  } catch {
    apiOk.value = false
  }
})
</script>

<style scoped>
.logout-btn {
  width: 100%;
  margin-top: 10px;
  text-align: center;
}
</style>
