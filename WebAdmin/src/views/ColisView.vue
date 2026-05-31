<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">📦 Colis</div>
        <div class="page-subtitle">{{ filtered.length }} colis affichés</div>
      </div>
      <button class="btn btn--outline btn--sm" @click="load">🔄 Actualiser</button>
    </div>

    <!-- Filtres -->
    <div class="filters">
      <select class="select" v-model="filterStatut">
        <option value="">Tous les statuts</option>
        <option value="ATTENTE_DEPOT">En attente dépôt</option>
        <option value="SCAN_MOBILE_OK">Scan mobile OK</option>
        <option value="DEPOSE">Déposé</option>
        <option value="RETIRE">Retiré</option>
      </select>
      <input class="input" v-model="search" placeholder="🔍 Rechercher UUID / code..." />
    </div>

    <div v-if="loading" class="state-center"><div class="spinner"></div></div>
    <div v-else-if="error" class="state-center error-msg">{{ error }}</div>

    <div v-else class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>UUID</th>
              <th>Statut</th>
              <th>Taille</th>
              <th>Casier</th>
              <th>Code retrait</th>
              <th>Date dépôt</th>
              <th>Date retrait</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filtered.length === 0">
              <td colspan="7" style="text-align:center;color:var(--text-muted);padding:32px">
                Aucun colis trouvé
              </td>
            </tr>
            <tr v-for="c in filtered" :key="c._id">
              <td class="text-mono">{{ c._id }}</td>
              <td><span :class="statutBadge(c.statut)">{{ statutLabel(c.statut) }}</span></td>
              <td>{{ c.taille_colis || '—' }}</td>
              <td>{{ c.casier_numero ?? '—' }}</td>
              <td class="text-mono" style="letter-spacing:.15em">{{ c.code_retrait || '—' }}</td>
              <td class="text-muted">{{ formatDate(c.date_depot) }}</td>
              <td class="text-muted">{{ formatDate(c.date_retrait) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/services/api'

const colis      = ref([])
const loading    = ref(true)
const error      = ref(null)
const filterStatut = ref('')
const search       = ref('')

const filtered = computed(() => colis.value.filter(c => {
  const matchStatut = !filterStatut.value || c.statut === filterStatut.value
  const q = search.value.toLowerCase()
  const matchSearch = !q || c._id?.toLowerCase().includes(q) || c.code_retrait?.includes(q)
  return matchStatut && matchSearch
}))

const statutLabel = s => ({
  ATTENTE_DEPOT:  'Attente dépôt',
  SCAN_MOBILE_OK: 'Scan OK',
  DEPOSE:         'Déposé',
  RETIRE:         'Retiré'
}[s] || s)

const statutBadge = s => ({
  ATTENTE_DEPOT:  'badge badge--attente',
  SCAN_MOBILE_OK: 'badge badge--scan',
  DEPOSE:         'badge badge--depose',
  RETIRE:         'badge badge--retire'
}[s] || 'badge')

const formatDate = iso => iso ? new Date(iso).toLocaleString('fr-FR', {
  day:'2-digit', month:'2-digit', year:'2-digit', hour:'2-digit', minute:'2-digit'
}) : '—'

async function load() {
  loading.value = true; error.value = null
  try {
    const r = await api.getColis({})
    colis.value = r.data?.data || r.data || []
  } catch (e) {
    error.value = 'Erreur chargement colis : ' + e.message
  } finally { loading.value = false }
}

onMounted(load)
</script>
