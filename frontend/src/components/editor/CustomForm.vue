<template>
  <div class="custom-form">
    <div
      v-for="(item, index) in customSections"
      :key="item.id || index"
      class="custom-section-item"
    >
      <div class="item-header">
        <h4>自定义模块 {{ index + 1 }}</h4>
        <el-button
          v-if="customSections.length > 1"
          type="danger"
          size="small"
          text
          @click="removeCustomSection(index)"
        >
          删除
        </el-button>
      </div>

      <el-form
        :model="item"
        label-width="100px"
        size="default"
      >
        <el-form-item
          label="模块标题"
          required
        >
          <el-input
            v-model="item.title"
            placeholder="请输入模块标题，例如：获奖经历、证书等"
          />
        </el-form-item>

        <el-form-item label="显示顺序">
          <el-input-number
            v-model="item.order"
            :min="0"
            :max="100"
            placeholder="顺序"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="是否显示">
          <el-switch v-model="item.visible" />
        </el-form-item>

        <el-form-item
          label="模块内容"
          required
        >
          <el-input
            v-model="item.content"
            type="textarea"
            :rows="6"
            placeholder="请输入模块内容，支持换行"
          />
        </el-form-item>
      </el-form>
    </div>

    <el-button
      type="primary"
      plain
      style="width: 100%; margin-top: 16px"
      @click="addCustomSection"
    >
      <el-icon><Plus /></el-icon>
      添加自定义模块
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { useSectionSync } from '@/composables/useSectionSync'

interface Props {
  sections: any[]
}

const props = defineProps<Props>()
const emit = defineEmits(['update'])

const customSections = ref<any[]>([])

// 从sections中提取自定义模块
function extractCustomSections() {
  if (props.sections) {
    const customSectionsList = props.sections.filter((s: any) => s.type === 'custom')
    if (customSectionsList.length > 0) {
      customSections.value = customSectionsList.map((section: any) => ({
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
    ? Math.max(...customSections.value.map(s => s.order || 0))
    : 0
  
  customSections.value.push({
    id: `custom_${Date.now()}`,
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
    let updatedSections = props.sections.filter((s: any) => s.type !== 'custom')

    // 添加新的自定义sections
    const customSectionData = customSections.value.map(item => ({
      id: item.id,
      type: 'custom',
      title: item.title,
      order: item.order,
      visible: item.visible,
      data: {
        content: item.content
      }
    }))

    updatedSections = [...updatedSections, ...customSectionData]

    emit('update', updatedSections)
  }
)
</script>

<style scoped lang="scss">
.custom-form {
  padding: 16px 0;
}

.custom-section-item {
  background: #f9fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
}

.el-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }
}
</style>
