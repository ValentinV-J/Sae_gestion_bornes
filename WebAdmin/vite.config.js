import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 8080,
    proxy: {
      // Proxifie /api vers le serveur Node pour éviter les problèmes CORS en dev
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true
      }
    }
  }
})
