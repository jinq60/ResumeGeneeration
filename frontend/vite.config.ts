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
      },
      '/templates': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 内置模板缩略图 /templates/thumbs/** 由后端 /api/templates/thumbs/** 提供服务
        rewrite: (path) => path.replace(/^\/templates/, '/api/templates')
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        // 三方库分包，减小首屏主包体积
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          axios: ['axios'],
          cropper: ['vue-cropper']
        }
      }
    }
  }
})
