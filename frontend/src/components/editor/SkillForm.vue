<template>
  <div class="skill-form">
    <div
      v-for="(category, index) in skillCategories"
      :key="index"
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
        :key="skillIndex"
        class="skill-item"
      >
        <el-input
          v-model="skill.name"
          placeholder="技能名称"
          style="width: 200px"
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
import type { SkillItem } from '@/types/resume'
import { useSectionSync } from '@/composables/useSectionSync'

interface Props {
  sections: any[]
}

const props = defineProps<Props>()
const emit = defineEmits(['update'])

const skillCategories = ref<any[]>([])

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
  const option = CATEGORY_OPTIONS.find(opt => opt.value === value)
  return option ? option.label : value
}

// 从sections中提取技能
function extractSkills() {
  if (props.sections) {
    const skillSection = props.sections.find((s: any) => s.type === 'skill')
    if (skillSection && Array.isArray(skillSection.data)) {
      skillCategories.value = skillSection.data.map((item: SkillItem) => ({
        category: item.category,
        items: item.items || []
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
    category,
    items: []
  })
}

function addSkillItem(category: any) {
  category.items.push({
    name: '',
    level: 'familiar',
    highlight: false
  })
}

function removeSkillItem(category: any, skillIndex: number) {
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
    const skillData = skillCategories.value
      .filter(cat => cat.items.length > 0)
      .map(cat => ({
        category: cat.category,
        items: cat.items
      }))

    emit('update', props.sections.map((section: any) => {
      if (section.type === 'skill') {
        return {
          ...section,
          data: skillData
        }
      }
      return section
    }))
  }
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
