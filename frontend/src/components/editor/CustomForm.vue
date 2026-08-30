<template>
  <div class="custom-form magic-form">
    <div v-for="(item, index) in customSections" :key="item.id || index" class="custom-section-item magic-item group">
      <div class="magic-drag-col" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></div>
      <div class="magic-item-main">
        <div class="magic-item-header">
          <div class="magic-item-title">
            <h4>{{ item.title || `自定义模块 ${index + 1}` }}</h4>
            <p v-if="item.content" class="magic-item-sub">{{ item.content.slice(0, 32) }}{{ item.content.length > 32 ? '…' : '' }}</p>
          </div>
          <div class="magic-item-actions">
            <button type="button" class="magic-action-sm" title="显示/隐藏" @click="item.visible = !item.visible">
              <el-icon :size="14"><View v-if="item.visible" /><Delete v-else /></el-icon>
            </button>
            <button v-if="customSections.length > 1" type="button" class="magic-delete-sm" title="删除" @click="removeCustomSection(index)">
              <el-icon :size="14"><Delete /></el-icon>
            </button>
            <span class="magic-chevron"><el-icon :size="14"><ArrowDown /></el-icon></span>
          </div>
        </div>
        <div class="magic-divider" />
        <el-form :model="item" label-width="84px" size="default" class="magic-form-grid">
          <el-form-item label="模块标题" required class="magic-field">
            <el-input v-model="item.title" placeholder="请输入模块标题，例如：获奖经历、证书等" class="magic-input" />
          </el-form-item>
          <div class="magic-grid-2">
            <el-form-item label="显示顺序" class="magic-field">
              <el-input-number v-model="item.order" :min="0" :max="100" placeholder="顺序" style="width: 100%" class="magic-input magic-number" controls-position="right" />
            </el-form-item>
            <el-form-item label="是否显示" class="magic-field">
              <el-switch v-model="item.visible" />
            </el-form-item>
          </div>
          <el-form-item label="模块内容" required class="magic-field magic-field-editor">
            <el-input v-model="item.content" type="textarea" :rows="6" placeholder="请输入模块内容，支持换行" class="magic-input magic-textarea" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <button type="button" class="magic-add-btn" @click="addCustomSection">
      <el-icon><Plus /></el-icon>
      添加自定义模块
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowDown, Delete, Plus, Rank, View } from '@element-plus/icons-vue'
import { useSectionSync } from '@/composables/useSectionSync'
import type { Section } from '@/types/resume'

type CustomDraft = {
  id: string
  title: string
  order: number
  visible: boolean
  content: string
}

interface Props {
  sections: Section[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', sections: Section[]): void
}>()

const customSections = ref<CustomDraft[]>([])

function nextCustomId() {
  return `custom_${Date.now()}_${Math.floor(Math.random() * 1000)}`
}

// 从sections中提取自定义模块
function extractCustomSections() {
  if (props.sections) {
    const customSectionsList = props.sections.filter((s) => s.type === 'custom')
    if (customSectionsList.length > 0) {
      customSections.value = customSectionsList.map((section) => ({
        id: section.id,
        title: section.title,
        order: section.order,
        visible: section.visible,
        content: section.data?.content || ''
      }))
    }
  }

  // 如果没有自定义模块，添加一个默认的
  if (customSections.value.length === 0) {
    addCustomSection()
  }
}

function addCustomSection() {
  const maxOrder = customSections.value.length > 0
    ? Math.max(...customSections.value.map((s) => s.order || 0))
    : 0

  customSections.value.push({
    id: nextCustomId(),
    title: '',
    order: maxOrder + 1,
    visible: true,
    content: ''
  })
}

function removeCustomSection(index: number) {
  customSections.value.splice(index, 1)
}

// 初始化
extractCustomSections()

// 与父组件 sections 双向同步：外部变更时重新提取，自身 emit 的回传自动忽略
useSectionSync(
  customSections,
  () => props.sections,
  extractCustomSections,
  () => {
    // 先移除所有自定义section
    const nonCustom = props.sections.filter((s) => s.type !== 'custom')

    // 添加新的自定义sections
    const customSectionData: Section[] = customSections.value.map((item) => ({
      id: item.id,
      type: 'custom',
      title: item.title,
      order: item.order,
      visible: item.visible,
      data: { content: item.content }
    }))

    emit('update', [...nonCustom, ...customSectionData])
  },
  () => props.sections.filter(s => s.type === 'custom').map(s => ({ id: s.id, title: s.title, order: s.order, visible: s.visible, content: (s.data as any)?.content }))
)
</script>

<style scoped lang="scss">
.custom-form.magic-form { display: flex; flex-direction: column; gap: 12px; padding: 4px 0 8px; }
.magic-item { display: flex; overflow: hidden; background: hsl(var(--st-card)); border: 1px solid hsl(var(--st-border)); border-radius: var(--st-radius-lg); box-shadow: var(--st-shadow-sm); transition: border-color 160ms ease, box-shadow 160ms ease; }
.magic-item:hover { border-color: hsl(var(--st-foreground) / 0.14); box-shadow: var(--st-shadow-md); }
.magic-drag-col { width: 44px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border-right: 1px solid hsl(var(--st-border)); cursor: grab; color: hsl(var(--st-muted)); transition: background 160ms ease, color 160ms ease; }
.magic-item:hover .magic-drag-col { background: hsl(var(--st-secondary) / 0.5); color: hsl(var(--st-foreground) / 0.7); }
.magic-drag-icon { font-size: 14px; }
.magic-item-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.magic-item-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 16px; }
.magic-item-title h4 { margin: 0; font-size: 14px; font-weight: 600; color: hsl(var(--st-foreground)); }
.magic-item-sub { margin: 2px 0 0; font-size: 12px; color: hsl(var(--st-muted-foreground)); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 240px; }
.magic-item-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.magic-action-sm, .magic-delete-sm { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; border-radius: 9999px; border: 1px solid transparent; background: transparent; cursor: pointer; color: hsl(var(--st-muted-foreground)); transition: all 160ms ease; }
.magic-action-sm:hover { background: hsl(var(--st-secondary)); color: hsl(var(--st-foreground)); border-color: hsl(var(--st-border)); }
.magic-delete-sm:hover { background: hsl(0 84% 97%); color: #dc2626; border-color: hsl(0 84% 88%); }
.magic-chevron { width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; color: hsl(var(--st-muted)); }
.magic-divider { height: 1px; background: hsl(var(--st-border)); margin: 0 16px; }
.magic-form-grid { padding: 16px; }
.magic-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 16px; }
@media (max-width: 640px) { .magic-grid-2 { grid-template-columns: 1fr; } }
.magic-field { margin-bottom: 10px !important; }
.magic-field :deep(.el-form-item__label) { font-size: 12.5px; font-weight: 500; color: hsl(var(--st-foreground)); }
.magic-input :deep(.el-input__wrapper), .magic-input :deep(.el-select .el-input__wrapper) { min-height: 36px; height: 36px; background: hsl(var(--st-background)); border-radius: 0.5rem; box-shadow: 0 0 0 1px hsl(var(--st-input)) inset, var(--st-shadow-xs); }
.magic-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset; background: hsl(var(--st-card)); }
.magic-input :deep(.el-input__inner)::placeholder, .magic-input :deep(.el-textarea__inner)::placeholder { color: hsl(var(--st-muted)); }
.magic-textarea :deep(.el-textarea__inner) { background: hsl(var(--st-background)); border-radius: 0.5rem; box-shadow: 0 0 0 1px hsl(var(--st-input)) inset; padding: 10px 12px; font-size: 13px; color: hsl(var(--st-foreground)); min-height: 110px; }
.magic-textarea :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset; background: hsl(var(--st-card)); }
.magic-number :deep(.el-input-number__increase), .magic-number :deep(.el-input-number__decrease) { background: hsl(var(--st-secondary)); border-color: hsl(var(--st-border)); }
.magic-add-btn { width: 100%; height: 42px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; background: hsl(var(--st-primary)); color: hsl(var(--st-primary-foreground)); border: 0; border-radius: 9999px; font-size: 13.5px; font-weight: 500; cursor: pointer; box-shadow: var(--st-shadow-sm); }
.magic-add-btn:hover { opacity: 0.92; }
</style>
