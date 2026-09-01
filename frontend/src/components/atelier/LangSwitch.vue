<script setup lang="ts">
  import { useI18n } from 'vue-i18n'
  import { setLocale, type Locale } from '@/i18n'
  const { locale } = useI18n()
  function toggle() {
    const next: Locale = locale.value === 'zh' ? 'en' : 'zh'
    setLocale(next)
  }
</script>
<template>
  <button
    @click="toggle"
    class="h-8 px-3 rounded-full border border-[#c7c7c0] bg-white text-[11px] font-[JetBrains_Mono] tracking-[0.06em] hover:border-black flex items-center gap-1"
  >
    <span class="material-symbols-outlined text-[16px]">language</span>
    {{ locale === 'zh' ? 'EN' : '中' }}
  </button>
</template>
