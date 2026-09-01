<script setup lang="ts">
  defineProps<{ label?: string; modelValue?: string; placeholder?: string; type?: string }>()
  defineEmits<{ (e: 'update:modelValue', v: string): void }>()
</script>
<template>
  <label class="block">
    <span v-if="label" class="text-sm text-ink font-inter mb-1 block">{{ label }}</span>
    <input
      :type="type || 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      class="w-full h-[38px] px-3 rounded-[10px] border border-line bg-white text-sm focus:border-ink focus:outline-none transition"
    />
  </label>
</template>
