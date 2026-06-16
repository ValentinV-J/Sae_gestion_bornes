<template>
  <div class="login-page">
    <div class="login-card">

      <!-- En-tête -->
      <div class="login-header">
        <span class="login-icon">📦</span>
        <h1 class="login-title">Bornes Admin</h1>
        <p class="login-subtitle">Connectez-vous pour accéder au tableau de bord</p>
      </div>

      <!-- Formulaire -->
      <form class="login-form" @submit.prevent="soumettre">
        <div class="form-group">
          <label class="form-label" for="identifiant">Identifiant</label>
          <input
            id="identifiant"
            v-model="form.identifiant"
            class="input"
            type="text"
            placeholder="ex: admin"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="mot_de_passe">Mot de passe</label>
          <input
            id="mot_de_passe"
            v-model="form.mot_de_passe"
            class="input"
            type="password"
            placeholder="••••••••"
            autocomplete="current-password"
            required
          />
        </div>

        <!-- Message d'erreur -->
        <div v-if="authStore.erreur" class="login-error">
          ⚠️ {{ authStore.erreur }}
        </div>

        <button
          class="btn btn--primary login-btn"
          type="submit"
          :disabled="authStore.loading"
        >
          <span v-if="authStore.loading" class="spinner spinner--sm"></span>
          <span v-else>Se connecter</span>
        </button>
      </form>

    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

const router    = useRouter()
const authStore = useAuthStore()

const form = reactive({
  identifiant:  '',
  mot_de_passe: '',
})

async function soumettre() {
  const ok = await authStore.login(form.identifiant, form.mot_de_passe)
  if (ok) router.push('/')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 24px;
}

.login-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 40px 36px;
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 12px;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 6px;
}

.login-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid var(--error);
  color: var(--error);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  font-size: 13px;
  margin-bottom: 8px;
}

.login-btn {
  width: 100%;
  padding: 10px;
  font-size: 14px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinner--sm {
  width: 16px;
  height: 16px;
  border-width: 2px;
}
</style>
