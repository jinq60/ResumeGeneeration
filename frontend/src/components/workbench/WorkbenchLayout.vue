<template>
  <div class="workbench-layout" :class="{ 'is-sidebar-collapsed': sidebarCollapsed }">
    <div
      v-if="sidebarOpen"
      class="mobile-sidebar-overlay"
      @click="sidebarOpen = false"
    />
    <button
      class="mobile-menu-trigger"
      aria-label="打开工作台导航"
      @click="sidebarOpen = true"
    >
      <el-icon><Menu /></el-icon>
    </button>
    <WorkbenchSidebar
      :mobile-open="sidebarOpen"
      :collapsed="sidebarCollapsed"
      @close-mobile="sidebarOpen = false"
      @toggle-collapse="toggleSidebar"
    />
    <main class="workbench-main">
      <div class="workbench-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Menu } from '@element-plus/icons-vue'
import WorkbenchSidebar from './WorkbenchSidebar.vue'

const sidebarOpen = ref(false)
const sidebarCollapsed = ref(false)

onMounted(() => {
  sidebarCollapsed.value = localStorage.getItem('workbench_sidebar_collapsed') === 'true'
})

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('workbench_sidebar_collapsed', String(sidebarCollapsed.value))
}
</script>

<style scoped lang="scss">
.workbench-layout {
  --st-sidebar-current-width: var(--st-sidebar-width, 220px);
  min-height: 100vh;
  background: var(--st-background);
}

.workbench-main {
  flex: 1;
  margin-left: var(--st-sidebar-current-width);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.workbench-content {
  flex: 1;
  width: 100%;
  min-width: 0;
  padding: clamp(20px, 2.2vw, 40px) clamp(20px, 2.5vw, 48px);
}

.workbench-layout.is-sidebar-collapsed {
  --st-sidebar-current-width: 72px;
}

/* 页面级容器在工作台中应填满可用桌面宽度；页面内部再决定自己的阅读宽度。 */
.workbench-content :deep(.workbench-page) {
  width: 100%;
  max-width: none;
  margin-left: 0;
  margin-right: 0;
}

.mobile-menu-trigger,
.mobile-sidebar-overlay {
  display: none;
}

@media (max-width: 768px) {
  .workbench-main {
    margin-left: 0;
  }

  .workbench-content {
    padding: 12px;
  }

  .mobile-sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 35;
    background: rgba(15, 23, 42, 0.28);
    backdrop-filter: blur(2px);
  }

  .mobile-menu-trigger {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    position: fixed;
    top: 12px;
    left: 12px;
    z-index: 50;
    width: 36px;
    height: 36px;
    border: 1px solid var(--st-outline-variant);
    border-radius: var(--st-radius-md);
    color: var(--st-on-surface);
    background: var(--st-surface-bright);
    box-shadow: var(--st-shadow-sm);
  }
}

@media (min-width: 1600px) {
  .workbench-content {
    padding-left: clamp(40px, 4vw, 80px);
    padding-right: clamp(40px, 4vw, 80px);
  }
}
</style>
