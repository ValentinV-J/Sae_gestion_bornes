<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">📊 Dashboard</div>
        <div class="page-subtitle">Vue d'ensemble en temps réel</div>
      </div>
      <button class="btn btn--outline btn--sm" @click="loadAll">🔄 Actualiser</button>
    </div>

    <!-- KPIs -->
    <div class="kpi-grid">
      <div class="card">
        <div class="card-title">Bornes actives</div>
        <div class="card-value card-value--accent">{{ stats.bornes }}</div>
      </div>
      <div class="card">
        <div class="card-title">Colis en attente</div>
        <div class="card-value card-value--warning">{{ stats.attente }}</div>
      </div>
      <div class="card">
        <div class="card-title">Colis déposés</div>
        <div class="card-value">{{ stats.deposes }}</div>
      </div>
      <div class="card">
        <div class="card-title">Colis retirés (total)</div>
        <div class="card-value card-value--success">{{ stats.retires }}</div>
      </div>
      <div class="card">
        <div class="card-title">Livreurs enregistrés</div>
        <div class="card-value">{{ stats.livreurs }}</div>
      </div>
      <div class="card">
        <div class="card-title">Alertes (24h)</div>
        <div class="card-value card-value--error">{{ stats.alertes }}</div>
      </div>
    </div>

    <div v-if="loading" class="state-center"><div class="spinner"></div></div>
    <div v-else-if="error" class="state-center error-msg">{{ error }}</div>

    <div v-else class="grid-2">
      <!-- Bornes résumé -->
      <div class="card">
        <div class="flex-between mb-4">
          <div class="card-title" style="margin:0">État des bornes</div>
          <RouterLink to="/bornes" class="btn btn--outline btn--sm">Voir tout</RouterLink>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Borne</th><th>Casiers libres</th><th>Adresse</th></tr>
            </thead>
            <tbody>
              <tr v-for="b in bornes" :key="b._id" @click="$router.push(`/bornes/${b._id}`)">
                <td><strong>{{ b.nom }}</strong></td>
                <td>
                  <span :class="casierLibres(b) === 0 ? 'badge badge--retire' : 'badge badge--ok'">
                    {{ casierLibres(b) }} / {{ b.casiers?.length || 0 }}
                  </span>
                </td>
                <td class="text-muted">{{ b.adresse }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Logs récents -->
      <div class="card">
        <div class="flex-between mb-4">
          <div class="card-title" style="margin:0">Logs récents</div>
          <RouterLink to="/logs" class="btn btn--outline btn--sm">Voir tout</RouterLink>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Niveau</th><th>Action</th><th>Date</th></tr>
            </thead>
            <tbody>
              <tr v-for="log in recentLogs" :key="log._id">
                <td><span :class="`badge badge--${log.niveau.toLowerCase()}`">{{ log.niveau }}</span></td>
                <td>{{ log.action }}</td>
                <td class="text-muted">{{ formatDate(log.horodatage) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const loading = ref(true)
const error   = ref(null)
const bornes  = ref([])
const recentLogs = ref([])
const stats   = ref({ bornes:0, attente:0, deposes:0, retires:0, livreurs:0, alertes:0 })

const casierLibres = b =>
  (b.casiers || []).filter(c => c.etat_occupation === 'VIDE' && c.etat_materiel === 'OK').length

const formatDate = iso => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' })
}

async function loadAll() {
  loading.value = true
  error.value = null
  try {
    const [rBornes, rColis, rLivreurs, rLogs] = await Promise.all([
      api.getBornes(),
      api.getColis({}),
      api.getLivreurs(),
      api.getLogs({ limit: 8, sort: '-horodatage' })
    ])
    bornes.value     = rBornes.data?.data  || rBornes.data  || []
    const allColis   = rColis.data?.data   || rColis.data   || []
    recentLogs.value = rLogs.data?.data    || rLogs.data    || []
    const allLivr    = rLivreurs.data?.data || rLivreurs.data || []

    stats.value = {
      bornes:   bornes.value.length,
      attente:  allColis.filter(c => c.statut === 'ATTENTE_DEPOT' || c.statut === 'SCAN_MOBILE_OK').length,
      deposes:  allColis.filter(c => c.statut === 'DEPOSE').length,
      retires:  allColis.filter(c => c.statut === 'RETIRE').length,
      livreurs: allLivr.length,
      alertes:  (rLogs.data?.data || []).filter(l => l.niveau === 'CRITICAL').length
    }
  } catch (e) {
    error.value = 'Impossible de joindre l\'API : ' + (e.message || 'erreur réseau')
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>
