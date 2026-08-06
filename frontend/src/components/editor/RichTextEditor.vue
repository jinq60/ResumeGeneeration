<template>
  <div class="rich-text-editor">
    <div
      class="rich-text-toolbar"
      role="toolbar"
      aria-label="文本格式"
    >
      <button
        v-for="tool in tools"
        :key="tool.command"
        type="button"
        :title="tool.label"
        :aria-label="tool.label"
        @mousedown.prevent="executeCommand(tool.command)"
      >
        <strong v-if="tool.command === 'bold'">B</strong>
        <em v-else-if="tool.command === 'italic'">I</em>
        <u v-else-if="tool.command === 'underline'">U</u>
        <span v-else-if="tool.command === 'insertUnorderedList'">•</span>
        <span v-else-if="tool.command === 'insertOrderedList'">1.</span>
        <span v-else>Tx</span>
      </button>
      <span class="rich-text-hint">支持加粗、斜体、下划线和列表</span>
    </div>
    <div
      ref="editorRef"
      class="rich-text-content"
      :class="{ 'is-focused': focused }"
      contenteditable="true"
      role="textbox"
      aria-multiline="true"
      :data-placeholder="placeholder"
      @focus="focused = true"
      @blur="focused = false"
      @input="emitValue"
      @paste="handlePaste"
    />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { sanitizeRichText } from '@/utils/richText'

interface Props {
  modelValue: string
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入内容'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorRef = ref<HTMLElement | null>(null)
const focused = ref(false)
const tools = [
  { command: 'bold', label: '加粗' },
  { command: 'italic', label: '斜体' },
  { command: 'underline', label: '下划线' },
  { command: 'insertUnorderedList', label: '项目列表' },
  { command: 'insertOrderedList', label: '编号列表' },
  { command: 'removeFormat', label: '清除格式' }
]

function syncEditorValue() {
  if (!editorRef.value || focused.value) return
  const sanitized = sanitizeRichText(props.modelValue)
  if (editorRef.value.innerHTML !== sanitized) {
    editorRef.value.innerHTML = sanitized
  }
}

function emitValue() {
  if (!editorRef.value) return
  const sanitized = sanitizeRichText(editorRef.value.innerHTML)
  if (editorRef.value.innerHTML !== sanitized) {
    editorRef.value.innerHTML = sanitized
  }
  emit('update:modelValue', sanitized)
}

function executeCommand(command: string) {
  editorRef.value?.focus()
  document.execCommand(command, false)
  emitValue()
}

function handlePaste(event: ClipboardEvent) {
  event.preventDefault()
  const text = event.clipboardData?.getData('text/plain') || ''
  document.execCommand('insertText', false, text)
  emitValue()
}

watch(() => props.modelValue, syncEditorValue)

onMounted(async () => {
  await nextTick()
  syncEditorValue()
})
</script>

<style scoped lang="scss">
.rich-text-editor {
  overflow: hidden;
  border: 1px solid var(--st-outline-variant);
  border-radius: var(--st-radius-md);
  background: var(--st-surface-bright);
}

.rich-text-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--st-outline-variant);
  background: var(--st-surface-container-low);
}

.rich-text-toolbar button {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  color: var(--st-on-surface-variant);
  font-size: 13px;
}

.rich-text-toolbar button:hover {
  color: var(--st-primary);
  background: var(--st-primary-container);
}

.rich-text-hint {
  margin-left: auto;
  color: var(--st-outline);
  font-size: 11px;
  white-space: nowrap;
}

.rich-text-content {
  min-height: 150px;
  padding: 12px;
  outline: none;
  color: var(--st-on-surface);
  font-size: 14px;
  line-height: 1.7;
}

.rich-text-content:empty::before {
  content: attr(data-placeholder);
  color: var(--st-outline);
  pointer-events: none;
}

.rich-text-content.is-focused {
  box-shadow: inset 0 0 0 2px var(--st-primary-fixed-dim);
}

.rich-text-content :deep(p) {
  margin: 0 0 8px;
}

.rich-text-content :deep(ul),
.rich-text-content :deep(ol) {
  padding-left: 22px;
  margin: 6px 0;
}

@media (max-width: 768px) {
  .rich-text-hint {
    display: none;
  }
}
</style>
