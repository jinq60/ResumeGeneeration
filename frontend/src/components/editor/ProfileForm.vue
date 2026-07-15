<template>
  <div class="profile-form">
    <el-form
      :model="formData"
      :rules="rules"
      label-width="100px"
      size="default"
    >
      <el-form-item
        label="姓名"
        prop="name"
      >
        <el-input
          v-model="formData.name"
          placeholder="请输入姓名"
        />
      </el-form-item>

      <el-form-item
        label="性别"
        prop="gender"
      >
        <el-radio-group v-model="formData.gender">
          <el-radio label="male">
            男
          </el-radio>
          <el-radio label="female">
            女
          </el-radio>
          <el-radio label="other">
            其他
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item
        label="出生日期"
        prop="birthDate"
      >
        <el-date-picker
          v-model="formData.birthDate"
          type="month"
          placeholder="选择出生年月"
          format="YYYY-MM"
          value-format="YYYY-MM"
        />
      </el-form-item>

      <el-form-item
        label="手机号"
        prop="phone"
      >
        <el-input
          v-model="formData.phone"
          placeholder="请输入手机号"
        />
      </el-form-item>

      <el-form-item
        label="邮箱"
        prop="email"
      >
        <el-input
          v-model="formData.email"
          placeholder="请输入邮箱"
        />
      </el-form-item>

      <el-form-item
        label="所在城市"
        prop="city"
      >
        <el-input
          v-model="formData.city"
          placeholder="请输入所在城市"
        />
      </el-form-item>

      <el-form-item
        label="目标岗位"
        prop="targetPosition"
      >
        <el-input
          v-model="formData.targetPosition"
          placeholder="请输入目标岗位"
        />
      </el-form-item>

      <el-form-item
        label="期望薪资"
        prop="expectedSalary"
      >
        <el-input
          v-model="formData.expectedSalary"
          placeholder="请输入期望薪资"
        />
      </el-form-item>

      <el-form-item
        label="到岗时间"
        prop="availability"
      >
        <el-input
          v-model="formData.availability"
          placeholder="请输入到岗时间"
        />
      </el-form-item>

      <el-form-item
        label="个人网站"
        prop="personalWebsite"
      >
        <el-input
          v-model="formData.personalWebsite"
          placeholder="请输入个人网站链接"
        />
      </el-form-item>

      <el-form-item
        label="GitHub"
        prop="github"
      >
        <el-input
          v-model="formData.github"
          placeholder="请输入GitHub链接"
        />
      </el-form-item>

      <el-form-item
        label="作品集"
        prop="portfolio"
      >
        <el-input
          v-model="formData.portfolio"
          placeholder="请输入作品集链接"
        />
      </el-form-item>

      <el-form-item
        label="头像URL"
        prop="avatarUrl"
      >
        <el-input
          v-model="formData.avatarUrl"
          placeholder="请输入头像URL"
        />
      </el-form-item>

      <el-divider>展示设置</el-divider>

      <el-form-item label="显示性别">
        <el-switch v-model="formData.showGender" />
      </el-form-item>

      <el-form-item label="显示年龄">
        <el-switch v-model="formData.showAge" />
      </el-form-item>

      <el-form-item label="显示薪资">
        <el-switch v-model="formData.showSalary" />
      </el-form-item>

      <el-form-item label="显示头像">
        <el-switch v-model="formData.showAvatar" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Profile } from '@/types/resume'

interface Props {
  resume: any
}

const props = defineProps<Props>()
const emit = defineEmits(['update'])

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
  showAvatar: true
})

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 1, max: 50, message: '姓名长度在1-50个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ],
  personalWebsite: [
    { type: 'url', message: '请输入正确的URL', trigger: 'blur' }
  ],
  github: [
    { type: 'url', message: '请输入正确的URL', trigger: 'blur' }
  ],
  portfolio: [
    { type: 'url', message: '请输入正确的URL', trigger: 'blur' }
  ],
  avatarUrl: [
    { type: 'url', message: '请输入正确的URL', trigger: 'blur' }
  ]
}

// 从简历中提取个人信息
function extractProfile() {
  if (props.resume && props.resume.sections) {
    const profileSection = props.resume.sections.find(
      (s: any) => s.type === 'profile'
    )
    if (profileSection && profileSection.data) {
      Object.assign(formData.value, profileSection.data)
    }
  }
}

// 监听表单变化，触发更新
watch(formData, (newData) => {
  emit('update', {
    title: props.resume.title,
    targetPosition: newData.targetPosition,
    sections: props.resume.sections.map((section: any) => {
      if (section.type === 'profile') {
        return {
          ...section,
          data: newData
        }
      }
      return section
    })
  })
}, { deep: true })

// 初始化
extractProfile()
</script>

<style scoped lang="scss">
.profile-form {
  padding: 16px 0;

  .el-form {
    max-width: 600px;
  }
}
</style>
