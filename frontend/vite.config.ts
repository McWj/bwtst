import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// add by codebuddy 20260817
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // add by codebuddy 20260817: 转发第三方ERP接口，避免跨域
      '/wtoptst': {
        target: 'http://106.14.92.92',
        changeOrigin: true
      }
    }
  }
})
