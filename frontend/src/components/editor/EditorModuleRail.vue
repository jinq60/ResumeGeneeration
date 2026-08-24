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
.module-rail{width:292px;flex:0 0 292px;overflow:auto;padding:18px;border-right:1px solid #e8e7e2;background:#fffdf9}.rail-group{margin-bottom:22px}.rail-group-title{display:flex;align-items:center;gap:8px;margin:0 2px 10px;font-size:14px;font-weight:700;color:#33312d}.profile-layout-card,.module-card,.typography-button{width:100%;border:1px solid #e5e3de;background:#fff;border-radius:12px;color:#26241f}.profile-layout-card{display:flex;align-items:center;justify-content:center;gap:10px;height:54px;border:2px solid #282720;font-weight:650}.module-group{display:flex;flex-direction:column;gap:9px}.module-card{display:flex;align-items:center;gap:9px;min-height:52px;padding:9px 10px;text-align:left;cursor:pointer}.module-card:hover,.module-card.is-active{border-color:#24231f;box-shadow:0 0 0 1px #24231f;background:#f8f7f2}.module-card.is-hidden{opacity:.58}.drag-handle{color:#928f88;font-size:17px;line-height:1}.module-title{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:14px}.module-actions{display:flex;gap:2px}.module-action{display:grid;place-items:center;width:26px;height:26px;border:0;border-radius:6px;background:transparent;color:#595750;cursor:pointer}.module-action:hover{background:#eceae3}.module-action.is-danger{color:#e55454}.add-module-button{height:42px;border:1px dashed #c7c4bb;border-radius:10px;background:#f7f6f2;color:#34312c;font-weight:600;cursor:pointer}.theme-swatches{display:flex;flex-wrap:wrap;gap:9px}.theme-swatches button{width:25px;height:25px;border:2px solid transparent;border-radius:50%;cursor:pointer}.theme-swatches button.is-selected{outline:2px solid #24231f;outline-offset:2px}.typography-button{display:flex;align-items:center;justify-content:space-between;padding:12px;font-size:13px;cursor:pointer}.typography-button:hover{background:#f8f7f2}
@media (max-width: 1440px){.module-rail{width:248px;flex-basis:248px;padding:14px}.module-card{min-height:46px;padding:7px 8px}.module-title{font-size:13px}.rail-group{margin-bottom:16px}}
</style>
