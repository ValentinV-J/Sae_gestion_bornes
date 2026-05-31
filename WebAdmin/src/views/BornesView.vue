<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">🏪 Bornes</div>
        <div class="page-subtitle">{{ bornes.length }} borne(s) enregistrée(s)</div>
      </div>
      <button class="btn btn--outline btn--sm" @click="load">🔄 Actualiser</button>
    </div>

    <div v-if="loading" class="state-center"><div class="spinner"></div></div>
    <div v-else-if="error" class="state-center error-msg">{{ error }}</div>

    <div v-else class="bornes-grid">
      <div
        v-for="b in bornes"
        :key="b._id"
        class="card borne-card"
        @click="$router.push(`/bornes/${b._id}`)"
      >
        <div class="flex-between mb-4">
          <strong>{{ b.nom }}</strong>
          <span :class="['badge', casierLibres(b) > 0 ? 'badge--ok' : 'badge--retire']">
            {{ casierLibres(b) === 0 ? 'Pleine' : `${casierLibres(b)} libre(s)` }}
          </span>
        </div>
        <div class="text-muted" style="margin-bottom:16px">📍 {{ b.adresse }}</div>

        <!-- Mini casier grid -->
        <div class="casier-grid">
          <div
            v-for="c in b.casiers"
            :key="c.numero"
            :class="['casier-card',
              c.etat_materiel !== 'OK' ? 'casier-card--err'
              : c.etat_occupation === 'OCCUPE' ? 'casier-card--occupe'
              : 'casier-card--vide']"
          >
            <div class="casier-num">#{{ c.numero }}</div>
            <div class="casier-taille">{{ c.taille }}</div>
            <div
              :class="['casier-etat',
                c.etat_materiel !== 'OK' ? 'text-danger'
                : c.etat_occupation === 'OCCUPE' ? 'text-warning' : 'text-success']"
              style="color: inherit"
            >
              {{ c.etat_materiel !== 'OK' ? '⚠ ' + c.etat_materiel : c.etat_occupation }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const bornes  = ref([])
const loading = ref(true)
const error   = ref(null)

const casierLibres = b =>
  (b.casiers || []).filter(c => c.etat_occupation === 'VIDE' && c.etat_materiel === 'OK').length

async function load() {
  loading.value = true; error.value = null
  try {
    const r = await api.getBornes()
    bornes.value = r.data?.data || r.data || []
  } catch (e) {
    error.value = 'Erreur chargement bornes : ' + e.message
  } finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.bornes-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(310px, 1fr)); gap: 20px; }
.borne-card  { cursor: pointer; transition: border-color .18s; }
.borne-card:hover { border-color: var(--accent); }
</style>
