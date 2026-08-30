<template>
  <el-form ref="formRef" :model="formData" :rules="fieldRules" class="profile-form magic-form" label-position="top">
    <!-- 基础字段：对齐 BasicPanel renderBasicField — drag handle 16px / icon 14px / label 80px / input flex1 h-9 / eye 22px / delete 22px -->
    <section class="magic-section">
      <div class="magic-section-head">
        <h3 class="magic-heading">基本信息</h3>
        <span class="magic-heading-sub">拖拽可排序 · 眼睛控制显示</span>
      </div>
      <div class="profile-fields magic-field-list">
        <el-form-item v-if="isFieldVisible('name')" prop="name" class="profile-field-row magic-field-row group">
          <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
          <span class="magic-icon"><el-icon :size="14"><User /></el-icon></span>
          <span class="field-label magic-label">姓名</span>
          <el-input v-model="formData.name" class="field-control magic-input" placeholder="请输入姓名" />
          <button type="button" class="magic-action" title="显示/隐藏" @click="toggleField('name')"><el-icon :size="14"><View v-if="isFieldVisible('name')" /><Hide v-else /></el-icon></button>
          <span class="magic-delete-placeholder" />
        </el-form-item>
        <el-form-item v-if="isFieldVisible('targetPosition')" prop="targetPosition" class="profile-field-row magic-field-row group">
          <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
          <span class="magic-icon"><el-icon :size="14"><Briefcase /></el-icon></span>
          <span class="field-label magic-label">职位</span>
          <el-input v-model="formData.targetPosition" class="field-control magic-input" placeholder="请输入目标岗位" maxlength="128" show-word-limit />
          <button type="button" class="magic-action" title="显示/隐藏" @click="toggleField('targetPosition')"><el-icon :size="14"><View v-if="isFieldVisible('targetPosition')" /><Hide v-else /></el-icon></button>
          <span class="magic-delete-placeholder" />
        </el-form-item>
        <el-form-item v-if="isFieldVisible('availability')" prop="availability" class="profile-field-row magic-field-row group">
          <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
          <span class="magic-icon"><el-icon :size="14"><Briefcase /></el-icon></span>
          <span class="field-label magic-label">状态</span>
          <el-input v-model="formData.availability" class="field-control magic-input" placeholder="请输入到岗时间" maxlength="64" />
          <button type="button" class="magic-action" title="显示/隐藏" @click="toggleField('availability')"><el-icon :size="14"><View /></el-icon></button>
          <button type="button" class="magic-delete" title="移除" @click="removeProfileField('availability')"><el-icon :size="14"><Delete /></el-icon></button>
        </el-form-item>
        <el-form-item v-if="isFieldVisible('birthDate')" prop="birthDate" class="profile-field-row magic-field-row group">
          <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
          <span class="magic-icon"><el-icon :size="14"><Calendar /></el-icon></span>
          <span class="field-label magic-label">生日</span>
          <el-date-picker v-model="formData.birthDate" class="field-control magic-input" type="month" placeholder="选择出生年月" format="YYYY-MM" value-format="YYYY-MM" />
          <button type="button" class="magic-action" title="显示/隐藏" @click="toggleField('birthDate')"><el-icon :size="14"><View /></el-icon></button>
          <button type="button" class="magic-delete" title="移除" @click="removeProfileField('birthDate')"><el-icon :size="14"><Delete /></el-icon></button>
        </el-form-item>
        <el-form-item v-if="isFieldVisible('email')" prop="email" class="profile-field-row magic-field-row group">
          <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
          <span class="magic-icon"><el-icon :size="14"><Message /></el-icon></span>
          <span class="field-label magic-label">邮箱</span>
          <el-input v-model="formData.email" class="field-control magic-input" placeholder="请输入邮箱" maxlength="128" />
          <button type="button" class="magic-action" title="显示/隐藏" @click="toggleField('email')"><el-icon :size="14"><View /></el-icon></button>
          <button type="button" class="magic-delete" title="移除" @click="removeProfileField('email')"><el-icon :size="14"><Delete /></el-icon></button>
        </el-form-item>
        <el-form-item v-if="isFieldVisible('phone')" prop="phone" class="profile-field-row magic-field-row group">
          <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
          <span class="magic-icon"><el-icon :size="14"><Phone /></el-icon></span>
          <span class="field-label magic-label">电话</span>
          <el-input v-model="formData.phone" class="field-control magic-input" placeholder="请输入手机号" maxlength="11" />
          <button type="button" class="magic-action" title="显示/隐藏" @click="toggleField('phone')"><el-icon :size="14"><View /></el-icon></button>
          <button type="button" class="magic-delete" title="移除" @click="removeProfileField('phone')"><el-icon :size="14"><Delete /></el-icon></button>
        </el-form-item>
        <el-form-item v-if="isFieldVisible('city')" prop="city" class="profile-field-row magic-field-row group">
          <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
          <span class="magic-icon"><el-icon :size="14"><Location /></el-icon></span>
          <span class="field-label magic-label">地址</span>
          <el-input v-model="formData.city" class="field-control magic-input" placeholder="请输入所在城市" maxlength="50" />
          <button type="button" class="magic-action" title="显示/隐藏" @click="toggleField('city')"><el-icon :size="14"><View /></el-icon></button>
          <button type="button" class="magic-delete" title="移除" @click="removeProfileField('city')"><el-icon :size="14"><Delete /></el-icon></button>
        </el-form-item>
      </div>
    </section>

    <section class="magic-section">
      <div class="magic-section-head">
        <h3 class="magic-heading">自定义字段</h3>
        <span class="magic-heading-sub">对齐 CustomField 样式 · label/value 两栏</span>
      </div>
      <div class="magic-field-list">
        <template v-for="field in formData.customFields || []" :key="field.id">
          <div v-if="isFieldVisible(field.id)" class="custom-field-row magic-custom-row group">
            <span class="magic-drag" aria-hidden="true"><el-icon class="magic-drag-icon"><Rank /></el-icon></span>
            <span class="magic-icon"><el-icon :size="14"><Link /></el-icon></span>
            <el-input v-model="field.label" class="custom-label magic-input" placeholder="字段名称" maxlength="32" />
            <el-input v-model="field.value" class="custom-value magic-input" placeholder="字段内容" maxlength="200" />
            <span class="show-label"><el-switch v-model="field.showLabel" size="small" /> <span class="show-label-text">显示标签</span></span>
            <button type="button" class="magic-action" @click="toggleField(field.id)"><el-icon :size="14"><View /></el-icon></button>
            <button type="button" class="magic-delete" @click="removeCustomField(field.id)"><el-icon :size="14"><Delete /></el-icon></button>
          </div>
        </template>
        <div v-if="addingCustomField" class="custom-add-form magic-card">
          <el-input v-model="newFieldLabel" autofocus placeholder="请输入字段名称" @keyup.enter="confirmAddCustomField" class="magic-input" />
          <el-button type="primary" class="magic-primary-btn" round @click="confirmAddCustomField">确定</el-button>
          <el-button round @click="cancelAddCustomField">取消</el-button>
        </div>
        <button v-else class="add-custom-button magic-primary-btn" type="button" @click="startAddCustomField"><el-icon><Plus /></el-icon> 添加自定义字段</button>
      </div>
    </section>

    <section class="magic-section">
      <div class="avatar-card magic-card">
        <div class="avatar-title">头像</div>
        <div class="avatar-body">
          <img v-if="formData.avatarUrl" class="avatar-preview" :src="formData.avatarUrl" alt="头像预览">
          <div v-else class="avatar-placeholder">暂无头像</div>
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/jpeg,image/png,image/webp" :on-change="handleAvatarChange">
            <el-button round :loading="avatarUploading">{{ avatarUploading ? '上传中…' : '选择图片' }}</el-button>
          </el-upload>
        </div>
        <span class="avatar-upload-hint">JPG、PNG 或 WEBP，最大 10MB · 将用于简历头像与一寸照优化</span>
      </div>
      <div class="extra-settings magic-card">
        <el-button type="primary" round class="magic-primary-btn" @click="goAvatarUpload">上传并优化一寸照</el-button>
        <el-divider class="magic-divider">展示设置</el-divider>
        <div class="setting-grid">
          <label class="setting-row"><span>显示头像</span><el-switch v-model="formData.showAvatar" /></label>
          <label class="setting-row"><span>显示性别</span><el-switch v-model="formData.showGender" /></label>
          <label class="setting-row"><span>显示年龄</span><el-switch v-model="formData.showAge" /></label>
          <label class="setting-row"><span>显示薪资</span><el-switch v-model="formData.showSalary" /></label>
        </div>
      </div>
    </section>
  </el-form>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { Briefcase, Calendar, Delete, Hide, Link, Location, Message, Phone, Plus, Rank, User, View } from '@element-plus/icons-vue'
import type { Profile, Resume, Section } from '@/types/resume'
import { useSectionSync } from '@/composables/useSectionSync'
import { avatarApi } from '@/api/avatar'
import { validateAvatarFile, validateDate, validateEmail, validatePhone, validateUrl } from '@/utils/validation'

interface Props {
  resume: Resume
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', payload: { title?: string; targetPosition?: string; sections: Section[] }): void
  (e: 'validation', valid: boolean): void
}>()
const formRef = ref<FormInstance>()
const avatarUploading = ref(false)
const hiddenFields = ref<string[]>([])
const addingCustomField = ref(false)
const newFieldLabel = ref('')

function isFieldVisible(key: string) {
  return !hiddenFields.value.includes(key)
}

function removeProfileField(key: string) {
  hiddenFields.value = [...hiddenFields.value, key]
}

function toggleField(key: string) {
  hiddenFields.value = isFieldVisible(key) ? [...hiddenFields.value, key] : hiddenFields.value.filter(item => item !== key)
}

async function handleAvatarChange(file: UploadFile) {
  if (!file.raw || !props.resume?.id) return
  const result = validateAvatarFile(file.raw)
  if (!result.valid) {
    ElMessage.error(result.message || '头像文件不合法')
    return
  }
  avatarUploading.value = true
  try {
    const upload = await avatarApi.upload(file.raw, props.resume.id)
    formData.value.avatarUrl = upload.sourceImageUrl
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const router = useRouter()

function goAvatarUpload() {
  const resumeId = props.resume?.id
  if (!resumeId) return
  router.push(`/workbench/avatar/upload?resumeId=${encodeURIComponent(resumeId)}`)
}

const formData = ref<Profile>({
  name: '',
  gender: '',
  birthDate: '',
  phone: '',
  email: '',
  city: '',
  targetPosition: '',
  expectedSalary: '',
  availability: '',
  personalWebsite: '',
  github: '',
  portfolio: '',
  avatarUrl: '',
  showGender: true,
  showAge: false,
  showSalary: false,
  showAvatar: true,
  customFields: []
})

// 校验规则：前后端必须一致（validation-rules.md §2-§3）
// 注意：profile 内部字段做格式校验；name 必填跨字段「手机或邮箱至少一个」
// 由父组件在保存/导出前再触发 validate() 主动校验，避免编辑中频繁报错。
const fieldRules: FormRules<Profile> = {
  name: [
    { required: true, message: '姓名为必填项', trigger: 'blur' },
    { max: 50, message: '姓名不能超过 50 个字符', trigger: 'blur' }
  ],
  phone: [
    {
      validator: (_rule, value: string, cb) => {
        if (!value) return cb()
        if (!validatePhone(value)) return cb(new Error('手机号格式不正确'))
        cb()
      },
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: (_rule, value: string, cb) => {
        if (!value) return cb()
        if (!validateEmail(value)) return cb(new Error('邮箱格式不正确'))
        cb()
      },
      trigger: 'blur'
    }
  ],
  birthDate: [
    {
      validator: (_rule, value: string, cb) => {
        if (!value) return cb()
        if (!validateDate(value)) return cb(new Error('出生年月格式应为 YYYY-MM'))
        cb()
      },
      trigger: 'change'
    }
  ],
  city: [{ max: 50, message: '所在城市不能超过 50 个字符', trigger: 'blur' }],
  targetPosition: [{ max: 128, message: '目标岗位不能超过 128 个字符', trigger: 'blur' }],
  expectedSalary: [{ max: 64, message: '期望薪资不能超过 64 个字符', trigger: 'blur' }],
  availability: [{ max: 64, message: '到岗时间描述不能超过 64 个字符', trigger: 'blur' }],
  personalWebsite: [
    {
      validator: (_rule, value: string, cb) => {
        if (!value) return cb()
        if (!validateUrl(value)) return cb(new Error('个人网站格式不正确'))
        cb()
      },
      trigger: 'blur'
    }
  ],
  github: [
    {
      validator: (_rule, value: string, cb) => {
        if (!value) return cb()
        if (!validateUrl(value)) return cb(new Error('GitHub 链接格式不正确'))
        cb()
      },
      trigger: 'blur'
    }
  ],
  portfolio: [
    {
      validator: (_rule, value: string, cb) => {
        if (!value) return cb()
        if (!validateUrl(value)) return cb(new Error('作品集链接格式不正确'))
        cb()
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 供父组件（保存/导出 PDF）主动触发的整体校验。
 * 在跨字段「手机或邮箱至少一个」的基础上额外检查姓名非空，
 * 错误信息与后端 RESUME_PROFILE_NAME_REQUIRED / RESUME_PROFILE_CONTACT_REQUIRED 对齐。
 */
async function validate(): Promise<boolean> {
  const ok = await new Promise<boolean>((resolve) => {
    if (!formRef.value) return resolve(true)
    formRef.value.validate((valid) => resolve(valid))
  })
  if (!ok) {
    emit('validation', false)
    return false
  }
  if (!formData.value.name?.trim()) {
    ElMessage.error('姓名为必填项')
    emit('validation', false)
    return false
  }
  if (!formData.value.phone && !formData.value.email) {
    ElMessage.error('简历中至少需要填写手机号或邮箱')
    emit('validation', false)
    return false
  }
  emit('validation', true)
  return true
}

defineExpose({ validate })

function startAddCustomField() {
  newFieldLabel.value = ''
  addingCustomField.value = true
}

function confirmAddCustomField() {
  const label = newFieldLabel.value.trim()
  if (!label) return
  formData.value.customFields = [
    ...(formData.value.customFields || []),
    { id: `profile_field_${Date.now()}`, label, value: '', showLabel: true }
  ]
  addingCustomField.value = false
  newFieldLabel.value = ''
}

function cancelAddCustomField() {
  addingCustomField.value = false
  newFieldLabel.value = ''
}

function removeCustomField(id: string) {
  formData.value.customFields = (formData.value.customFields || []).filter(field => field.id !== id)
}

// 从简历中提取个人信息
function extractProfile() {
  if (props.resume && props.resume.sections) {
    const profileSection = props.resume.sections.find(
      (s) => s.type === 'profile'
    )
    if (profileSection && profileSection.data) {
      Object.assign(formData.value, profileSection.data)
    }
  }
}

// 初始化
extractProfile()

// 与父组件 sections 双向同步：外部变更时重新提取，自身 emit 的回传自动忽略
useSectionSync(
  formData,
  () => props.resume?.sections,
  extractProfile,
  () => {
    emit('update', {
      title: props.resume.title,
      targetPosition: formData.value.targetPosition,
      sections: props.resume.sections.map((section) => {
        if (section.type === 'profile') {
          return {
            ...section,
            data: { ...formData.value }
          }
        }
        return section
      })
    })
  },
  () => props.resume?.sections?.find((s) => s.type === 'profile')?.data
)
</script>

<style scoped lang="scss">
/* 对齐 magic-resume globals.css： hsl(48 20% 97%) parchment + 0.75rem 卡片 + ring 输入框 + 细边框阴影 */
.profile-form.magic-form {
  padding: 4px 0 28px;
  color: hsl(var(--st-foreground));
  font-family: var(--st-font-sans);
  background: transparent;
}

/* 全局 magic 令牌复用 — 与 tailwind.css / design-system.scss 同源 */
.magic-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}
.magic-section:last-child { margin-bottom: 0; }

.magic-section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px;
}
.magic-heading {
  margin: 0;
  font-family: var(--st-font-sans);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: hsl(var(--st-foreground));
}
.magic-heading-sub {
  font-size: 11px;
  color: hsl(var(--st-muted));
  white-space: nowrap;
}

.magic-field-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 单行：左侧 6px drag 占位(实际 16px 含 padding) + 14px 图标 + 80px label + flex1 input + 22px eye + 22px delete */
.magic-field-row {
  display: grid;
  grid-template-columns: 16px 22px 80px minmax(0, 1fr) 28px 28px;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 8px 10px 8px 8px;
  background: hsl(var(--st-card));
  border: 1px solid hsl(var(--st-border));
  border-radius: var(--st-radius-lg); /* 0.75rem 对齐 magic --radius */
  box-shadow: var(--st-shadow-sm);
  transition: border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}
.magic-field-row:hover {
  border-color: hsl(var(--st-foreground) / 0.14);
  box-shadow: var(--st-shadow-md);
}
.magic-field-row :deep(.el-form-item__content) {
  display: contents;
}
.magic-field-row :deep(.el-form-item__error) {
  grid-column: 4;
  position: static;
  padding-top: 4px;
  font-size: 11px;
}

.magic-drag {
  width: 16px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: hsl(var(--st-muted));
  opacity: 0.55;
  cursor: grab;
  transition: opacity 160ms ease, color 160ms ease;
}
.magic-field-row:hover .magic-drag { opacity: 1; color: hsl(var(--st-foreground) / 0.6); }
.magic-drag-icon { font-size: 14px; }

.magic-icon {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: hsl(var(--st-muted-foreground));
  background: hsl(var(--st-secondary) / 0.7);
  border: 1px solid hsl(var(--st-border));
  border-radius: 6px;
}

.magic-label {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--st-foreground));
  white-space: nowrap;
  letter-spacing: -0.01em;
}

.field-control.magic-input { width: 100%; min-width: 0; }
.magic-input :deep(.el-input__wrapper),
.magic-input :deep(.el-date-editor.el-input__wrapper),
:deep(.custom-field-row .el-input__wrapper) {
  min-height: 36px;
  height: 36px;
  padding: 1px 10px;
  background: hsl(var(--st-background));
  border-radius: 0.5rem; /* 输入框 rounded-lg h-9 语义，视觉上 10px 与 magic ring 1 呼应 */
  box-shadow: 0 0 0 1px hsl(var(--st-input)) inset, var(--st-shadow-xs);
  transition: box-shadow 160ms ease, background 160ms ease;
}
.magic-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px hsl(var(--st-foreground) / 0.12) inset, var(--st-shadow-xs);
}
.magic-input :deep(.el-input__wrapper.is-focus) {
  background: hsl(var(--st-card));
  box-shadow: 0 0 0 2px hsl(var(--st-ring)) inset, var(--st-shadow-sm);
}
.magic-input :deep(.el-input__inner) {
  font-size: 13.5px;
  color: hsl(var(--st-foreground));
}
.magic-input :deep(.el-input__inner::placeholder) { color: hsl(var(--st-muted)); }
.magic-input :deep(.el-input__count) { font-size: 11px; }

/* 右侧 22px eye/delete，hover 显现 — 对齐 Field 行尾 */
.magic-action,
.magic-delete {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  border: 1px solid transparent;
  background: transparent;
  color: hsl(var(--st-muted-foreground));
  cursor: pointer;
  transition: background 160ms ease, color 160ms ease, border-color 160ms ease, opacity 160ms ease;
}
.magic-action:hover {
  background: hsl(var(--st-secondary));
  color: hsl(var(--st-foreground));
  border-color: hsl(var(--st-border));
}
.magic-action:active { transform: scale(0.96); }
.magic-delete {
  color: hsl(var(--st-muted-foreground));
}
.magic-delete:hover {
  background: hsl(0 84% 97%);
  color: #dc2626;
  border-color: hsl(0 84% 88%);
}
.magic-delete-placeholder { width: 28px; height: 28px; }

.custom-field-row.magic-custom-row {
  display: grid;
  grid-template-columns: 16px 22px minmax(110px, 1fr) minmax(140px, 1.5fr) auto 28px 28px;
  align-items: center;
  gap: 10px;
  padding: 12px 10px 12px 8px;
  background: hsl(var(--st-card));
  border: 1px solid hsl(var(--st-border));
  border-radius: var(--st-radius-lg);
  box-shadow: var(--st-shadow-sm);
  transition: border-color 160ms ease, box-shadow 160ms ease;
}
.custom-field-row.magic-custom-row:hover {
  border-color: hsl(var(--st-foreground) / 0.14);
  box-shadow: var(--st-shadow-md);
}
.show-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--st-muted-foreground));
  font-size: 12px;
  white-space: nowrap;
}
.show-label-text { font-size: 12px; }
.show-label :deep(.el-switch) { --el-switch-on-color: hsl(var(--st-foreground)); }

.add-custom-button.magic-primary-btn,
.magic-primary-btn {
  width: 100%;
  height: 40px;
  border: 0;
  border-radius: 9999px; /* magic button rounded-full */
  background: hsl(var(--st-primary));
  color: hsl(var(--st-primary-foreground));
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: var(--st-shadow-sm);
  transition: opacity 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}
.add-custom-button.magic-primary-btn:hover { opacity: 0.92; box-shadow: var(--st-shadow-md); }
.add-custom-button.magic-primary-btn:active { transform: scale(0.99); }

.custom-add-form.magic-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: hsl(var(--st-card));
  border: 1px solid hsl(var(--st-border));
  border-radius: var(--st-radius-lg);
  box-shadow: var(--st-shadow-sm);
}
.custom-add-form .el-input { flex: 1; min-width: 0; }

/* 头像卡片带边框阴影 — 对齐 BasicPanel PhotoUpload rounded-xl border */
.avatar-card.magic-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: hsl(var(--st-card));
  border: 1px solid hsl(var(--st-border));
  border-radius: var(--st-radius-lg);
  box-shadow: var(--st-shadow-sm);
}
.avatar-title {
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--st-foreground));
}
.avatar-body {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.avatar-preview {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid hsl(var(--st-border));
  box-shadow: var(--st-shadow-xs);
  background: hsl(var(--st-background));
}
.avatar-placeholder {
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed hsl(var(--st-border));
  border-radius: 10px;
  color: hsl(var(--st-muted));
  font-size: 11px;
  background: hsl(var(--st-secondary) / 0.6);
}
.avatar-upload-hint {
  color: hsl(var(--st-muted-foreground));
  font-size: 11px;
  line-height: 1.5;
}

.extra-settings.magic-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: hsl(var(--st-card));
  border: 1px solid hsl(var(--st-border));
  border-radius: var(--st-radius-lg);
  box-shadow: var(--st-shadow-sm);
}
.extra-settings.magic-card :deep(.el-divider__text) {
  font-size: 12px;
  color: hsl(var(--st-muted-foreground));
  background: hsl(var(--st-card));
}
.magic-divider { margin: 2px 0; }
.setting-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 18px;
}
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12.5px;
  color: hsl(var(--st-muted-foreground));
}
.setting-row :deep(.el-switch.is-checked .el-switch__core) { background-color: hsl(var(--st-foreground)); border-color: hsl(var(--st-foreground)); }

@media (max-width: 900px) {
  .magic-field-row { grid-template-columns: 14px 20px 64px minmax(0, 1fr) 28px 28px; gap: 6px; padding: 8px 8px 8px 6px; }
  .magic-label { font-size: 12.5px; }
  .custom-field-row.magic-custom-row { grid-template-columns: 14px 20px 1fr 1fr 28px 28px; }
  .show-label { grid-column: 3 / 5; }
  .setting-grid { grid-template-columns: 1fr; }
}
</style>
