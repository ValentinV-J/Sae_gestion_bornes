<template>
  <div class="app-shell">
    <aside class="sidebar" v-if="route.path !== '/login'">
      <div class="sidebar-brand">
        <span class="brand-icon">📦</span>
        <div>
          <div class="brand-title">Bornes Admin</div>
          <div class="brand-sub">Tableau de bord</div>
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
        <div class="api-status" style="margin-bottom: 12px;">
          <span :class="['status-dot', apiOk ? 'status-dot--ok' : 'status-dot--err']"></span>
          <span class="status-label">{{ apiOk ? 'API connectée' : 'API hors ligne' }}</span>
        </div>
        <button class="btn btn--outline btn--sm" style="width: 100%; margin-bottom: 12px; border-color: var(--danger); color: var(--danger);" @click="resetDB">🔄 Réinitialiser BDD</button>
        <button class="btn btn--outline btn--sm" style="width: 100%;" @click="logout">Se déconnecter</button>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/services/api'

const route = useRoute()
const router = useRouter()
const apiOk = ref(false)

const navItems = [
  { name: 'dashboard', path: '/',         icon: '📊', label: 'Dashboard'  },
  { name: 'bornes',    path: '/bornes',   icon: '🏪', label: 'Bornes'     },
  { name: 'colis',     path: '/colis',    icon: '📦', label: 'Colis'      },
  { name: 'livreurs',  path: '/livreurs', icon: '🚚', label: 'Livreurs'   },
  { name: 'logs',      path: '/logs',     icon: '📋', label: 'Logs'       },
]

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}

async function resetDB() {
  if (confirm("⚠️ Attention, cela va vider la base de données et générer des fausses données. Le colis de test sera préservé. Confirmer ?")) {
    try {
      await api.apiClient.post('/dev/reset-db');
      alert("✅ Base de données réinitialisée avec succès !");
      window.location.reload();
    } catch (err) {
      alert("❌ Erreur lors de la réinitialisation : " + err.message);
    }
  }
}

onMounted(async () => {
  try {
    await api.getBornes()
    apiOk.value = true
  } catch {
    apiOk.value = false
  }
})
</script>
