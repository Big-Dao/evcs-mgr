import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: '127.0.0.1',
    port: 3000,
    strictPort: true,
    proxy: {
      '/api/auth': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        rewrite: (p: string) => p.replace(/^\/api/, '')
      },
      '/api/station': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
        rewrite: (p: string) => p.replace(/^\/api/, '')
      },
      '/api/charger': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
        rewrite: (p: string) => p.replace(/^\/api/, '')
      },
      '/api/dashboard': {
        target: 'http://127.0.0.1:8086',
        changeOrigin: true,
        rewrite: (p: string) => p.replace(/^\/api/, '')
      },
      '/api/order': {
        target: 'http://127.0.0.1:8083',
        changeOrigin: true,
        rewrite: (p: string) => p.replace(/^\/api/, '')
      },
      '/api/billing': {
        target: 'http://127.0.0.1:8083',
        changeOrigin: true,
        rewrite: (p: string) => p.replace(/^\/api/, '')
      },
      '/api/tenant': {
        target: 'http://127.0.0.1:8086',
        changeOrigin: true,
        rewrite: (p: string) => p.replace(/^\/api/, '')
      }
    }
  }
})
