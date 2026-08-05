<template>
  <el-dropdown
    trigger="click"
    @command="handleCommand"
    @click.stop
  >
    <el-button
      size="small"
      text
      :loading="loading"
      class="ai-writer-btn"
      @click.stop
    >
      <el-icon :size="14">
        <MagicStick />
      </el-icon>
      <span>AI</span>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="generate">
          AI 生成
        </el-dropdown-item>
        <el-dropdown-item command="polish">
          润色
        </el-dropdown-item>
        <el-dropdown-item command="shorten">
          精简
        </el-dropdown-item>
        <el-dropdown-item command="expand">
          扩写
        </el-dropdown-item>
        <el-dropdown-item
          command="translate"
          divided
        >
          翻译成英文
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    align-center
    :close-on-click-modal="false"
  >
    <el-input
      v-model="editedContent"
      type="textarea"
      :rows="8"
      :disabled="loading"
      placeholder="AI 正在生成内容…"
    />
    <div
      v-if="errorMsg"
      class="mt-2 text-xs text-error"
    >
      {{ errorMsg }}
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">
        取消
      </el-button>
      <el-button
        :loading="loading"
        @click="runAction(currentAction)"
      >
        重新生成
      </el-button>
      <el-button
        type="primary"
        :disabled="!editedContent"
        @click="applyContent"
      >
        应用到简历
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { resumeApi, type AiWritePayload } from '@/api/resume'

interface Props {
  resumeId: string
  sectionType: string
  field: string
  getOriginalText?: () => string
}

const props = defineProps<Props>()
const emit = defineEmits(['apply'])

const loading = ref(false)
const dialogVisible = ref(false)
const editedContent = ref('')
const errorMsg = ref('')
const currentAction = ref<AiWritePayload['action']>('polish')

const ACTION_LABEL: Record<string, string> = {
  generate: 'AI 生成',
  polish: '润色',
  shorten: '精简',
  expand: '扩写',
  translate: '翻译成英文'
}

const dialogTitle = computed(() => {
  const action = ACTION_LABEL[currentAction.value] || 'AI 写作'
  const field = props.field === 'description' ? '经历描述' : props.field
  return `${action} · ${field}`
})

function handleCommand(command: string) {
  runAction(command as AiWritePayload['action'])
}

async function runAction(action: AiWritePayload['action']) {
  loading.value = true
  errorMsg.value = ''
  currentAction.value = action
  try {
    const payload: AiWritePayload = {
      sectionType: props.sectionType,
      field: props.field,
      action,
      originalText: props.getOriginalText ? props.getOriginalText() : ''
    }
    if (action === 'translate') {
      payload.targetLang = 'en'
    }
    const result = await resumeApi.aiWrite(props.resumeId, payload)
    editedContent.value = result.content
    dialogVisible.value = true
  } catch (e: any) {
    errorMsg.value = e.message || 'AI 写作失败，请稍后重试'
    ElMessage.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

function applyContent() {
  if (!editedContent.value) return
  emit('apply', editedContent.value)
  dialogVisible.value = false
  ElMessage.success('已应用到简历')
}
</script>

<style scoped>
.ai-writer-btn {
  color: var(--st-primary);
}
</style>
