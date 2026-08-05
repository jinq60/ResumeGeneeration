import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 后端 context-path 为 /api，/uploads/** 实际由 /api/uploads/** 提供服务
        rewrite: (path) => path.replace(/^\/uploads/, '/api/uploads')
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})
