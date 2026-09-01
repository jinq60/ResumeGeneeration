<script setup lang="ts">
  defineProps<{ title?: string; desc?: string; actionLabel?: string }>()
  defineEmits<{ (e: 'action'): void }>()
</script>
<template>
  <div
    class="border border-dashed border-[#c7c7c0] rounded-xl bg-[#fbf9f5] p-12 flex flex-col items-center text-center"
  >
    <div
      class="w-12 h-12 rounded-full border border-[#eae8e3] bg-white flex items-center justify-center mb-4"
    >
      <span class="material-symbols-outlined text-[#777871]">inbox</span>
    </div>
    <p class="font-[Newsreader] text-[16px] text-black">{{ title || '暂无内容' }}</p>
    <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-1 max-w-sm">
      {{ desc || '在左侧添加内容，A4 画布将实时更新' }}
    </p>
    <button
      v-if="actionLabel"
      @click="$emit('action')"
      class="mt-4 h-8 px-4 rounded-full bg-black text-white text-[13px] hover:opacity-90"
    >
      {{ actionLabel }}
    </button>
    <slot />
  </div>
</template>
