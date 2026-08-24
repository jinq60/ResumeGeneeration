<template>
  <main class="workbench-page py-stack-lg">
    <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4 mb-stack-lg">
      <div>
        <h1 class="text-headline-md font-headline-md text-on-surface">
          模板中心
        </h1>
        <p class="text-body-md text-on-surface-variant mt-1">
          选择适合你职业场景的简历模板，一键套用专业排版
        </p>
      </div>
      <div class="flex items-center gap-2 bg-surface-container-lowest border border-outline-variant rounded-lg px-4 py-2 w-full md:w-64 text-on-surface-variant focus-within:ring-1 focus-within:ring-primary focus-within:border-primary transition-all">
        <el-icon size="14">
          <Search />
        </el-icon>
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索模板名称 / 风格"
          class="bg-transparent border-none outline-none flex-1 text-body-md text-on-surface placeholder:text-outline"
        >
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-[220px_1fr] gap-gutter items-start">
      <aside class="lg:sticky lg:top-20 flex flex-row lg:flex-col gap-1 bg-surface-container-lowest rounded-xl border border-outline-variant p-2 overflow-x-auto">
        <button
          v-for="category in categories"
          :key="category.value"
          :class="[
            'flex justify-between items-center px-3 py-2.5 rounded-lg text-body-md text-left whitespace-nowrap transition-colors',
            selectedCategory === category.value
              ? 'bg-primary text-on-primary'
              : 'text-on-surface-variant hover:bg-surface-container-low'
          ]"
          @click="selectedCategory = category.value"
        >
          <span>{{ category.label }}</span>
          <span
            :class="[
              'text-[11px] font-mono',
              selectedCategory === category.value ? 'text-on-primary/70' : 'text-on-surface-variant'
            ]"
          >
            {{ countOf(category.value) }}
          </span>
        </button>
      </aside>

      <section class="min-w-0">
        <div
          v-if="loading"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6"
        >
          <el-skeleton
            :rows="4"
            animated
          />
        </div>

        <div
          v-else
          class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-gutter"
        >
          <article
            v-for="template in filteredTemplates"
            :key="template.id"
            class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm hover:-translate-y-1 hover:shadow-md transition-all"
          >
            <div class="relative aspect-[16/10] bg-surface-container-low flex items-center justify-center overflow-hidden">
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
                <el-icon size="40">
                  <Document />
                </el-icon>
              </div>
              <span
                v-if="template.isRecommended"
                class="absolute top-2 left-2 text-[10px] font-semibold tracking-wide px-2 py-0.5 rounded-full bg-secondary text-on-secondary"
              >推荐</span>
              <span
                v-if="template.isPremium"
                class="absolute top-2 right-2 text-[10px] font-semibold tracking-wide px-2 py-0.5 rounded-full bg-tertiary text-on-tertiary"
              >付费</span>
            </div>
            <div class="p-4">
              <h3 class="text-title-md font-title-md text-on-surface">
                {{ template.name }}
              </h3>
              <p class="text-body-md text-on-surface-variant mt-1 line-clamp-2 leading-relaxed">
                {{ template.description }}
              </p>
              <div class="mt-3 flex justify-between items-center">
                <span class="text-[11px] px-2 py-0.5 bg-surface-container-low rounded-full text-on-surface-variant">
                  {{ getCategoryLabel(template.category) }}
                </span>
                <span class="text-[11px] text-on-surface-variant font-mono">
                  A4 · {{ template.renderEngine || 'server' }}
                </span>
              </div>
              <div class="mt-4 flex gap-2">
                <button
                  class="flex-1 justify-center bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
                  @click="useTemplate(template.id)"
                >
                  <span>使用模板</span>
                  <el-icon size="14">
                    <ArrowRight />
                  </el-icon>
                </button>
                <button
                  class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 font-label-md transition-colors"
                  @click="previewTemplate(template.id)"
                >
                  预览
                </button>
              </div>
            </div>
          </article>
        </div>

        <div
          v-if="!loading && filteredTemplates.length === 0"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant p-12 text-center text-on-surface-variant"
        >
          <el-icon
            size="48"
            class="mb-4"
          >
            <Document />
          </el-icon>
          <p class="text-body-md mb-4">
            暂无符合条件的模板
          </p>
          <button
            class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 font-label-md transition-colors"
            @click="searchKeyword = ''; selectedCategory = 'all'"
          >
            重置筛选
          </button>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Document, ArrowRight } from '@element-plus/icons-vue'
import { templateApi } from '@/api/template'
import { resumeApi } from '@/api/resume'
import { TEMPLATE_PLACEHOLDER } from '@/utils/placeholder'
import type { Template } from '@/api/template'

const router = useRouter()
const loading = ref(false)
const templates = ref<Template[]>([])
const selectedCategory = ref('all')
const searchKeyword = ref('')

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

function countOf(cat: string) {
  if (cat === 'all') return templates.value.length
  return templates.value.filter(t => t.category === cat).length
}

const filteredTemplates = computed(() => {
  let list = templates.value
  if (selectedCategory.value !== 'all') {
    list = list.filter(t => t.category === selectedCategory.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(t =>
      t.name.toLowerCase().includes(kw) ||
      (t.description && t.description.toLowerCase().includes(kw)))
  }
  return list
})

function getCategoryLabel(category: string) {
  const cat = categories.find(c => c.value === category)
  return cat ? cat.label : category
}

async function loadTemplates() {
  loading.value = true
  try {
    templates.value = await templateApi.list()
  } catch (e: any) {
    ElMessage.error(e.message || '模板加载失败')
    templates.value = []
  } finally {
    loading.value = false
  }
}

async function useTemplate(templateId: string) {
  try {
    const resume = await resumeApi.create({
      templateId,
      scene: 'campus_recruitment',
      title: '未命名简历'
    })
    router.push(`/workbench/editor/${resume.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建简历失败')
  }
}

function previewTemplate(templateId: string) {
  router.push(`/workbench/templates/${templateId}`)
}

onMounted(() => { loadTemplates() })
</script>

<style scoped>
/* 所有样式已迁移至 Tailwind 工具类 */
</style>
