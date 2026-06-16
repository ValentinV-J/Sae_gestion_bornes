<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-title">🚚 Livreurs</div>
        <div class="page-subtitle">{{ livreurs.length }} livreur(s) autorisé(s)</div>
      </div>
      <button class="btn btn--primary btn--sm" @click="openAdd">+ Ajouter</button>
    </div>

    <div v-if="loading" class="state-center"><div class="spinner"></div></div>
    <div v-else-if="error" class="state-center error-msg">{{ error }}</div>

    <div v-else class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Nom & Prénom</th><th>Société</th><th>ID Badge RFID</th><th>Actions</th></tr>
          </thead>
          <tbody>
            <tr v-if="livreurs.length === 0">
              <td colspan="4" style="text-align:center;color:var(--text-muted);padding:32px">
                Aucun livreur enregistré
              </td>
            </tr>
            <tr v-for="l in livreurs" :key="l._id">
              <td><strong>{{ l.nom }} {{ l.prenom }}</strong></td>
              <td>{{ l.societe }}</td>
              <td class="text-mono">{{ l.id_badge_rfid }}</td>
              <td>
                <button class="btn btn--danger btn--sm" @click="confirmDelete(l)">🗑 Supprimer</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal ajout -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
      <div class="modal">
        <div class="modal-title">Ajouter un livreur</div>
        <div class="form-group">
          <label class="form-label">Nom *</label>
          <input class="input" v-model="newLivreur.nom" placeholder="Ex: Dupont" />
        </div>
        <div class="form-group">
          <label class="form-label">Prénom</label>
          <input class="input" v-model="newLivreur.prenom" placeholder="Ex: Jean" />
        </div>
        <div class="form-group">
          <label class="form-label">Société</label>
          <input class="input" v-model="newLivreur.societe" placeholder="Ex: Chronopost" />
        </div>
        <div class="form-group">
          <label class="form-label">ID Badge RFID *</label>
          <input class="input" v-model="newLivreur.id_badge_rfid" placeholder="Ex: A1B2C3D4" style="font-family:monospace" />
        </div>
        <div class="modal-actions">
          <button class="btn btn--outline" @click="showAddModal = false">Annuler</button>
          <button class="btn btn--primary" :disabled="saving || !newLivreur.nom || !newLivreur.id_badge_rfid" @click="addLivreur">
            {{ saving ? 'Ajout…' : 'Ajouter' }}
          </button>
        </div>
        <div v-if="formError" class="error-msg mt-4">{{ formError }}</div>
      </div>
    </div>

    <!-- Modal confirmation suppression -->
    <div v-if="toDelete" class="modal-overlay" @click.self="toDelete = null">
      <div class="modal">
        <div class="modal-title">Supprimer ce livreur ?</div>
        <p style="color:var(--text-muted);margin-bottom:8px">
          Société : <strong>{{ toDelete.societe }}</strong><br>
          Badge RFID : <span class="text-mono">{{ toDelete.id_badge_rfid }}</span>
        </p>
        <p style="color:var(--error);font-size:13px">Cette action est irréversible.</p>
        <div class="modal-actions">
          <button class="btn btn--outline" @click="toDelete = null">Annuler</button>
          <button class="btn btn--danger" :disabled="saving" @click="deleteLivreur">
            {{ saving ? 'Suppression…' : 'Supprimer' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '@/services/api'

const livreurs    = ref([])
const loading     = ref(true)
const error       = ref(null)
const showAddModal = ref(false)
const toDelete    = ref(null)
const saving      = ref(false)
const formError   = ref(null)
const newLivreur  = reactive({ nom: '', prenom: '', societe: '', id_badge_rfid: '' })

function openAdd() {
  newLivreur.nom = ''; newLivreur.prenom = ''; newLivreur.societe = ''; newLivreur.id_badge_rfid = ''
  formError.value = null
  showAddModal.value = true
}

function confirmDelete(l) { toDelete.value = l }

async function load() {
  loading.value = true; error.value = null
  try {
    const r = await api.getLivreurs()
    livreurs.value = r.data?.data || r.data || []
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function addLivreur() {
  saving.value = true; formError.value = null
  try {
    await api.createLivreur({ ...newLivreur })
    showAddModal.value = false
    await load()
  } catch (e) { formError.value = 'Erreur : ' + e.message }
  finally { saving.value = false }
}

async function deleteLivreur() {
  if (!toDelete.value) return
  saving.value = true
  try {
    await api.deleteLivreur(toDelete.value._id)
    toDelete.value = null
    await load()
  } catch (e) { error.value = 'Erreur suppression : ' + e.message }
  finally { saving.value = false }
}

onMounted(load)
</script>
