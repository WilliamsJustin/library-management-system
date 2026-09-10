import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// 后端接口代理目标：开发服务器与预览服务器共用
const API_PROXY = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: API_PROXY
  },
  // 本地预览生产构建（dist）时同样把 /api 代理到后端，便于端到端自验
  preview: {
    port: 4173,
    proxy: API_PROXY
  }
})
