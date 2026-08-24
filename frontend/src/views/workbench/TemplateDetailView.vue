<template>
  <main class="workbench-page py-stack-lg">
    <div class="flex justify-between items-center mb-stack-lg">
      <div class="flex items-center gap-3">
        <button
          class="w-8 h-8 bg-surface-container-lowest border border-outline-variant rounded-lg flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-surface-container transition-colors"
          title="返回"
          @click="router.back()"
        >
          <el-icon size="16">
            <ArrowLeft />
          </el-icon>
        </button>
        <h1 class="text-headline-md font-headline-md text-on-surface">
          模板详情
        </h1>
      </div>
      <button
        class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
        :disabled="!template"
        @click="useTemplate"
      >
        <span>使用模板</span>
        <el-icon size="14">
          <ArrowRight />
        </el-icon>
      </button>
    </div>

    <div
      v-if="loading"
      class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6"
    >
      <el-skeleton
        :rows="6"
        animated
      />
    </div>

    <div
      v-else-if="!template"
      class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 text-center text-on-surface-variant"
    >
      <el-icon
        size="48"
        class="mb-4"
      >
        <Document />
      </el-icon>
      <p class="text-body-md mb-4">
        模板不存在或已下架
      </p>
      <RouterLink
        to="/workbench/templates"
        class="inline-flex items-center justify-center gap-2 bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md shadow-sm hover:scale-[0.98] transition-transform"
      >
        返回模板中心
      </RouterLink>
    </div>

    <div
      v-else
      class="grid grid-cols-1 lg:grid-cols-[1fr_380px] gap-gutter items-start"
    >
      <section class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 flex justify-center">
        <div class="w-full max-w-[595px] aspect-[210/297] bg-surface-container-low rounded shadow-md overflow-hidden flex items-center justify-center">
          <img
            v-if="template.thumbnailUrl"
            :src="template.thumbnailUrl"
            :alt="template.name"
            class="w-full h-full object-cover"
            @error="onThumbnailError"
          >
          <div
            v-else
            class="text-outline"
          >
            <el-icon size="64">
              <Document />
            </el-icon>
          </div>
        </div>
      </section>

      <aside class="flex flex-col gap-stack-md">
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm">
          <h2 class="text-title-lg font-title-lg text-on-surface">
            {{ template.name }}
          </h2>
          <div class="mt-2 flex gap-2 flex-wrap">
            <span
              v-if="template.isRecommended"
              class="text-[11px] font-semibold tracking-wide px-2 py-0.5 rounded-full bg-secondary text-on-secondary"
            >推荐</span>
            <span
              v-if="template.isPremium"
              class="text-[11px] font-semibold tracking-wide px-2 py-0.5 rounded-full bg-tertiary text-on-tertiary"
            >付费</span>
            <span class="text-[11px] font-semibold tracking-wide px-2 py-0.5 rounded-full bg-surface-container-low text-on-surface-variant">
              {{ getCategoryLabel(template.category) }}
            </span>
          </div>

          <p class="mt-4 text-body-md text-on-surface-variant leading-relaxed">
            {{ template.description || '暂无描述' }}
          </p>

          <ul class="mt-5 space-y-2">
            <li class="flex justify-between text-body-md text-on-surface-variant py-2 border-b border-outline-variant/30">
              <span>渲染引擎</span>
              <span class="font-mono">{{ template.renderEngine || 'server' }}</span>
            </li>
            <li class="flex justify-between text-body-md text-on-surface-variant py-2 border-b border-outline-variant/30">
              <span>纸张规格</span>
              <span class="font-mono">A4 · 210 × 297 mm</span>
            </li>
            <li class="flex justify-between text-body-md text-on-surface-variant py-2 border-b border-outline-variant/30">
              <span>更新时间</span>
              <span>{{ template.updatedAt || '—' }}</span>
            </li>
            <li class="flex justify-between text-body-md text-on-surface-variant py-2">
              <span>状态</span>
              <span>{{ template.status === 'active' ? '已上线' : '已下架' }}</span>
            </li>
          </ul>

          <div class="mt-6 flex flex-col gap-3">
            <button
              class="w-full justify-center bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              @click="useTemplate"
            >
              <span>使用此模板创建简历</span>
              <el-icon size="14">
                <ArrowRight />
              </el-icon>
            </button>
            <RouterLink
              to="/workbench/templates"
              class="w-full justify-center inline-flex items-center gap-2 border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 font-label-md transition-colors"
            >
              返回模板中心
            </RouterLink>
          </div>
        </div>

        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm">
          <h3 class="text-title-md font-title-md text-on-surface">
            使用建议
          </h3>
          <p class="mt-2 text-body-md text-on-surface-variant leading-relaxed">
            该模板适用于 {{ getCategoryLabel(template.category) }} 场景。创建简历后可在编辑器中自由调整模块顺序与内容。
          </p>
        </div>
      </aside>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Document } from '@element-plus/icons-vue'
import { templateApi } from '@/api/template'
import { resumeApi } from '@/api/resume'
import { TEMPLATE_PLACEHOLDER } from '@/utils/placeholder'
import type { Template } from '@/api/template'

const router = useRouter()
const route = useRoute()
const templateId = route.params.id as string

const loading = ref(false)
const template = ref<Template | null>(null)

function onThumbnailError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img && img.src !== TEMPLATE_PLACEHOLDER) {
    img.src = TEMPLATE_PLACEHOLDER
  }
}

const categories = [
  { label: '全部', value: 'all' },
  { label: '经典', value: 'classic' },
  { label: '技术', value: 'tech' },
  { label: '应届生', value: 'fresh' },
  { label: '商务', value: 'business' },
  { label: '考研复试', value: 'postgraduate' }
]

function getCategoryLabel(category: string) {
  const cat = categories.find(c => c.value === category)
  return cat ? cat.label : category
}

async function loadTemplate() {
  loading.value = true
  try {
    template.value = await templateApi.get(templateId)
  } catch (e: any) {
    ElMessage.error(e.message || '模板加载失败')
    template.value = null
  } finally {
    loading.value = false
  }
}

async function useTemplate() {
  if (!template.value) return
  try {
    const resume = await resumeApi.create({
      templateId: template.value.id,
      scene: 'campus_recruitment',
      title: '未命名简历'
    })
    router.push(`/workbench/editor/${resume.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建简历失败')
  }
}

onMounted(() => { loadTemplate() })
</script>

<style scoped>
/* 所有样式已迁移至 Tailwind 工具类 */
</style>
