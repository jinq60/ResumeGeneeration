<template>
  <div class="py-24">
    <div class="max-w-[1440px] mx-auto px-margin-page">
      <div class="text-center mb-16">
        <h1 class="text-headline-md font-headline-md text-on-surface mb-4">
          模板中心
        </h1>
        <p class="text-body-md font-body-md text-on-surface-variant max-w-2xl mx-auto">
          精选多种行业风格的简历模板，适用于不同岗位与求职场景。
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-gutter">
        <div
          v-for="template in displayTemplates"
          :key="template.id"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden hover:shadow-md transition-shadow"
        >
          <div class="aspect-[3/4] bg-surface-container flex items-center justify-center overflow-hidden">
            <img
              v-if="template.thumbnailUrl"
              :src="template.thumbnailUrl"
              :alt="template.name"
              class="w-full h-full object-cover"
              loading="lazy"
              @error="onThumbnailError"
            >
            <span
              v-else
              class="text-title-md text-on-surface-variant"
            >{{ template.name }}</span>
          </div>
          <div class="p-4">
            <h3 class="text-title-md font-title-md text-on-surface mb-1">
              {{ template.name }}
            </h3>
            <p class="text-body-md text-on-surface-variant mb-4 line-clamp-2">
              {{ template.description || defaultDescription(template.category) }}
            </p>
            <button
              class="w-full py-2 rounded-lg bg-primary text-on-primary font-label-md"
              @click="goToLogin"
            >
              立即使用
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthModalStore } from '@/stores/authModal'
import { templateApi, type Template } from '@/api/template'
import { TEMPLATE_PLACEHOLDER } from '@/utils/placeholder'

const authModalStore = useAuthModalStore()

const templates = ref<Template[]>([])

const displayTemplates = computed(() => {
  const list = templates.value
  if (list.length > 0) return list.slice(0, 8)
  return Array.from({ length: 8 }, (_, i) => ({
    id: `mock-${i + 1}`,
    name: `商务风格 ${i + 1}`,
    category: 'default',
    description: '简洁专业，适合大多数岗位',
    thumbnailUrl: '',
    config: {}
  }))
})

function defaultDescription(category: string): string {
  const map: Record<string, string> = {
    campus: '清新排版，适合应届毕业生求职',
    technical: '突出技能与技术栈，适合技术岗位',
    business: '简洁专业，适合大多数岗位',
    creative: '个性鲜明，适合设计创意岗位',
    executive: '稳重大气，适合管理岗位'
  }
  return map[category] || '精选模板，助你快速完成专业简历'
}

function onThumbnailError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img && img.src !== TEMPLATE_PLACEHOLDER) {
    img.src = TEMPLATE_PLACEHOLDER
  }
}

function goToLogin() {
  authModalStore.open()
}

onMounted(async () => {
  try {
    const data = await templateApi.list()
    templates.value = data || []
  } catch {
    // 模板加载失败时展示默认占位卡片
  }
})
</script>