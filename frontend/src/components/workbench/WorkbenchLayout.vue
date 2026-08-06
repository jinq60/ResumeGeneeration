<template>
  <div class="workbench-layout">
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
      @close-mobile="sidebarOpen = false"
    />
    <main class="workbench-main">
      <button
        class="theme-toggle"
        type="button"
        :aria-label="themeIsDark ? '切换到浅色模式' : '切换到深色模式'"
        :title="themeIsDark ? '切换到浅色模式' : '切换到深色模式'"
        @click="toggleTheme"
      >
        <el-icon size="16">
          <Moon v-if="!themeIsDark" />
          <Sunny v-else />
        </el-icon>
      </button>
      <div class="workbench-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Menu, Moon, Sunny } from '@element-plus/icons-vue'
import WorkbenchSidebar from './WorkbenchSidebar.vue'
import { useTheme } from '@/composables/useTheme'

const sidebarOpen = ref(false)
const { preference, toggleTheme } = useTheme()
const themeIsDark = computed(() =>
  preference.value === 'dark'
    || (preference.value === 'system'
      && typeof window !== 'undefined'
      && typeof window.matchMedia === 'function'
      && window.matchMedia('(prefers-color-scheme: dark)').matches)
)
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

.theme-toggle {
  position: fixed;
  top: 16px;
  right: 20px;
  z-index: 40;
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--st-outline-variant);
  border-radius: var(--st-radius-md);
  color: var(--st-on-surface-variant);
  background: var(--st-surface-bright);
  box-shadow: var(--st-shadow-sm);
  cursor: pointer;
  transition: color 0.2s ease;
}

.theme-toggle:hover {
  color: var(--st-primary);
}

@media (max-width: 768px) {
  .theme-toggle {
    top: 12px;
    right: 12px;
  }
}

.workbench-content {
  flex: 1;
  padding: var(--st-margin-page);
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
</style>
