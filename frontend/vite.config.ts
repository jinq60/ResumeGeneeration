import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import ElementPlusPlugin from 'unplugin-element-plus/vite'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
      dts: 'src/types/auto-imports.d.ts',
      imports: []
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
      dts: 'src/types/components.d.ts',
      dirs: ['src/components']
    }),
    ElementPlusPlugin({ useSource: false })
  ],
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
    // 生产关闭 sourcemap，避免源码公开可下载；排障时改为 'hidden' 并上传到私有监控平台
    sourcemap: false,
    rollupOptions: {
      output: {
        // 三方库分包，减小首屏主包体积
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          axios: ['axios'],
          cropper: ['vue-cropper']
        }
      }
    }
  }
})
