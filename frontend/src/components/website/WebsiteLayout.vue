<template>
  <div class="website-layout">
    <header class="website-header">
      <div class="header-inner">
        <div class="header-left">
          <RouterLink
            to="/"
            class="header-brand"
          >
            <div class="brand-mark">
              <el-icon
                color="#fff"
                size="18"
              >
                <Document />
              </el-icon>
            </div>
            <span class="brand-name">智能简历</span>
          </RouterLink>

          <nav class="header-nav">
            <RouterLink
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              :class="['header-link', { 'is-active': isActive(item.path) }]"
            >
              {{ item.label }}
            </RouterLink>
          </nav>
        </div>

        <div class="header-right">
          <RouterLink
            to="/login"
            class="header-link"
          >
            登录
          </RouterLink>
          <RouterLink
            to="/login"
            class="header-cta"
          >
            免费试用
          </RouterLink>
        </div>
      </div>
    </header>

    <main class="website-main">
      <router-view />
    </main>

    <footer class="website-footer">
      <div class="footer-inner">
        <div>
          <div class="footer-brand">
            <el-icon size="24">
              <Document />
            </el-icon>
            智能简历
          </div>
          <p class="footer-desc">
            AI 驱动的职场晋升利器
          </p>
        </div>
        <div>
          <h4 class="footer-title">
            产品
          </h4>
          <ul class="footer-links">
            <li>
              <RouterLink to="/features">
                功能特性
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/templates">
                模板中心
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/pricing">
                定价方案
              </RouterLink>
            </li>
          </ul>
        </div>
        <div>
          <h4 class="footer-title">
            支持
          </h4>
          <ul class="footer-links">
            <li>
              <RouterLink to="/help">
                帮助文档
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/contact">
                联系我们
              </RouterLink>
            </li>
            <li>
              <RouterLink to="/about">
                关于我们
              </RouterLink>
            </li>
          </ul>
        </div>
        <div>
          <h4 class="footer-title">
            关注我们
          </h4>
          <p class="footer-desc">
            © {{ currentYear }} 智能简历. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Document } from '@element-plus/icons-vue'

const route = useRoute()
const currentYear = new Date().getFullYear()

const navItems = [
  { path: '/', label: '首页' },
  { path: '/features', label: '功能' },
  { path: '/templates', label: '模板' },
  { path: '/pricing', label: '定价' },
  { path: '/about', label: '关于' },
  { path: '/contact', label: '联系' },
  { path: '/help', label: '帮助' }
]

function isActive(path: string) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style scoped lang="scss">
.website-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--st-surface-container-lowest);
}

.website-header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--st-outline-variant);
}

.header-inner {
  max-width: 1440px;
  height: 100%;
  margin: 0 auto;
  padding: 0 var(--st-margin-page);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--st-stack-md);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--st-stack-lg);
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--st-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-family: var(--st-font-headline);
  font-size: 18px;
  font-weight: 700;
  color: var(--st-primary);
  letter-spacing: -0.02em;
}

.header-nav {
  display: none;
  align-items: center;
  gap: var(--st-stack-md);

  @media (min-width: 768px) {
    display: flex;
  }
}

.header-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--st-on-surface-variant);
  text-decoration: none;
  transition: color 0.2s ease;

  &:hover,
  &.is-active {
    color: var(--st-primary);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--st-stack-md);
}

.header-cta {
  padding: 8px 16px;
  border-radius: var(--st-radius-md);
  background: var(--st-primary);
  color: var(--st-on-primary);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.9;
  }
}

.website-main {
  flex: 1;
}

.website-footer {
  background: var(--st-on-surface);
  color: var(--st-surface-variant);
  padding: 48px var(--st-margin-page);
}

.footer-inner {
  max-width: 1440px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;

  @media (min-width: 768px) {
    grid-template-columns: 2fr 1fr 1fr 1fr;
  }
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}

.footer-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 16px;
}

.footer-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.64);
  line-height: 1.6;
}

.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;

  a {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.64);
    text-decoration: none;
    transition: color 0.2s ease;

    &:hover {
      color: #fff;
    }
  }
}
</style>
