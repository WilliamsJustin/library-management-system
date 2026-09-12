import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// 后端接口代理目标：开发服务器与预览服务器共用。
// 可用环境变量 API_PROXY_TARGET 覆盖（便于对着临时端口上的后端做端到端自验）。
const API_PROXY_TARGET = process.env.API_PROXY_TARGET || 'http://localhost:8080'

const API_PROXY = {
  '/api': {
    target: API_PROXY_TARGET,
    changeOrigin: true
  },
  // 上传的封面等静态文件由后端 /uploads/** 提供，开发/预览时同样需要代理
  '/uploads': {
    target: API_PROXY_TARGET,
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
