import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'

const { messageMocks, reviewMock, getLatestReviewMock, getMock, updateMock } = vi.hoisted(() => ({
  messageMocks: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  reviewMock: vi.fn(),
  getLatestReviewMock: vi.fn(),
  getMock: vi.fn(),
  updateMock: vi.fn()
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: messageMocks,
    ElMessageBox: { confirm: vi.fn() }
  }
})

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: 'resume_1' }, query: {} }),
  useRouter: () => ({ back: vi.fn(), push: vi.fn() })
}))

vi.mock('@/api/resume', () => ({
  resumeApi: {
    review: reviewMock,
    getLatestReview: getLatestReviewMock,
    getLatestOptimize: vi.fn().mockResolvedValue(null),
    createOptimizeTask: vi.fn(),
    getOptimizeTask: vi.fn(),
    get: getMock,
    update: updateMock
  }
}))

import AIReviewView from '@/views/workbench/AIReviewView.vue'

async function mountView() {
  const wrapper = mount(AIReviewView, {
    global: { plugins: [ElementPlus] }
  })
  await flushPromises()
  return wrapper
}

describe('AIReviewView 点评轮询', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('只采纳本次任务 reviewId 对应的点评，不回显历史旧结果', async () => {
    reviewMock.mockResolvedValue({ reviewId: 'rev_2', resumeId: 'resume_1' })
    // 第一次轮询返回历史旧点评（rev_1），第二次才返回本次结果（rev_2），且 overallScore 为 0
    getLatestReviewMock
      .mockResolvedValueOnce({ reviewId: 'rev_1', overallScore: 90 })
      .mockResolvedValue({ reviewId: 'rev_2', overallScore: 0 })

    const wrapper = await mountView()
    ;(wrapper.vm as any).reviewForm.jobDescription = '目标岗位JD'
    const promise = (wrapper.vm as any).handleAnalyze()
    await vi.advanceTimersByTimeAsync(3000)
    await promise

    // 历史旧结果没有被采纳
    expect((wrapper.vm as any).reviewResult?.reviewId).toBe('rev_2')
    expect((wrapper.vm as any).reviewResult?.overallScore).toBe(0)
    expect(messageMocks.success).toHaveBeenCalledWith('分析完成')
  })

  it('得分为 0 的完成态也能正常结束轮询（不再依赖 overallScore 真值判断）', async () => {
    reviewMock.mockResolvedValue({ reviewId: 'rev_3' })
    getLatestReviewMock.mockResolvedValue({ reviewId: 'rev_3', overallScore: 0 })

    const wrapper = await mountView()
    ;(wrapper.vm as any).reviewForm.jobDescription = '目标岗位JD'
    const promise = (wrapper.vm as any).handleAnalyze()
    await vi.advanceTimersByTimeAsync(1500)
    await promise

    expect(getLatestReviewMock).toHaveBeenCalledTimes(1)
    expect(messageMocks.success).toHaveBeenCalled()
  })

  it('超时未完成时提示稍后查看而不是成功', async () => {
    reviewMock.mockResolvedValue({ reviewId: 'rev_4' })
    // 始终返回历史旧点评：本次任务一直未完成
    getLatestReviewMock.mockResolvedValue({ reviewId: 'rev_1', overallScore: 90 })

    const wrapper = await mountView()
    ;(wrapper.vm as any).reviewForm.jobDescription = '目标岗位JD'
    const promise = (wrapper.vm as any).handleAnalyze()
    await vi.advanceTimersByTimeAsync(70000)
    await promise

    expect(getLatestReviewMock).toHaveBeenCalledTimes(60)
    expect((wrapper.vm as any).reviewResult).toBeNull()
    expect(messageMocks.warning).toHaveBeenCalledWith('分析仍在进行中，请稍后在点评中心查看')
    expect(messageMocks.success).not.toHaveBeenCalledWith('分析完成')
  })
})

describe('AIReviewView 应用建议', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('无 JD 优化结果时提示先运行 JD 优化且不写入简历', async () => {
    const wrapper = await mountView()
    ;(wrapper.vm as any).handleApplySuggestions()

    expect(messageMocks.info).toHaveBeenCalledWith(expect.stringContaining('JD 匹配深度优化'))
    expect(updateMock).not.toHaveBeenCalled()
  })

  it('有 JD 优化结果时将缺失技能合并进技能章节', async () => {
    getMock.mockResolvedValue({
      id: 'resume_1',
      title: '简历',
      sections: [
        {
          id: 'skill_1',
          type: 'skill',
          title: '技能清单',
          order: 1,
          visible: true,
          data: [{ category: '专业技能', items: [{ name: 'Vue' }] }]
        }
      ]
    })
    updateMock.mockResolvedValue({ id: 'resume_1', updatedAt: '' })

    const wrapper = await mountView()
    ;(wrapper.vm as any).optimizeResult = { status: 'success', missingSkills: ['TypeScript', 'vue'] }
    await (wrapper.vm as any).applyMissingSkillsToResume(['TypeScript', 'vue'])

    expect(updateMock).toHaveBeenCalledWith('resume_1', expect.objectContaining({
      sections: expect.any(Array)
    }))
    const payload = updateMock.mock.calls[0][1]
    const skillSection = payload.sections.find((s: any) => s.type === 'skill')
    const items = skillSection.data[0].items
    // 已存在（忽略大小写）的不重复添加
    expect(items.map((i: any) => i.name)).toEqual(['Vue', 'TypeScript'])
  })
})
