<template>
  <div class="workbench-layout">
    <WorkbenchSidebar />
    <main class="workbench-main">
      <div class="workbench-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import WorkbenchSidebar from './WorkbenchSidebar.vue'
</script>

<style scoped lang="scss">
.workbench-layout {
  display: flex;
  min-height: 100vh;
  background: var(--st-background);
}

.workbench-main {
  flex: 1;
  margin-left: var(--st-sidebar-width, 220px);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.workbench-content {
  flex: 1;
  padding: var(--st-margin-page);
}
</style>
