<template>
  <el-form ref="formRef" :model="formData" :rules="fieldRules" class="profile-form" label-position="top">
    <section class="profile-fields">
      <el-form-item v-if="isFieldVisible('name')" prop="name" class="profile-field-row">
        <el-icon><User /></el-icon><span class="field-label">姓名</span>
        <el-input v-model="formData.name" class="field-control" placeholder="请输入姓名" />
        <el-icon class="field-action" @click="toggleField('name')"><View /></el-icon>
      </el-form-item>
      <el-form-item v-if="isFieldVisible('targetPosition')" prop="targetPosition" class="profile-field-row">
        <el-icon><Briefcase /></el-icon><span class="field-label">职位</span>
        <el-input v-model="formData.targetPosition" class="field-control" placeholder="请输入目标岗位" maxlength="128" show-word-limit />
        <el-icon class="field-action" @click="toggleField('targetPosition')"><View /></el-icon>
      </el-form-item>
      <el-form-item v-if="isFieldVisible('availability')" prop="availability" class="profile-field-row">
        <el-icon><Briefcase /></el-icon><span class="field-label">状态</span>
        <el-input v-model="formData.availability" class="field-control" placeholder="请输入到岗时间" maxlength="64" />
        <el-icon class="field-action" @click="toggleField('availability')"><View /></el-icon><el-icon class="field-delete" @click="removeProfileField('availability')"><Delete /></el-icon>
      </el-form-item>
      <el-form-item v-if="isFieldVisible('birthDate')" prop="birthDate" class="profile-field-row">
        <el-icon><Calendar /></el-icon><span class="field-label">生日</span>
        <el-date-picker v-model="formData.birthDate" class="field-control" type="month" placeholder="选择出生年月" format="YYYY-MM" value-format="YYYY-MM" />
        <el-icon class="field-action" @click="toggleField('birthDate')"><View /></el-icon><el-icon class="field-delete" @click="removeProfileField('birthDate')"><Delete /></el-icon>
      </el-form-item>
      <el-form-item v-if="isFieldVisible('email')" prop="email" class="profile-field-row">
        <el-icon><Message /></el-icon><span class="field-label">邮箱</span>
        <el-input v-model="formData.email" class="field-control" placeholder="请输入邮箱" maxlength="128" />
        <el-icon class="field-action" @click="toggleField('email')"><View /></el-icon><el-icon class="field-delete" @click="removeProfileField('email')"><Delete /></el-icon>
      </el-form-item>
      <el-form-item v-if="isFieldVisible('phone')" prop="phone" class="profile-field-row">
        <el-icon><Phone /></el-icon><span class="field-label">电话</span>
        <el-input v-model="formData.phone" class="field-control" placeholder="请输入手机号" maxlength="11" />
        <el-icon class="field-action" @click="toggleField('phone')"><View /></el-icon><el-icon class="field-delete" @click="removeProfileField('phone')"><Delete /></el-icon>
      </el-form-item>
      <el-form-item v-if="isFieldVisible('city')" prop="city" class="profile-field-row">
        <el-icon><Location /></el-icon><span class="field-label">地址</span>
        <el-input v-model="formData.city" class="field-control" placeholder="请输入所在城市" maxlength="50" />
        <el-icon class="field-action" @click="toggleField('city')"><View /></el-icon><el-icon class="field-delete" @click="removeProfileField('city')"><Delete /></el-icon>
      </el-form-item>
    </section>

    <h3 class="custom-heading">自定义字段</h3>
<template v-for="field in formData.customFields || []" :key="field.id">
      <div v-if="isFieldVisible(field.id)" class="custom-field-row">
        <el-icon><Link /></el-icon>
        <el-input v-model="field.label" class="custom-label" placeholder="字段名称" maxlength="32" />
        <el-input v-model="field.value" class="custom-value" placeholder="字段内容" maxlength="200" />
        <span class="show-label"><el-switch v-model="field.showLabel" size="small" /> 显示标签</span>
        <el-icon class="field-action" @click="toggleField(field.id)"><View /></el-icon><el-icon class="field-delete" @click="removeCustomField(field.id)"><Delete /></el-icon>
      </div>
    </template>
    <div v-if="addingCustomField" class="custom-add-form">
      <el-input v-model="newFieldLabel" autofocus placeholder="请输入字段名称" @keyup.enter="confirmAddCustomField" />
      <el-button type="primary" @click="confirmAddCustomField">确定</el-button>
      <el-button @click="cancelAddCustomField">取消</el-button>
    </div>
    <button v-else class="add-custom-button" type="button" @click="startAddCustomField"><el-icon><Plus /></el-icon> 添加自定义字段</button>

    <div class="avatar-card">
      <div class="avatar-title">头像</div>
      <img v-if="formData.avatarUrl" class="avatar-preview" :src="formData.avatarUrl" alt="头像预览">
      <el-upload :show-file-list="false" :auto-upload="false" accept="image/jpeg,image/png,image/webp" :on-change="handleAvatarChange">
        <el-button :loading="avatarUploading">{{ avatarUploading ? '上传中…' : '选择图片' }}</el-button>
      </el-upload>
      <span class="avatar-upload-hint">JPG、PNG 或 WEBP，最大 10MB</span>
    </div>
    <div class="extra-settings"><el-button type="primary" plain @click="goAvatarUpload">上传并优化一寸照</el-button><el-divider>展示设置</el-divider><span>显示头像</span><el-switch v-model="formData.showAvatar" /><span>显示性别</span><el-switch v-model="formData.showGender" /><span>显示年龄</span><el-switch v-model="formData.showAge" /><span>显示薪资</span><el-switch v-model="formData.showSalary" /></div>
  </el-form>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { Briefcase, Calendar, Delete, Link, Location, Message, Phone, Plus, User, View } from '@element-plus/icons-vue'
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
.profile-form {
  padding: 24px 34px 48px;
  color: #171717;
  font-family: Georgia, 'Times New Roman', 'Microsoft YaHei', serif;
}

.profile-fields {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-field-row {
  display: grid;
  grid-template-columns: 22px 70px minmax(120px, 1fr) 28px 28px;
  align-items: center;
  gap: 10px;
  min-height: 48px;
}

.field-label {
  white-space: nowrap;
  font-size: 15px;
}

.field-control {
  width: min(100%, 360px);
}

.field-control :deep(.el-input__wrapper),
.field-control :deep(.el-date-editor) {
  min-height: 36px;
  border-radius: 11px;
}

.field-action,
.field-delete {
  cursor: pointer;
  justify-self: center;
}

.field-delete {
  color: #ff4d4f;
}

.custom-heading {
  margin: 28px 0 16px 6px;
  font-size: 17px;
  font-weight: 400;
}

.custom-field-row {
  display: grid;
  grid-template-columns: 24px 22px 110px minmax(120px, 1fr) 92px 28px 28px;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #e2e2e2;
  border-radius: 13px;
  margin-bottom: 16px;
}

.custom-field-row :deep(.el-input__wrapper) {
  min-height: 36px;
  border-radius: 10px;
}

.show-label {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
  font-size: 12px;
  white-space: nowrap;
}

.add-custom-button {
  width: 100%;
  height: 42px;
  border: 0;
  border-radius: 9px;
  background: #1b1b18;
  color: white;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.custom-add-form {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.custom-add-form .el-input {
  flex: 1;
}

.avatar-card {
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid #e7e7e7;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.avatar-title {
  width: 100%;
  font-size: 16px;
}

.avatar-preview {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 7px;
  border: 1px solid #ddd;
}

.avatar-upload-hint {
  width: 100%;
  color: #777;
  font-size: 12px;
}

.extra-settings {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 24px;
  color: #555;
  font-size: 13px;
}

.extra-settings :deep(.el-divider) {
  width: 100%;
  margin: 0;
}

@media (max-width: 900px) {
  .profile-form {
    padding: 20px 18px 40px;
  }

  .custom-field-row {
    grid-template-columns: 20px 20px 1fr 1fr 28px 28px;
  }

  .show-label {
    grid-column: 3 / 5;
  }
}
</style>
