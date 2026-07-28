<template>
  <div class="app-shell">
    <TopNavbar />
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import TopNavbar from './TopNavbar.vue'
</script>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
  width: 100%;
  background: var(--st-background);
}

.app-main {
  flex: 1;
  min-width: 0;
  overflow-x: hidden;
}
</style>