<template>
  <div class="skill-form">
    <div
      v-for="(category, index) in skillCategories"
      :key="category.id || index"
      class="skill-category"
    >
      <div class="category-header">
        <h4>{{ getCategoryLabel(category.category) }}</h4>
        <el-button
          type="primary"
          size="small"
          text
          @click="addSkillItem(category)"
        >
          <el-icon><Plus /></el-icon>
          添加技能
        </el-button>
      </div>

      <div
        v-for="(skill, skillIndex) in category.items"
        :key="skill.id || skillIndex"
        class="skill-item"
      >
        <el-input
          v-model="skill.name"
          placeholder="技能名称"
          style="width: 200px"
          maxlength="64"
        />
        <el-select
          v-model="skill.level"
          placeholder="熟练度"
          style="width: 120px"
        >
          <el-option
            label="入门"
            value="beginner"
          />
          <el-option
            label="熟悉"
            value="familiar"
          />
          <el-option
            label="熟练"
            value="proficient"
          />
          <el-option
            label="精通"
            value="expert"
          />
        </el-select>
        <el-switch
          v-model="skill.highlight"
          active-text="高亮"
        />
        <el-button
          type="danger"
          size="small"
          text
          @click="removeSkillItem(category, skillIndex)"
        >
          删除
        </el-button>
      </div>

      <el-empty
        v-if="category.items.length === 0"
        description="暂无技能"
        :image-size="60"
      />
    </div>

    <el-button
      type="primary"
      plain
      style="width: 100%; margin-top: 16px"
      @click="addCategory()"
    >
      <el-icon><Plus /></el-icon>
      添加技能分类
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
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
.skill-form {
  padding: 16px 0;
}

.skill-category {
  background: #f9fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
}

.skill-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}
</style>
