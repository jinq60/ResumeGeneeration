<script setup lang="ts">
  defineProps<{ html?: string }>()
</script>
<template>
  <div class="flex justify-center p-8 bg-stone">
    <div class="a4 rounded-[6px] overflow-hidden p-[20mm] text-ink">
      <div v-if="html" v-html="html" />
      <slot v-else />
    </div>
  </div>
</template>
