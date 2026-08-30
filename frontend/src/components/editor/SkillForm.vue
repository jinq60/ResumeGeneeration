<template>
  <div class="skill-form magic-form">
    <div v-for="(category, index) in skillCategories" :key="category.id || index" class="skill-category magic-item group">
      <div class="magic-drag-col" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></div>
      <div class="magic-item-main">
        <div class="category-header magic-header">
          <div class="magic-header-title">
            <h4>{{ getCategoryLabel(category.category) }}</h4>
            <span class="magic-header-sub">{{ category.items.length }} 项</span>
          </div>
          <button type="button" class="magic-ghost-btn" @click="addSkillItem(category)">
            <el-icon><Plus /></el-icon>
            添加技能
          </button>
        </div>
        <div class="magic-divider" />
        <div class="magic-skill-list">
          <div v-for="(skill, skillIndex) in category.items" :key="skill.id || skillIndex" class="skill-item magic-skill-row group/row">
            <span class="magic-drag-sm" aria-hidden="true"><el-icon :size="12"><Rank /></el-icon></span>
            <el-input v-model="skill.name" placeholder="技能名称" maxlength="64" class="magic-input magic-input-name" />
            <el-select v-model="skill.level" placeholder="熟练度" class="magic-input magic-input-level">
              <el-option label="入门" value="beginner" />
              <el-option label="熟悉" value="familiar" />
              <el-option label="熟练" value="proficient" />
              <el-option label="精通" value="expert" />
            </el-select>
            <label class="magic-switch">
              <el-switch v-model="skill.highlight" size="small" />
              <span>高亮</span>
            </label>
            <button type="button" class="magic-delete-sm" @click="removeSkillItem(category, skillIndex)">
              <el-icon :size="14"><Delete /></el-icon>
            </button>
          </div>
          <el-empty v-if="category.items.length === 0" description="暂无技能" :image-size="48" class="magic-empty" />
        </div>
      </div>
    </div>

    <button type="button" class="magic-add-btn" @click="addCategory()">
      <el-icon><Plus /></el-icon>
      添加技能分类
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Delete, Plus, Rank, View } from '@element-plus/icons-vue'
import type { Section, SkillItem } from '@/types/resume'
import { useSectionSync } from '@/composables/useSectionSync'

type SkillDraftItem = { id?: string; name: string; level?: string; highlight?: boolean }
type SkillDraftCategory = { id?: string; category: string; items: SkillDraftItem[] }

interface Props {
  sections: Section[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', sections: Section[]): void
}>()

const skillCategories = ref<SkillDraftCategory[]>([])

const CATEGORY_OPTIONS = [
  { label: '编程语言', value: 'programming_language' },
  { label: '前端', value: 'frontend' },
  { label: '后端', value: 'backend' },
  { label: '数据库', value: 'database' },
  { label: 'AI/数据', value: 'ai_data' },
  { label: '设计', value: 'design' },
  { label: '办公软件', value: 'office' },
  { label: '语言', value: 'language' },
  { label: '工具', value: 'tool' },
  { label: '其他', value: 'other' }
]

function getCategoryLabel(value: string): string {
  const option = CATEGORY_OPTIONS.find((opt) => opt.value === value)
  return option ? option.label : value
}

function nextCategoryId() {
  return `skill_cat_${Date.now()}_${Math.floor(Math.random() * 1000)}`
}

function nextSkillId() {
  return `skill_${Date.now()}_${Math.floor(Math.random() * 1000)}`
}

// 从sections中提取技能
function extractSkills() {
  if (props.sections) {
    const skillSection = props.sections.find((s) => s.type === 'skill')
    if (skillSection && Array.isArray(skillSection.data)) {
      skillCategories.value = (skillSection.data as SkillItem[]).map((item) => ({
        id: item.id || nextCategoryId(),
        category: item.category,
        items: (item.items || []).map((skill) => ({
          id: skill.name ? `skill_${skill.name}_${Math.random().toString(36).slice(2, 7)}` : nextSkillId(),
          name: skill.name,
          level: skill.level,
          highlight: skill.highlight
        }))
      }))
    }
  }

  // 如果没有技能，添加默认分类
  if (skillCategories.value.length === 0) {
    addCategory('programming_language')
  }
}

function addCategory(categoryValue?: string) {
  const category = categoryValue || 'other'
  skillCategories.value.push({
    id: nextCategoryId(),
    category,
    items: []
  })
}

function addSkillItem(category: SkillDraftCategory) {
  category.items.push({
    id: nextSkillId(),
    name: '',
    level: 'familiar',
    highlight: false
  })
}

function removeSkillItem(category: SkillDraftCategory, skillIndex: number) {
  category.items.splice(skillIndex, 1)
}

// 初始化
extractSkills()

// 与父组件 sections 双向同步：外部变更时重新提取，自身 emit 的回传自动忽略
useSectionSync(
  skillCategories,
  () => props.sections,
  extractSkills,
  () => {
    const skillData: SkillItem[] = skillCategories.value
      .filter((cat) => cat.items.length > 0)
      .map((cat) => ({
        id: cat.id,
        category: cat.category,
        items: cat.items.map((skill) => ({
          name: skill.name,
          level: skill.level,
          highlight: skill.highlight
        }))
      }))

    emit('update', props.sections.map((section) => {
      if (section.type === 'skill') {
        return {
          ...section,
          data: skillData
        }
      }
      return section
    }))
  },
  () => props.sections?.find((s) => s.type === 'skill')?.data
)
</script>

<style scoped lang="scss">
.skill-form.magic-form { display: flex; flex-direction: column; gap: 12px; padding: 4px 0 8px; }
.skill-category.magic-item { display: flex; overflow: hidden; background: hsl(var(--st-card)); border: 1px solid hsl(var(--st-border)); border-radius: var(--st-radius-lg); box-shadow: var(--st-shadow-sm); transition: border-color 160ms ease, box-shadow 160ms ease; }
.skill-category.magic-item:hover { border-color: hsl(var(--st-foreground) / 0.14); box-shadow: var(--st-shadow-md); }
.magic-drag-col { width: 44px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border-right: 1px solid hsl(var(--st-border)); cursor: grab; color: hsl(var(--st-muted)); }
.skill-category:hover .magic-drag-col { background: hsl(var(--st-secondary) / 0.5); color: hsl(var(--st-foreground) / 0.7); }
.magic-drag-icon { font-size: 14px; }
.magic-item-main { flex: 1; min-width: 0; }
.magic-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 16px; }
.magic-header-title { display: flex; align-items: baseline; gap: 8px; }
.magic-header-title h4 { margin: 0; font-size: 13.5px; font-weight: 600; color: hsl(var(--st-foreground)); }
.magic-header-sub { font-size: 11px; color: hsl(var(--st-muted-foreground)); background: hsl(var(--st-secondary)); padding: 2px 6px; border-radius: 9999px; }
.magic-ghost-btn { display: inline-flex; align-items: center; gap: 6px; height: 30px; padding: 0 12px; background: transparent; border: 1px solid hsl(var(--st-border)); border-radius: 9999px; font-size: 12px; font-weight: 500; color: hsl(var(--st-foreground)); cursor: pointer; transition: background 160ms ease; }
.magic-ghost-btn:hover { background: hsl(var(--st-secondary)); }
.magic-divider { height: 1px; background: hsl(var(--st-border)); margin: 0 16px; }
.magic-skill-list { padding: 12px 16px; display: flex; flex-direction: column; gap: 10px; }
.magic-skill-row { display: grid; grid-template-columns: 16px minmax(0, 1.4fr) 120px auto 28px; align-items: center; gap: 10px; padding: 8px 10px 8px 6px; background: hsl(var(--st-background) / 0.6); border: 1px solid hsl(var(--st-border) / 0.6); border-radius: 0.625rem; transition: border-color 160ms ease, background 160ms ease; }
.magic-skill-row:hover { background: hsl(var(--st-card)); border-color: hsl(var(--st-border)); box-shadow: var(--st-shadow-xs); }
.magic-drag-sm { width: 16px; height: 22px; display: inline-flex; align-items: center; justify-content: center; color: hsl(var(--st-muted)); opacity: 0.6; cursor: grab; }
.magic-skill-row:hover .magic-drag-sm { opacity: 1; }
.magic-input :deep(.el-input__wrapper), .magic-input :deep(.el-select .el-input__wrapper) { min-height: 36px; height: 36px; background: hsl(var(--st-background)); border-radius: 0.5rem; box-shadow: 0 0 0 1px hsl(var(--st-input)) inset, var(--st-shadow-xs); }
.magic-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset; background: hsl(var(--st-card)); }
.magic-input :deep(.el-input__inner)::placeholder { color: hsl(var(--st-muted)); }
.magic-input-name { min-width: 0; }
.magic-input-level { width: 120px; }
.magic-switch { display: inline-flex; align-items: center; gap: 6px; font-size: 11px; color: hsl(var(--st-muted-foreground)); white-space: nowrap; }
.magic-switch :deep(.el-switch.is-checked .el-switch__core) { background-color: hsl(var(--st-foreground)); border-color: hsl(var(--st-foreground)); }
.magic-delete-sm { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; border-radius: 9999px; border: 1px solid transparent; background: transparent; color: hsl(var(--st-muted-foreground)); cursor: pointer; transition: all 160ms ease; }
.magic-delete-sm:hover { background: hsl(0 84% 97%); color: #dc2626; border-color: hsl(0 84% 88%); }
.magic-empty :deep(.el-empty__description) p { font-size: 12px; color: hsl(var(--st-muted-foreground)); }
.magic-add-btn { width: 100%; height: 42px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; background: hsl(var(--st-primary)); color: hsl(var(--st-primary-foreground)); border: 0; border-radius: 9999px; font-size: 13.5px; font-weight: 500; cursor: pointer; box-shadow: var(--st-shadow-sm); }
.magic-add-btn:hover { opacity: 0.92; }
@media (max-width: 640px) { .magic-skill-row { grid-template-columns: 16px 1fr auto 28px; } .magic-input-level { width: 100%; } .magic-switch { grid-column: 2 / 4; } }
</style>
