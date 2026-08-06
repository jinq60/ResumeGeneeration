import { test, expect } from '@playwright/test'

/**
 * 工作台核心流程（编辑器 / 分享 / 导出）。
 * 需要本地后端（MySQL + MinIO）运行在 8080，否则请跳过：
 *   npm run dev  # 前端
 *   后端以 dev profile 启动后执行：
 *   npm run test:e2e -- tests/e2e/workbench-flow.spec.ts
 */

function randomPhone() {
  return `13${Math.floor(100000000 + Math.random() * 899999999)}`
}

async function registerAndEnterWorkbench(page: import('@playwright/test').Page, phone: string) {
  await page.goto('/login')
  await page.getByRole('button', { name: '注册', exact: true }).click()
  await page.getByPlaceholder('请输入手机号').fill(phone)
  await page.getByPlaceholder('6 位验证码').fill('123456')
  await page.getByPlaceholder('8-32 位，需包含字母和数字').fill('Passw0rd123')
  await page.getByRole('button', { name: '注册', exact: true }).click()
  await expect(page).toHaveURL(/\/workbench\//, { timeout: 15000 })
}

async function createResumeFromWorkbench(page: import('@playwright/test').Page) {
  await page.getByRole('button', { name: '新建简历' }).first().click()
  await expect(page).toHaveURL(/\/workbench\/resumes\/create/)
  await page.locator('article').first().click()
  await page.getByRole('button', { name: '下一步' }).click()
  await expect(page).toHaveURL(/\/workbench\/editor\//, { timeout: 15000 })
}

test.describe('工作台核心流程（需本地后端）', () => {
  test('注册后创建简历并在编辑器填写个人信息', async ({ page }) => {
    const phone = randomPhone()
    await registerAndEnterWorkbench(page, phone)
    await createResumeFromWorkbench(page)

    // 编辑器表单（个人信息模块）
    await page.getByPlaceholder('请输入姓名').fill('张三')
    await page.getByPlaceholder('请输入手机号').fill(phone)
    await page.getByPlaceholder('请输入邮箱').fill('zhangsan@example.com')
    await page.getByPlaceholder('请输入目标岗位').fill('Java 开发工程师')

    // 右侧预览 iframe 加载后展示姓名
    const previewFrame = page.locator('iframe').first().frameLocator(':scope')
    await expect(previewFrame.getByText('张三').first()).toBeVisible({ timeout: 15000 })
  })

  test('简历详情页开启分享并打开分享链接', async ({ page }) => {
    const phone = randomPhone()
    await registerAndEnterWorkbench(page, phone)
    await createResumeFromWorkbench(page)

    // 回到简历列表 → 进入详情
    await page.goBack()
    await expect(page).toHaveURL(/\/workbench\/(resumes|dashboard)/)
    await page.getByRole('link').filter({ hasText: /未命名简历|我的简历/ }).first().click()
    await expect(page).toHaveURL(/\/workbench\/resumes\/\w+$/)

    // 开启分享
    await page.getByRole('button', { name: '分享' }).click()
    await expect(page.getByText('公开分享')).toBeVisible({ timeout: 10000 })
    await page.getByRole('switch').first().click()
    await expect(page.getByText('复制链接').first()).toBeVisible({ timeout: 10000 })

    // 打开分享页（同域 iframe 内容由后端渲染）
    await page.getByText('预览分享页 →').click()
    await page.waitForLoadState('load')
    const shareFrame = page.locator('iframe').first().frameLocator(':scope')
    await expect(shareFrame.getByText('简历分享').first()).toBeVisible({ timeout: 15000 })
  })

  test('导出 Markdown 文件', async ({ page }) => {
    const phone = randomPhone()
    await registerAndEnterWorkbench(page, phone)
    await createResumeFromWorkbench(page)

    // 填写导出必填信息（姓名 + 联系方式）
    await page.getByPlaceholder('请输入姓名').fill('李四')
    await page.getByPlaceholder('请输入手机号').fill(phone)

    // 编辑器 → 导出页
    await page.getByRole('button', { name: '导出' }).first().click()
    await expect(page).toHaveURL(/\/export/, { timeout: 10000 })
    await page.getByText('Markdown', { exact: true }).first().click()

    const downloadPromise = page.waitForEvent('download', { timeout: 15000 })
    await page.getByRole('button', { name: /导出 Markdown/ }).click()
    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/\.md$/)
    await download.cancel()
  })
})
