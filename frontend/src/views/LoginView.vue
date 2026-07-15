<template>
  <div class="login-view">
    <el-card
      class="login-card"
      shadow="hover"
    >
      <h1 class="title">
        智能简历生成工具
      </h1>
      <p class="subtitle">
        登录后开始创建专业简历
      </p>

      <el-tabs
        v-model="activeTab"
        class="login-tabs"
      >
        <el-tab-pane
          label="登录"
          name="login"
        >
          <el-form
            :model="loginForm"
            @submit.prevent="handleLogin"
          >
            <el-form-item>
              <el-input
                v-model="loginForm.account"
                placeholder="手机号 / 邮箱"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                show-password
              />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane
          label="注册"
          name="register"
        >
          <el-form
            :model="registerForm"
            @submit.prevent="handleRegister"
          >
            <el-form-item>
              <el-input
                v-model="registerForm.phone"
                placeholder="手机号"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.verifyCode"
                placeholder="验证码（任意 6 位数字）"
                maxlength="6"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码（6-32 位）"
                show-password
              />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <el-divider>或</el-divider>

      <el-button
        class="guest-btn"
        plain
        :loading="loading"
        @click="handleGuest"
      >
        游客模式，直接体验
      </el-button>

      <p
        v-if="errorMsg"
        class="error-msg"
      >
        {{ errorMsg }}
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const errorMsg = ref('')

const loginForm = reactive({
  account: '',
  password: '',
  loginType: 'phone' as const
})

const registerForm = reactive({
  phone: '',
  verifyCode: '',
  password: ''
})

function detectLoginType(account: string): 'phone' | 'email' {
  return account.includes('@') ? 'email' : 'phone'
}

async function handleLogin() {
  if (!loginForm.account || !loginForm.password) {
    errorMsg.value = '请填写账号和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await authApi.login({
      account: loginForm.account,
      password: loginForm.password,
      loginType: detectLoginType(loginForm.account)
    })
    userStore.setUser({
      userId: res.userId,
      isGuest: res.isGuest ?? false,
      accessToken: res.accessToken
    })
    ElMessage.success('登录成功')
    router.push('/resumes')
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.phone || !registerForm.verifyCode || !registerForm.password) {
    errorMsg.value = '请填写完整注册信息'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await authApi.register({
      phone: registerForm.phone,
      verifyCode: registerForm.verifyCode,
      password: registerForm.password
    })
    userStore.setUser({
      userId: res.userId,
      isGuest: false,
      accessToken: res.accessToken
    })
    ElMessage.success('注册成功')
    router.push('/resumes')
  } catch (e: any) {
    errorMsg.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}

async function handleGuest() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await authApi.guest()
    userStore.setUser({
      userId: res.userId,
      isGuest: true,
      accessToken: res.accessToken
    })
    ElMessage.success('已进入游客模式')
    router.push('/resumes')
  } catch (e: any) {
    errorMsg.value = e.message || '游客登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
}

.title {
  text-align: center;
  margin-bottom: 8px;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  text-align: center;
  margin-bottom: 24px;
  color: #606266;
}

.login-tabs {
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
}

.guest-btn {
  width: 100%;
}

.error-msg {
  margin-top: 12px;
  color: #f56c6c;
  text-align: center;
  font-size: 14px;
}
</style>
