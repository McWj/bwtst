import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// add by codebuddy 20260819: 移动端前端，代理 /api 到后端 8081
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/wtoptst': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
