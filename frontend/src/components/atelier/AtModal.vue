<script setup lang="ts">
  defineProps<{ modelValue: boolean }>()
  defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div
          class="absolute inset-0 bg-black/25 backdrop-blur-[2px]"
          @click="$emit('update:modelValue', false)"
        ></div>
        <div
          class="relative bg-white border border-[#eae8e3] rounded-xl p-6 w-full max-w-[440px] shadow-xl max-h-[90vh] overflow-y-auto custom-scroll"
        >
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
  .modal-fade-enter-active,
  .modal-fade-leave-active {
    transition: opacity 180ms ease;
  }
  .modal-fade-enter-from,
  .modal-fade-leave-to {
    opacity: 0;
  }
  .custom-scroll::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  .custom-scroll::-webkit-scrollbar-thumb {
    background: #c7c7c0;
    border-radius: 9999px;
  }
</style>
