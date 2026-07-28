<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="not-found-code">
        404
      </div>
      <h1 class="not-found-title">
        页面找不到了
      </h1>
      <p class="not-found-desc">
        你访问的页面可能已被移除、更名或暂时不可用。
      </p>
      <div class="not-found-actions">
        <RouterLink
          to="/dashboard"
          class="btn-primary"
        >
          <el-icon size="14">
            <HomeFilled />
          </el-icon>
          <span>返回工作台</span>
        </RouterLink>
        <button
          class="btn-ghost"
          @click="router.back()"
        >
          返回上一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { HomeFilled } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  background: var(--color-desk-mist);
}

.not-found-content {
  text-align: center;
  max-width: 480px;
}

.not-found-code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.04em;
  color: var(--color-archive-blue);
  opacity: 0.18;
}

.not-found-title {
  margin-top: var(--space-4);
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-ink-navy);
}

.not-found-desc {
  margin-top: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.not-found-actions {
  margin-top: var(--space-8);
  display: flex;
  justify-content: center;
  gap: var(--space-3);
}

.btn-primary,
.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 10px var(--space-5);
  border-radius: var(--radius-lg);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition-base);
  text-decoration: none;
}

.btn-primary {
  background: var(--color-archive-blue);
  color: #fff;
  border: none;
  box-shadow: 0 4px 12px var(--color-archive-blue-12);

  &:hover {
    background: var(--color-archive-blue-80);
  }
}

.btn-ghost {
  background: transparent;
  color: var(--color-text-regular);
  border: 1px solid var(--color-border);

  &:hover {
    background: var(--color-paper-white);
  }
}
</style>
