<template>
  <main class="min-h-screen bg-background flex flex-col">
    <div
      v-if="errorMsg"
      class="flex-1 flex flex-col items-center justify-center gap-4 text-on-surface-variant p-8"
    >
      <el-icon
        size="48"
        class="text-outline"
      >
        <Warning />
      </el-icon>
      <p class="text-body-md">
        {{ errorMsg }}
      </p>
      <RouterLink
        to="/"
        class="text-primary text-sm hover:underline"
      >
        返回首页
      </RouterLink>
    </div>

    <div
      v-else
      class="flex-1 flex flex-col items-center p-4"
    >
      <iframe
        :src="iframeSrc"
        class="w-full max-w-[210mm] flex-1 bg-surface-container-lowest rounded-sm shadow-sm border border-outline-variant"
        frameborder="0"
        sandbox="allow-same-origin"
      />
      <div class="py-4 text-xs text-outline text-center">
        由
        <RouterLink
          to="/"
          class="text-primary no-underline"
        >
          智能简历
        </RouterLink>
        生成 · 本页面为公开只读预览
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Warning } from '@element-plus/icons-vue'

const route = useRoute()
const token = route.params.token as string

const errorMsg = ref('')

const iframeSrc = computed(() => {
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
  return `${base}/share/${encodeURIComponent(token)}`
})

// 无 token 时直接提示
if (!token) {
  errorMsg.value = '分享链接无效'
}
</script>
