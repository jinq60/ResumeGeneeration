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
// 1:1 对齐 magic-resume THEME_COLORS（12 色，含黑灰蓝红橙紫绿）
const themeColors = [
  '#000000', '#1A1A1A', '#333333', '#4D4D4D',
  '#666666', '#808080', '#999999', '#0047AB',
  '#8B0000', '#FF4500', '#4B0082', '#2E8B57'
]
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
.module-rail {
  width: 280px;
  flex: 0 0 280px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px;
  background: hsl(48 20% 97%);
  border-right: 1px solid hsl(48 10% 89%);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.rail-group {
  background: hsl(48 20% 97%);
  border: 1px solid hsl(48 10% 89%);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(27,27,24,0.05);
  padding: 16px;
}
.rail-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(60 5% 10%);
}
.rail-group-title .el-icon { color: hsl(60 2% 56%); }
.profile-layout-card,
.module-card,
.typography-button {
  width: 100%;
  border: 1px solid hsl(48 10% 89%);
  background: hsl(48 20% 97%);
  border-radius: 10px;
  color: hsl(60 5% 10%);
  transition: all 160ms ease;
}
.profile-layout-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 44px;
  font-weight: 500;
  font-size: 14px;
  background: white;
  border: 2px solid hsl(60 5% 10%);
}
.profile-layout-card:hover { background: hsl(48 18% 90%); }
.module-group { display: flex; flex-direction: column; gap: 6px; }
.module-card {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  min-height: 44px;
  padding: 0 12px;
  text-align: left;
  cursor: pointer;
  background: white;
  box-shadow: 0 1px 2px rgba(27,27,24,0.04);
}
.module-card:hover {
  background: hsl(48 10% 92%);
  border-color: hsl(48 10% 89%);
  transform: scale(1.01);
}
.module-card.is-active {
  background: white;
  border-color: hsl(60 5% 10%);
  border-width: 2px;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(27,27,24,0.08);
}
.module-card.is-hidden { opacity: 0.6; }
.drag-handle { color: hsl(60 2% 56%); font-size: 14px; opacity: 0; transition: opacity 120ms; }
.module-card:hover .drag-handle { opacity: 1; }
.module-title {
  min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-size: 14px; font-weight: 450;
}
.module-actions { display: flex; gap: 2px; opacity: 0; transition: opacity 120ms; }
.module-card:hover .module-actions { opacity: 1; }
.module-action {
  display: grid; place-items: center; width: 22px; height: 22px;
  border: 0; border-radius: 6px; background: transparent;
  color: hsl(60 2% 56%); cursor: pointer; transition: all 120ms;
}
.module-action:hover { background: hsl(48 10% 92%); color: hsl(60 5% 10%); }
.module-action.is-danger:hover { color: hsl(0 84.2% 60.2%); background: hsl(0 84.2% 95%); }
.add-module-button {
  height: 40px; border: 1px dashed hsl(60 5% 10% / 0.2); border-radius: 10px;
  background: white; color: hsl(60 5% 10%); font-weight: 500; font-size: 13px;
  cursor: pointer; transition: all 160ms; display: flex; align-items: center; justify-content: center; gap: 6px;
}
.add-module-button:hover { border-color: hsl(60 5% 10% / 0.4); background: hsl(48 10% 92%); }
.appearance-group .theme-swatches { display: flex; flex-wrap: wrap; gap: 10px; padding-top: 4px; }
.theme-swatches button {
  width: 24px; height: 24px; border-radius: 50%; border: 1px solid hsl(48 10% 89%);
  cursor: pointer; transition: all 200ms; position: relative; overflow: hidden;
}
.theme-swatches button:hover { transform: scale(1.1); border-color: hsl(60 5% 10% / 0.5); }
.theme-swatches button.is-selected {
  box-shadow: 0 0 0 2px white, 0 0 0 4px hsl(60 5% 10%);
  transform: scale(1.08);
}
.typography-group .typography-button {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; font-size: 13px; color: hsl(60 2% 40%); cursor: pointer;
  background: white;
}
.typography-group .typography-button:hover { background: hsl(48 10% 92%); color: hsl(60 5% 10%); }
@media (max-width: 1440px){
  .module-rail{ width: 260px; flex-basis: 260px; padding: 14px; }
}
</style>
