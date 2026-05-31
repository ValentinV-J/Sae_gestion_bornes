<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">🏪 {{ borne?.nom || 'Borne' }}</div>
        <div class="page-subtitle">📍 {{ borne?.adresse }}</div>
      </div>
      <div style="display:flex;gap:10px">
        <button class="btn btn--outline btn--sm" @click="$router.push('/bornes')">← Retour</button>
        <button class="btn btn--outline btn--sm" @click="load">🔄 Actualiser</button>
      </div>
    </div>

    <div v-if="loading" class="state-center"><div class="spinner"></div></div>
    <div v-else-if="error" class="state-center error-msg">{{ error }}</div>

    <div v-else>
      <!-- Paramètres attente -->
      <div class="card" style="margin-bottom:20px">
        <div class="flex-between mb-4">
          <div class="card-title" style="margin:0">Paramètres de temporisation</div>
          <button class="btn btn--primary btn--sm" @click="showParamModal = true">✏ Modifier</button>
        </div>
        <div style="display:grid; grid-template-columns: repeat(4,1fr); gap:16px">
          <div v-for="[key, label] in paramLabels" :key="key">
            <div class="form-label">{{ label }}</div>
            <div style="font-size:22px;font-weight:700;color:var(--accent)">
              {{ borne.parametres_attente?.[key] ?? '—' }}<span style="font-size:13px;color:var(--text-muted)"> s</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Casiers -->
      <div class="card">
        <div class="card-title">Casiers ({{ borne.casiers?.length }})</div>
        <div class="casier-grid">
          <div
            v-for="c in borne.casiers"
            :key="c.numero"
            :class="['casier-card',
              c.etat_materiel !== 'OK' ? 'casier-card--err'
              : c.etat_occupation === 'OCCUPE' ? 'casier-card--occupe'
              : 'casier-card--vide']"
          >
            <div class="casier-num">#{{ c.numero }}</div>
            <div class="casier-taille">Taille {{ c.taille }}</div>
            <span :class="`badge badge--${c.etat_occupation.toLowerCase()}`">{{ c.etat_occupation }}</span>
            <div v-if="c.etat_materiel !== 'OK'" style="margin-top:4px">
              <span class="badge badge--error" style="font-size:10px">{{ c.etat_materiel }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Paramètres -->
    <div v-if="showParamModal" class="modal-overlay" @click.self="showParamModal = false">
      <div class="modal">
        <div class="modal-title">Modifier les paramètres</div>
        <div v-for="[key, label] in paramLabels" :key="key" class="form-group">
          <label class="form-label">{{ label }} (secondes)</label>
          <input class="input" type="number" min="0" v-model.number="paramEdit[key]" />
        </div>
        <div class="modal-actions">
          <button class="btn btn--outline" @click="showParamModal = false">Annuler</button>
          <button class="btn btn--primary" :disabled="saving" @click="saveParams">
            {{ saving ? 'Enregistrement…' : 'Sauvegarder' }}
          </button>
        </div>
        <div v-if="saveError" class="error-msg mt-4">{{ saveError }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute }                 from 'vue-router'
import api                          from '@/services/api'

const route = useRoute()
const borne = ref(null)
const loading = ref(true)
const error   = ref(null)
const showParamModal = ref(false)
const saving    = ref(false)
const saveError = ref(null)

const paramLabels = [
  ['delai_A', 'Délai A (déclench. buzzer livreur)'],
  ['delai_B', 'Délai B (fermeture livreur)'],
  ['delai_X', 'Délai X (déclench. buzzer client)'],
  ['delai_Y', 'Délai Y (fermeture client)'],
]

const paramEdit = reactive({ delai_A: 0, delai_B: 0, delai_X: 0, delai_Y: 0 })

async function load() {
  loading.value = true; error.value = null
  try {
    const r = await api.getBorne(route.params.id)
    borne.value = r.data?.data || r.data
    Object.assign(paramEdit, borne.value?.parametres_attente || {})
  } catch (e) {
    error.value = 'Erreur : ' + e.message
  } finally { loading.value = false }
}

async function saveParams() {
  saving.value = true; saveError.value = null
  try {
    await api.updateParametres(route.params.id, { parametres_attente: { ...paramEdit } })
    borne.value.parametres_attente = { ...paramEdit }
    showParamModal.value = false
  } catch (e) {
    saveError.value = 'Erreur sauvegarde : ' + e.message
  } finally { saving.value = false }
}

onMounted(load)
</script>
