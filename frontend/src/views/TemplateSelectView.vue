<template>
  <MainLayout>
    <div class="flex flex-col h-screen bg-surface">
      <header class="h-16 flex-shrink-0 px-6 bg-surface-container-lowest border-b border-outline-variant flex items-center justify-between">
        <div class="flex items-center gap-3">
          <button
            class="w-8 h-8 flex items-center justify-center rounded-md border border-outline-variant text-on-surface-variant hover:bg-surface-container-low transition-colors"
            title="返回"
            @click="handleBack"
          >
            <el-icon size="16">
              <ArrowLeft />
            </el-icon>
          </button>
          <h1 class="text-title-md font-title-md text-on-surface">
            选择模板
          </h1>
        </div>
        <div class="flex items-center gap-2 px-4 py-2 bg-surface-container-lowest border border-outline-variant rounded-lg w-60 text-on-surface-variant focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/10">
          <el-icon size="14">
            <Search />
          </el-icon>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索模板"
            class="bg-transparent border-none outline-none flex-1 text-body-md text-on-surface placeholder:text-on-surface-variant"
          >
        </div>
      </header>

      <div class="flex-1 flex overflow-hidden">
        <aside class="w-[200px] flex-shrink-0 bg-surface-container-lowest border-r border-outline-variant px-3 py-4 flex flex-col gap-0.5 overflow-y-auto">
          <button
            v-for="category in categories"
            :key="category.value"
            :class="[
              'flex justify-between items-center px-3 py-2.5 bg-transparent border-none rounded-lg text-body-md font-medium transition-colors',
              selectedCategory === category.value
                ? 'bg-primary text-on-primary shadow-sm'
                : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
            ]"
            @click="selectedCategory = category.value"
          >
            <span>{{ category.label }}</span>
            <span
              :class="[
                'font-mono text-label-md',
                selectedCategory === category.value ? 'text-on-primary/70' : 'text-on-surface-variant'
              ]"
            >{{ countOf(category.value) }}</span>
          </button>
        </aside>

        <section class="flex-1 p-6 overflow-y-auto">
          <div
            v-if="loading"
            class="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant/30"
          >
            <el-skeleton
              :rows="4"
              animated
            />
          </div>

          <div
            v-else
            class="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-5"
          >
            <article
              v-for="template in filteredTemplates"
              :key="template.id"
              :class="[
                'bg-surface-container-lowest border-2 rounded-xl overflow-hidden cursor-pointer transition-all hover:-translate-y-1 hover:shadow-md',
                selectedTemplateId === template.id
                  ? 'border-primary ring-2 ring-primary/10 shadow-md'
                  : 'border-transparent shadow-sm'
              ]"
              @click="selectTemplate(template)"
            >
              <div class="relative aspect-[16/10] bg-surface-container-low flex items-center justify-center overflow-hidden">
                <img
                  v-if="template.thumbnailUrl"
                  class="w-full h-full object-cover"
                  :src="template.thumbnailUrl"
                  :alt="template.name"
                >
                <div
                  v-else
                  class="text-outline"
                >
                  <el-icon
                    size="40"
                  >
                    <Document />
                  </el-icon>
                </div>
                <span
                  v-if="template.isRecommended"
                  class="absolute top-2 left-2 px-2 py-0.5 bg-secondary text-on-secondary text-label-md font-semibold rounded-full tracking-wider"
                >推荐</span>
                <span
                  v-if="template.isPremium"
                  class="absolute top-2 right-2 px-2 py-0.5 bg-surface-inverse text-on-primary text-label-md font-semibold rounded-full tracking-wider"
                >付费</span>
              </div>
              <div class="p-4">
                <h3 class="text-title-md font-title-md text-on-surface">
                  {{ template.name }}
                </h3>
                <p class="mt-1 text-label-md text-on-surface-variant line-clamp-2 leading-relaxed">
                  {{ template.description }}
                </p>
                <div class="mt-3 flex justify-between items-center">
                  <span class="text-label-md px-2 py-0.5 bg-surface-container-low text-on-surface-variant rounded-full">{{ getCategoryLabel(template.category) }}</span>
                  <span class="font-mono text-label-md text-on-surface-variant">A4 · {{ template.renderEngine || 'server' }}</span>
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
              @click="loadTemplates"
            >
              重新加载
            </button>
          </div>
        </section>
      </div>

      <footer class="px-6 py-4 bg-surface-container-lowest border-t border-outline-variant flex justify-end gap-3">
        <button
          class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 font-label-md transition-colors"
          @click="handleBack"
        >
          取消
        </button>
        <button
          class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!selectedTemplateId"
          @click="handleConfirm"
        >
          下一步
          <el-icon size="14">
            <ArrowRight />
          </el-icon>
        </button>
      </footer>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Search, Document } from '@element-plus/icons-vue'
import { templateApi } from '@/api/template'
import { resumeApi } from '@/api/resume'
import type { Template } from '@/api/template'
import MainLayout from '@/components/layout/MainLayout.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const templates = ref<Template[]>([])
const selectedCategory = ref('all')
const selectedTemplateId = ref('')
const searchKeyword = ref('')

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

function selectTemplate(template: Template) {
  selectedTemplateId.value = template.id
}

async function loadTemplates() {
  loading.value = true
  try {
    templates.value = await templateApi.list()
  } catch (e: any) {
    ElMessage.error(e.message || '加载模板失败')
  } finally {
    loading.value = false
  }
}

function handleBack() { router.back() }

async function handleConfirm() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请选择模板')
    return
  }
  const scene = (route.query.scene as string) || 'campus_recruitment'
  try {
    const resume = await resumeApi.create({
      templateId: selectedTemplateId.value,
      scene,
      title: '未命名简历'
    })
    router.push(`/editor/${resume.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建简历失败')
  }
}

onMounted(() => { loadTemplates() })
</script>
