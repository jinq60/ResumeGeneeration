<template>
  <div class="min-h-screen w-full bg-surface flex items-center justify-center p-3 md:p-6">
    <div class="w-full max-w-[1080px] min-h-[640px] grid grid-cols-1 lg:grid-cols-[42%_1fr] bg-surface-container-lowest rounded-2xl shadow-lg overflow-hidden border border-outline-variant/30">
      <!-- 左：深蓝档案封面 -->
      <aside class="bg-navy-900 text-on-primary p-10 flex flex-col justify-between relative overflow-hidden">
        <div class="relative z-10">
          <span class="inline-flex items-center gap-2 px-3 py-1.5 border border-white/20 bg-white/10 rounded-full text-label-md text-primary font-mono">
            <span class="w-1.5 h-1.5 rounded-full bg-primary" />
            <span>Resume / 01</span>
          </span>
          <h1 class="text-headline-lg font-headline-lg mt-6 leading-tight tracking-tight">
            把经历，<br>
            整理成<span class="text-primary">机会</span>。
          </h1>
          <div class="w-12 h-0.5 bg-primary mt-4 rounded-full" />
        </div>

        <!-- 微缩 A4 简历 -->
        <div class="relative z-10 bg-surface-container-lowest text-on-surface rounded-lg p-5 shadow-md max-w-[360px] mx-auto my-8">
          <div class="flex items-center gap-3 pb-3 border-b border-outline-variant">
            <div class="w-11 h-11 rounded-md bg-gradient-to-br from-primary-container to-primary flex items-center justify-center text-on-primary font-bold text-lg">
              Z
            </div>
            <div>
              <div class="text-body-md font-bold text-on-surface">
                张一航
              </div>
              <div class="text-label-md text-primary">
                产品经理
              </div>
              <div class="text-label-md text-on-surface-variant mt-0.5 flex items-center gap-1.5">
                <span>上海</span><span>·</span><span>3 年经验</span>
              </div>
            </div>
          </div>
          <div class="mt-4">
            <div class="text-label-md font-semibold text-on-surface border-l-2 border-primary pl-1.5 mb-1">
              工作经历
            </div>
            <div class="flex justify-between text-label-md text-on-surface-variant">
              <span>星云科技 · 产品经理</span>
              <span>2021 - 至今</span>
            </div>
            <div class="text-label-md text-on-surface-variant mt-0.5">
              · 负责企业协作产品规划，提升活跃度 35%
            </div>
          </div>
          <div class="mt-4">
            <div class="text-label-md font-semibold text-on-surface border-l-2 border-primary pl-1.5 mb-1">
              技能
            </div>
            <div class="flex flex-wrap gap-1">
              <span
                v-for="s in skills"
                :key="s"
                class="bg-surface-container-low border border-outline-variant text-on-surface-variant text-label-md px-1.5 py-0.5 rounded-sm"
              >{{ s }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右：纸白登录面 -->
      <section class="flex flex-col justify-between p-8 bg-surface-container-lowest">
        <div class="max-w-sm mx-auto my-auto w-full">
          <header class="mb-6">
            <h2 class="text-headline-md font-headline-md text-on-surface">
              欢迎回来
            </h2>
            <p class="text-body-md text-on-surface-variant mt-2">
              邮箱账号首次登录将自动创建
            </p>
          </header>

          <div
            v-if="errorMsg"
            class="mt-5 flex items-center gap-2 p-3 bg-error-container/30 border border-error/25 rounded-lg text-body-md text-error"
          >
            <el-icon><WarningFilled /></el-icon>
            <span>{{ errorMsg }}</span>
          </div>

          <!-- 登录表单 -->
          <form
            class="mt-6 flex flex-col gap-3"
            @submit.prevent="handleLogin"
          >
            <div class="flex gap-2 mb-1">
              <button
                type="button"
                :class="['flex-1 py-2 rounded-lg text-label-md font-label-md transition-colors', loginMode === 'password' ? 'bg-primary-fixed text-primary' : 'text-on-surface-variant hover:bg-surface-container-low']"
                @click="loginMode = 'password'"
              >
                账号密码
              </button>
              <button
                type="button"
                :class="['flex-1 py-2 rounded-lg text-label-md font-label-md transition-colors', loginMode === 'email_code' ? 'bg-primary-fixed text-primary' : 'text-on-surface-variant hover:bg-surface-container-low']"
                @click="loginMode = 'email_code'"
              >
                邮箱验证码
              </button>
            </div>

            <template v-if="loginMode === 'password'">
              <label class="text-label-md font-label-md text-on-surface-variant">手机号或邮箱</label>
              <div class="relative flex items-center">
                <span class="absolute left-3 text-outline pointer-events-none">
                  <el-icon size="16"><User /></el-icon>
                </span>
                <input
                  v-model="loginForm.account"
                  type="text"
                  required
                  placeholder="请输入手机号或邮箱"
                  class="w-full bg-surface-container-low border-none rounded-lg pl-10 pr-3 py-2.5 text-body-md placeholder:text-on-surface-variant focus:ring-1 focus:ring-primary focus:outline-none"
                >
              </div>

              <label class="text-label-md font-label-md text-on-surface-variant mt-2">密码</label>
              <div class="relative flex items-center">
                <span class="absolute left-3 text-outline pointer-events-none">
                  <el-icon size="16"><Lock /></el-icon>
                </span>
                <input
                  v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  required
                  placeholder="请输入 8-32 位密码"
                  class="w-full bg-surface-container-low border-none rounded-lg pl-10 pr-10 py-2.5 text-body-md placeholder:text-on-surface-variant focus:ring-1 focus:ring-primary focus:outline-none"
                >
                <button
                  type="button"
                  class="absolute right-3 text-outline hover:text-on-surface transition-colors"
                  @click="showPassword = !showPassword"
                >
                  <el-icon size="16">
                    <View v-if="showPassword" /><Hide v-else />
                  </el-icon>
                </button>
              </div>
            </template>

            <template v-else>
              <label class="text-label-md font-label-md text-on-surface-variant">邮箱</label>
              <div class="relative flex items-center">
                <span class="absolute left-3 text-outline pointer-events-none">
                  <el-icon size="16"><Message /></el-icon>
                </span>
                <input
                  v-model="emailCodeForm.email"
                  type="email"
                  required
                  placeholder="请输入邮箱"
                  class="w-full bg-surface-container-low border-none rounded-lg pl-10 pr-3 py-2.5 text-body-md placeholder:text-on-surface-variant focus:ring-1 focus:ring-primary focus:outline-none"
                >
              </div>

              <label class="text-label-md font-label-md text-on-surface-variant mt-2">验证码</label>
              <div class="flex gap-2">
                <input
                  v-model="emailCodeForm.code"
                  type="text"
                  required
                  maxlength="6"
                  placeholder="6 位验证码"
                  class="flex-1 bg-surface-container-low border-none rounded-lg px-3 py-2.5 text-body-md placeholder:text-on-surface-variant focus:ring-1 focus:ring-primary focus:outline-none"
                >
                <button
                  type="button"
                  class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 font-label-md transition-colors whitespace-nowrap disabled:opacity-55 disabled:cursor-not-allowed"
                  :disabled="countdown > 0 || codeSending"
                  @click="handleSendEmailCode"
                >
                  {{ countdown > 0 ? `${countdown}s 后重发` : codeSending ? '发送中…' : '获取验证码' }}
                </button>
              </div>
            </template>

            <button
              type="submit"
              class="mt-4 w-full bg-primary text-on-primary px-4 py-2.5 rounded-lg font-label-md flex items-center justify-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-55 disabled:cursor-not-allowed"
              :disabled="loading"
            >
              <el-icon
                v-if="loading"
                class="animate-spin"
                size="16"
              >
                <Loading />
              </el-icon>
              <span>{{ loading ? '登录中...' : loginMode === 'password' ? '登录' : '验证码登录' }}</span>
            </button>

            <div class="relative flex items-center my-5">
              <div class="flex-1 h-px bg-outline-variant" />
              <span class="px-3 text-label-md text-on-surface-variant">或</span>
              <div class="flex-1 h-px bg-outline-variant" />
            </div>

            <div class="grid grid-cols-4 gap-2">
              <button
                type="button"
                class="flex flex-col items-center gap-1.5 py-2.5 rounded-lg border border-outline-variant hover:bg-surface-container-low text-on-surface-variant transition-colors"
                title="使用 Google 账号登录"
                @click="handleOAuth('google')"
              >
                <BrandIcon name="google" />
                <span class="text-label-md">Google</span>
              </button>
              <button
                type="button"
                class="flex flex-col items-center gap-1.5 py-2.5 rounded-lg border border-outline-variant hover:bg-surface-container-low text-on-surface-variant transition-colors"
                title="使用 GitHub 账号登录"
                @click="handleOAuth('github')"
              >
                <BrandIcon name="github" />
                <span class="text-label-md">GitHub</span>
              </button>
              <button
                type="button"
                class="flex flex-col items-center gap-1.5 py-2.5 rounded-lg border border-outline-variant hover:bg-surface-container-low text-on-surface-variant transition-colors"
                title="使用 QQ 扫码登录"
                @click="handleOAuth('qq')"
              >
                <BrandIcon name="qq" />
                <span class="text-label-md">QQ</span>
              </button>
              <button
                type="button"
                disabled
                class="flex flex-col items-center gap-1.5 py-2.5 rounded-lg border border-outline-variant text-outline opacity-60 cursor-not-allowed"
                title="手机验证码登录即将上线"
              >
                <el-icon :size="18">
                  <Iphone />
                </el-icon>
                <span class="text-label-md">短信</span>
              </button>
            </div>

            <button
              type="button"
              class="login-ghost w-full border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2.5 font-label-md flex items-center justify-center gap-2 disabled:opacity-55 disabled:cursor-not-allowed transition-colors"
              :disabled="loading"
              @click="handleGuest"
            >
              <el-icon size="16">
                <User />
              </el-icon>
              <span>以游客身份体验</span>
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi, oauthAuthorizeUrl } from '@/api/auth'
import BrandIcon from '@/components/common/BrandIcon.vue'
import { User, Lock, View, Hide, Loading, Iphone, Message, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginMode = ref<'password' | 'email_code'>('password')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const loginForm = reactive({ account: '', password: '' })
const emailCodeForm = reactive({ email: '', code: '' })

const skills = ['需求分析', '产品设计', '数据分析', 'Axure', 'SQL']

function applyAuth(res: { userId: string; accessToken: string; refreshToken: string; isGuest?: boolean }) {
  userStore.setUser({
    userId: res.userId,
    accessToken: res.accessToken,
    refreshToken: res.refreshToken,
    isGuest: res.isGuest
  })
  router.push('/workbench/dashboard')
}

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    if (loginMode.value === 'password') {
      const loginType = loginForm.account.includes('@') ? 'email' : 'phone'
      const res = await authApi.login({ account: loginForm.account, password: loginForm.password, loginType } as any)
      applyAuth(res)
    } else {
      const res = await authApi.emailCodeLogin({ email: emailCodeForm.email, code: emailCodeForm.code })
      applyAuth(res)
    }
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleSendEmailCode() {
  errorMsg.value = ''
  if (!emailCodeForm.email) {
    errorMsg.value = '请先输入邮箱'
    return
  }
  codeSending.value = true
  try {
    await authApi.emailCodeSend(emailCodeForm.email)
    countdown.value = 60
    if (countdownTimer) clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch (e: any) {
    errorMsg.value = e.message || '验证码发送失败'
  } finally {
    codeSending.value = false
  }
}

function handleOAuth(provider: string) {
  errorMsg.value = ''
  window.location.href = oauthAuthorizeUrl(provider)
}

/** 第三方回调：/login?token=...&refresh=... 或 /login?error=... */
onMounted(() => {
  const query = route.query
  if (query.error) {
    errorMsg.value = String(query.error)
  } else if (query.token) {
    userStore.setUser({
      userId: '',
      accessToken: String(query.token),
      refreshToken: String(query.refresh || ''),
      isGuest: query.guest === 'true'
    })
    router.replace('/workbench/dashboard')
  }
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

async function handleGuest() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await authApi.guest()
    userStore.setUser({ userId: res.userId, accessToken: res.accessToken, refreshToken: res.refreshToken, isGuest: true })
    router.push('/workbench/dashboard')
  } catch (e: any) {
    errorMsg.value = e.message || '游客登录失败'
  } finally {
    loading.value = false
  }
}
</script>
