<template>
  <div class="app-shell">
    <aside class="sidebar">
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
        <div class="api-status">
          <span :class="['status-dot', apiOk ? 'status-dot--ok' : 'status-dot--err']"></span>
          <span class="status-label">{{ apiOk ? 'API connectée' : 'API hors ligne' }}</span>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const apiOk = ref(false)

const navItems = [
  { name: 'dashboard', path: '/',         icon: '📊', label: 'Dashboard'  },
  { name: 'bornes',    path: '/bornes',   icon: '🏪', label: 'Bornes'     },
  { name: 'colis',     path: '/colis',    icon: '📦', label: 'Colis'      },
  { name: 'livreurs',  path: '/livreurs', icon: '🚚', label: 'Livreurs'   },
  { name: 'logs',      path: '/logs',     icon: '📋', label: 'Logs'       },
]

onMounted(async () => {
  try {
    await api.getBornes()
    apiOk.value = true
  } catch {
    apiOk.value = false
  }
})
</script>
