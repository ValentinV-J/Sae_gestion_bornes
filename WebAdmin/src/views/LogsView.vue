<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">📋 Logs système</div>
        <div class="page-subtitle">{{ logs.length }} entrée(s) affichée(s)</div>
      </div>
      <button class="btn btn--outline btn--sm" @click="load">🔄 Actualiser</button>
    </div>

    <!-- Filtres -->
    <div class="filters">
      <select class="select" v-model="filterNiveau">
        <option value="">Tous niveaux</option>
        <option value="INFO">INFO</option>
        <option value="WARNING">WARNING</option>
        <option value="CRITICAL">CRITICAL</option>
      </select>
      <select class="select" v-model="filterAction">
        <option value="">Toutes actions</option>
        <option value="OUVERTURE_CASIER">OUVERTURE_CASIER</option>
        <option value="ECHEC_FERMETURE">ECHEC_FERMETURE</option>
        <option value="SAISIE_CODE_ERRONE">SAISIE_CODE_ERRONE</option>
        <option value="BUZZER_TIMEOUT">BUZZER_TIMEOUT</option>
        <option value="MISE_A_JOUR_PARAMETRES">MISE_A_JOUR_PARAMETRES</option>
      </select>
      <input class="input" v-model="search" placeholder="🔍 Filtre détails / borne…" />
      <button class="btn btn--outline btn--sm" @click="resetFilters">✕ Réinitialiser</button>
    </div>

    <div v-if="loading" class="state-center"><div class="spinner"></div></div>
    <div v-else-if="error" class="state-center error-msg">{{ error }}</div>

    <div v-else class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Niveau</th>
              <th>Action</th>
              <th>Casier</th>
              <th>Détails</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filtered.length === 0">
              <td colspan="5" style="text-align:center;color:var(--text-muted);padding:32px">
                Aucun log correspondant
              </td>
            </tr>
            <tr v-for="log in filtered" :key="log._id">
              <td class="text-muted" style="white-space:nowrap">{{ formatDate(log.horodatage) }}</td>
              <td>
                <span :class="`badge badge--${log.niveau?.toLowerCase()}`">{{ log.niveau }}</span>
              </td>
              <td style="font-family:monospace;font-size:12px">{{ log.action }}</td>
              <td class="text-muted">{{ log.casier_numero ?? '—' }}</td>
              <td class="text-muted" style="max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
                {{ log.details || '—' }}
              </td>
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

const logs        = ref([])
const loading     = ref(true)
const error       = ref(null)
const filterNiveau  = ref('')
const filterAction  = ref('')
const search        = ref('')

const filtered = computed(() => logs.value.filter(l => {
  const matchN = !filterNiveau.value || l.niveau === filterNiveau.value
  const matchA = !filterAction.value || l.action === filterAction.value
  const q = search.value.toLowerCase()
  const matchS = !q || l.details?.toLowerCase().includes(q) || l.action?.toLowerCase().includes(q)
  return matchN && matchA && matchS
}))

const formatDate = iso => iso ? new Date(iso).toLocaleString('fr-FR', {
  day:'2-digit', month:'2-digit', year:'2-digit', hour:'2-digit', minute:'2-digit', second:'2-digit'
}) : '—'

function resetFilters() {
  filterNiveau.value = ''
  filterAction.value = ''
  search.value = ''
}

async function load() {
  loading.value = true; error.value = null
  try {
    const r = await api.getLogs({ sort: '-horodatage', limit: 200 })
    logs.value = r.data?.data || r.data || []
  } catch (e) { error.value = 'Erreur chargement logs : ' + e.message }
  finally { loading.value = false }
}

onMounted(load)
</script>
