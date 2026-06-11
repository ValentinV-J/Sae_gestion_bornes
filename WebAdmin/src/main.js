import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import '@/style/main.css'

const app = createApp(App)

app.use(createPinia())  // Pinia doit être installé AVANT le router (le guard utilise le store)
app.use(router)

app.mount('#app')
