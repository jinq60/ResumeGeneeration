<template>
  <aside class="module-rail" aria-label="简历模块">
    <section class="rail-group">
      <div class="rail-group-title"><el-icon><Grid /></el-icon><span>布局</span></div>
      <button class="profile-layout-card is-selected" type="button" @click="emit('open-settings')">
        <el-icon><User /></el-icon><span>基本信息</span>
      </button>
    </section>

    <section class="rail-group module-group">
      <div class="rail-group-title"><el-icon><CollectionTag /></el-icon><span>模块</span></div>
      <button
        v-for="section in sections"
        :key="section.id"
        :data-section-id="section.id"
        class="module-card"
        :class="{ 'is-active': activeSectionId === section.id, 'is-hidden': !section.visible }"
        type="button"
        draggable="true"
        @click="emit('select-section', section.id)"
        @dragstart="onDragStart(section.id, $event)"
        @dragover.prevent
        @drop="onDrop(section.id)"
      >
        <span class="drag-handle" aria-hidden="true">⠿</span>
        <el-icon><component :is="sectionIcon(section.type)" /></el-icon>
        <span class="module-title">{{ section.title || typeLabel(section.type) }}</span>
        <span class="module-actions" @click.stop>
          <button
            v-if="section.type !== 'profile'"
            class="module-action"
            type="button"
            :aria-label="`${section.visible ? '隐藏' : '显示'}${section.title || typeLabel(section.type)}模块`"
            @click="emit('toggle-visibility', section.id, !section.visible)"
          >
            <el-icon><View v-if="section.visible" /><Hide v-else /></el-icon>
          </button>
          <button
            v-if="section.type !== 'profile'"
            class="module-action is-danger"
            type="button"
            :aria-label="`删除${section.title || typeLabel(section.type)}模块`"
            @click="emit('remove-section', section.id)"
          >
            <el-icon><Delete /></el-icon>
          </button>
        </span>
      </button>
      <button class="add-module-button" type="button" aria-label="添加自定义模块" @click="emit('add-custom-section')"><el-icon><Plus /></el-icon>添加模块</button>
    </section>

    <section class="rail-group appearance-group">
      <div class="rail-group-title"><el-icon><Brush /></el-icon><span>主题色</span></div>
      <div class="theme-swatches" aria-label="主题色">
        <button
          v-for="color in themeColors"
          :key="color"
          :class="{ 'is-selected': themeColor === color }"
          :style="{ backgroundColor: color }"
          type="button"
          :aria-label="`使用${color === '#1557b0' ? '蓝色' : '主题'}主题`"
          @click="emit('select-theme', color)"
        />
      </div>
    </section>

    <section class="rail-group typography-group">
      <div class="rail-group-title"><el-icon><EditPen /></el-icon><span>排版</span></div>
      <button class="typography-button" type="button" aria-label="打开排版设置" @click="emit('open-settings')">字体、字号与边距<el-icon><ArrowRight /></el-icon></button>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowRight, Briefcase, Brush, CollectionTag, Delete, DocumentChecked, EditPen, Grid, Hide, Menu, Plus, School, User, View } from '@element-plus/icons-vue'
import type { Section, SectionType } from '@/types/resume'

const props = defineProps<{ sections: Section[]; activeSectionId: string; themeColor: string }>()
const emit = defineEmits<{
  'select-section': [id: string]
  'toggle-visibility': [id: string, visible: boolean]
  'remove-section': [id: string]
  'reorder-sections': [orderedIds: string[]]
  'add-custom-section': []
  'select-theme': [color: string]
  'open-settings': []
}>()

const draggingId = ref<string | null>(null)
const themeColors = ['#20201d', '#4b4b48', '#7b7b74', '#a6a69f', '#1557b0', '#a50e13']
const labels: Record<SectionType, string> = { profile: '基本信息', education: '教育经历', work: '工作经验', project: '项目经历', skill: '专业技能', introduction: '自我介绍', custom: '自定义模块' }
const icons: Record<SectionType, typeof User> = { profile: User, education: School, work: Briefcase, project: DocumentChecked, skill: Menu, introduction: EditPen, custom: CollectionTag }
const typeLabel = (type: SectionType) => labels[type]
const sectionIcon = (type: SectionType) => icons[type]

function onDragStart(id: string, event: DragEvent) {
  draggingId.value = id
  event.dataTransfer?.setData('text/plain', id)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function onDrop(targetId: string) {
  const sourceId = draggingId.value
  draggingId.value = null
  if (!sourceId || sourceId === targetId) return
  const ids = props.sections.filter(section => section.type !== 'profile').map(section => section.id)
  const sourceIndex = ids.indexOf(sourceId)
  const targetIndex = ids.indexOf(targetId)
  if (sourceIndex < 0 || targetIndex < 0) return
  ids.splice(sourceIndex, 1)
  ids.splice(targetIndex, 0, sourceId)
  emit('reorder-sections', ids)
}
</script>

<style scoped lang="scss">
.module-rail{width:240px;flex:0 0 240px;overflow:auto;padding:16px 12px;border-right:1px solid rgba(0,0,0,0.08);background:#FFFFFF}.rail-group{margin-bottom:24px}.rail-group-title{display:flex;align-items:center;gap:8px;margin:0 4px 12px;font-size:12px;font-weight:600;letter-spacing:0.04em;text-transform:uppercase;color:#999999}.profile-layout-card,.module-card,.typography-button{width:100%;border:1px solid rgba(0,0,0,0.08);background:#FFFFFF;border-radius:10px;color:#171717;transition:background 160ms ease,border-color 160ms ease}.profile-layout-card{display:flex;align-items:center;justify-content:center;gap:8px;height:48px;border:1px solid rgba(0,0,0,0.08);font-weight:500;font-size:14px;background:#F7F7F5}.profile-layout-card:hover{border-color:rgba(0,0,0,0.12);background:#FFFFFF}.module-group{display:flex;flex-direction:column;gap:4px}.module-card{display:flex;align-items:center;gap:10px;height:42px;min-height:42px;padding:0 10px;text-align:left;cursor:pointer}.module-card:hover{background:rgba(0,0,0,0.04);border-color:rgba(0,0,0,0.08)}.module-card.is-active{background:rgba(0,0,0,0.06);border-color:rgba(0,0,0,0.08);font-weight:500}.module-card.is-hidden{opacity:0.5}.drag-handle{color:#999999;font-size:14px;line-height:1;opacity:0;transition:opacity 120ms}.module-card:hover .drag-handle{opacity:1}.module-title{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:14px;font-weight:440}.module-actions{display:flex;gap:2px;opacity:0;transition:opacity 120ms}.module-card:hover .module-actions{opacity:1}.module-action{display:grid;place-items:center;width:26px;height:26px;border:0;border-radius:8px;background:transparent;color:#999999;cursor:pointer;transition:background 120ms,color 120ms}.module-action:hover{background:rgba(0,0,0,0.06);color:#171717}.module-action.is-danger:hover{color:#D92D20;background:rgba(217,45,32,0.08)}.add-module-button{height:40px;border:1px dashed rgba(0,0,0,0.12);border-radius:10px;background:#FFFFFF;color:#666666;font-weight:500;font-size:13px;cursor:pointer;transition:border-color 160ms,background 160ms}.add-module-button:hover{border-color:rgba(0,0,0,0.20);background:rgba(0,0,0,0.02)}.theme-swatches{display:flex;flex-wrap:wrap;gap:8px}.theme-swatches button{width:24px;height:24px;border:2px solid transparent;border-radius:50%;cursor:pointer;transition:transform 120ms}.theme-swatches button:hover{transform:scale(1.08)}.theme-swatches button.is-selected{outline:2px solid #171717;outline-offset:2px}.typography-button{display:flex;align-items:center;justify-content:space-between;padding:12px 14px;font-size:13px;color:#666666;cursor:pointer}.typography-button:hover{background:rgba(0,0,0,0.04);color:#171717}
@media (max-width: 1440px){.module-rail{width:240px;flex-basis:240px;padding:14px 10px}.module-card{height:42px;min-height:42px}.rail-group{margin-bottom:20px}}
</style>
