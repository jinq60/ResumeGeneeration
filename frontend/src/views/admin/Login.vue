<template>
  <div class="min-h-screen w-full flex items-center justify-center p-6 bg-surface-container">
    <div class="w-full max-w-[420px] bg-surface-container-lowest rounded-2xl border border-outline-variant p-8 shadow-md">
      <div class="flex items-center gap-3 mb-6">
        <div class="w-12 h-12 rounded-lg bg-primary flex items-center justify-center shrink-0">
          <el-icon
            size="28"
            color="#fff"
          >
            <DocumentChecked />
          </el-icon>
        </div>
        <div>
          <h1 class="text-title-lg font-title-lg text-on-surface tracking-tight">
            智能简历
          </h1>
          <p class="text-label-md text-on-surface-variant mt-0.5">
            管理后台
          </p>
        </div>
      </div>

      <h2 class="text-title-lg font-title-lg text-on-surface tracking-tight">
        管理员登录
      </h2>
      <p class="text-body-md text-on-surface-variant mt-1">
        请输入管理员账号继续访问后台
      </p>

      <div
        v-if="errorMsg"
        class="mt-5 flex items-center gap-2 p-3 bg-error-container/20 border border-error/25 rounded-lg text-error text-body-md"
      >
        <el-icon size="16">
          <WarningFilled />
        </el-icon>
        <span>{{ errorMsg }}</span>
      </div>

      <form
        class="mt-6 flex flex-col gap-3"
        @submit.prevent="handleLogin"
      >
        <label class="text-label-md font-label-md text-on-surface-variant">账号</label>
        <div class="relative flex items-center">
          <span class="absolute left-3 text-on-surface-variant pointer-events-none">
            <el-icon size="16">
              <User />
            </el-icon>
          </span>
          <input
            v-model="loginForm.username"
            type="text"
            required
            placeholder="请输入管理员账号"
            class="w-full pl-10 pr-4 py-3 bg-surface-container-low border-none rounded-lg text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
          >
        </div>

        <label class="text-label-md font-label-md text-on-surface-variant">密码</label>
        <div class="relative flex items-center">
          <span class="absolute left-3 text-on-surface-variant pointer-events-none">
            <el-icon size="16">
              <Lock />
            </el-icon>
          </span>
          <input
            v-model="loginForm.password"
            :type="showPassword ? 'text' : 'password'"
            required
            placeholder="请输入密码"
            class="w-full pl-10 pr-10 py-3 bg-surface-container-low border-none rounded-lg text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
          >
          <button
            type="button"
            class="absolute right-3 bg-transparent border-none p-1 text-on-surface-variant hover:text-on-surface transition-colors"
            @click="showPassword = !showPassword"
          >
            <el-icon size="16">
              <View v-if="showPassword" /><Hide v-else />
            </el-icon>
          </button>
        </div>

        <button
          type="submit"
          class="mt-4 w-full py-3 bg-primary text-on-primary rounded-lg font-label-md flex items-center justify-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-55 disabled:cursor-not-allowed"
          :disabled="loading"
        >
          <el-icon
            v-if="loading"
            class="animate-spin"
            size="16"
          >
            <Loading />
          </el-icon>
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </button>
      </form>

      <p class="mt-6 text-center text-label-md text-on-surface-variant">
        仅授权管理员可访问后台管理系统
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/admin/auth'
import { userApi } from '@/api/admin/users'
import {
  DocumentChecked,
  User,
  Lock,
  View,
  Hide,
  Loading,
  WarningFilled
} from '@element-plus/icons-vue'

const router = useRouter()

const loginForm = reactive({ username: '', password: '' })
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')

function parseJwtRole(token: string): string | null {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json).role || null
  } catch {
    return null
  }
}

async function handleLogin() {
  errorMsg.value = ''
  if (!loginForm.username.trim() || !loginForm.password.trim()) {
    errorMsg.value = '请输入账号和密码'
    return
  }

  loading.value = true
  try {
    const data = await authApi.login({
      account: loginForm.username.trim(),
      password: loginForm.password
    })

    const accessToken = data?.accessToken
    if (!accessToken) {
      throw new Error('登录响应缺少令牌')
    }

    const role = parseJwtRole(accessToken)
    if (role !== 'ADMIN') {
      throw new Error('该账号不是管理员，无法进入后台')
    }

    localStorage.setItem('admin_token', accessToken)

    // 获取管理员昵称用于顶部展示
    try {
      const user = await userApi.getUser(data.userId)
      localStorage.setItem('admin_username', user.nickname || loginForm.username.trim())
    } catch {
      localStorage.setItem('admin_username', loginForm.username.trim())
    }

    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
