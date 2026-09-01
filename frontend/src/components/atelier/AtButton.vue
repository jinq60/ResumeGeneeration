<script setup lang="ts">
  defineProps<{ variant?: 'primary' | 'ghost' | 'ai'; size?: 'sm' | 'md'; disabled?: boolean }>()
</script>
<template>
  <button
    :disabled="disabled"
    :class="[
      'inline-flex items-center justify-center font-inter font-medium transition',
      variant === 'ghost'
        ? 'bg-transparent text-graphite hover:text-ink hover:bg-stone'
        : 'bg-ink text-paper hover:bg-black',
      variant === 'ai' ? 'gap-2' : '',
      size === 'sm' ? 'h-8 px-3 text-sm rounded-md' : 'h-9 px-5 text-sm rounded-full',
      'border border-line focus-visible:border-ink',
      disabled ? 'opacity-40 cursor-not-allowed pointer-events-none' : '',
    ]"
  >
    <span v-if="variant === 'ai'" class="w-1.5 h-1.5 rounded-full bg-vermilion shrink-0"></span>
    <slot />
  </button>
</template>
