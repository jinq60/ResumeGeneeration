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
          to="/"
          class="btn-primary"
        >
          <el-icon size="14">
            <HomeFilled />
          </el-icon>
          <span>返回首页</span>
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
  padding: 24px;
  background: var(--st-background);
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
  color: var(--st-primary);
  opacity: 0.18;
}

.not-found-title {
  margin-top: 16px;
  font-size: 24px;
  font-weight: 600;
  color: var(--st-on-surface);
}

.not-found-desc {
  margin-top: 8px;
  font-size: 14px;
  color: var(--st-on-surface-variant);
}

.not-found-actions {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.btn-primary,
.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--st-radius-lg);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
}

.btn-primary {
  background: var(--st-primary);
  color: #fff;
  border: none;
  box-shadow: var(--st-shadow-md);

  &:hover {
    background: var(--st-primary-container);
  }
}

.btn-ghost {
  background: transparent;
  color: var(--st-on-surface-variant);
  border: 1px solid var(--st-outline-variant);

  &:hover {
    background: var(--st-surface-container-low);
  }
}
</style>
