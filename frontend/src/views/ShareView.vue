<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRoute } from 'vue-router'
  const route = useRoute()
  const html = ref('')
  const loading = ref(true)
  onMounted(async () => {
    try {
      const base = (import.meta as any).env?.VITE_API_BASE || '/api'
      const res = await fetch(`${base}/share/${route.params.token as string}`)
      if (res.ok) html.value = await res.text()
      else
        html.value = `<div style="padding:48px;text-align:center;font-family:Inter;color:#777871">分享不存在或已过期</div>`
    } catch {
      html.value = `<div style="padding:48px;text-align:center">加载失败</div>`
    } finally {
      loading.value = false
    }
  })
</script>
<template>
  <div class="min-h-screen bg-[#ede9e3]">
    <header
      class="h-14 bg-[#fbf9f5] border-b border-[#c7c7c0] flex items-center justify-center sticky top-0"
    >
      <span class="font-[Newsreader] font-semibold">Resume Atelier</span>
      <span class="ml-2 font-[JetBrains_Mono] text-[11px] tracking-widest uppercase text-[#777871]"
        >Public Preview</span
      >
    </header>
    <div
      v-if="loading"
      class="flex justify-center p-12 font-[JetBrains_Mono] text-[11px] text-[#777871]"
    >
      加载中…
    </div>
    <div
      v-else
      v-html="html"
      class="max-w-[210mm] mx-auto my-8 shadow-[0_4px_24px_rgba(0,0,0,0.04)] bg-white"
    ></div>
    <footer class="text-center py-8 font-[JetBrains_Mono] text-[11px] text-[#777871]">
      由 Resume Atelier 生成 · 只读预览
    </footer>
  </div>
</template>
