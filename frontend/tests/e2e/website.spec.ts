import { test, expect } from '@playwright/test'

test.describe('官网页面冒烟', () => {
  test('首页展示产品定位、CTA 与核心功能', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveTitle(/智能简历生成/)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.getByText('免费生成简历').first()).toBeVisible()
    await expect(page.getByText('智能生成').first()).toBeVisible()
  })

  test('功能页列出核心能力', async ({ page }) => {
    await page.goto('/features')
    await expect(page).toHaveTitle(/功能特性/)
    await expect(page.getByText('AI 简历点评').first()).toBeVisible()
    await expect(page.getByText('一键导出 PDF').first()).toBeVisible()
  })

  test('模板展示页可加载', async () => {
    // 注意：/templates 前缀被 vite 代理转发到后端（模板缩略图接口），
    // 因此该页仅在本地后端运行时才能访问，此处由 workbench-flow.spec.ts 覆盖。
    test.skip(true, '需要本地后端运行时（/templates 被 dev 代理接管）')
  })

  test('定价页展示免费版', async ({ page }) => {
    await page.goto('/pricing')
    await expect(page).toHaveTitle(/定价方案/)
    await expect(page.getByText('免费版').first()).toBeVisible()
    await expect(page.getByText('免费开始').first()).toBeVisible()
  })

  test('帮助文档页展示常见问题', async ({ page }) => {
    await page.goto('/help')
    await expect(page).toHaveTitle(/帮助文档/)
    await expect(page.getByText('如何创建第一份简历？')).toBeVisible()
  })

  test('关于页可加载', async ({ page }) => {
    await page.goto('/about')
    await expect(page).toHaveTitle(/关于我们/)
  })

  test('未知路径未登录时重定向到登录页（路由守卫）', async ({ page }) => {
    await page.goto('/definitely-not-a-page')
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByText('欢迎回来')).toBeVisible()
  })
})
