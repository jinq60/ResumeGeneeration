<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
        @click.self="close"
      >
        <Transition
          enter-active-class="transition-all duration-200 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition-all duration-150 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0"
          leave-to-class="opacity-0 scale-95 translate-y-2"
        >
          <div
            v-if="modelValue"
            class="w-full max-w-[420px] bg-card rounded-2xl shadow-2xl border border-border p-6 relative"
          >
            <!-- Close button -->
            <button
              type="button"
              class="absolute right-4 top-4 text-muted-foreground hover:text-foreground transition-colors"
              @click="close"
            >
              <el-icon size="18"><Close /></el-icon>
            </button>

            <header class="mb-6">
              <h2 class="text-xl font-semibold text-foreground">
                欢迎回来
              </h2>
              <p class="text-sm text-muted-foreground mt-1">
                登录后继续创建你的专业简历
              </p>
            </header>

            <div
              v-if="errorMsg"
              class="mb-4 flex items-center gap-2 p-3 bg-destructive/5 border border-destructive/10 rounded-lg text-sm text-destructive"
            >
              <el-icon><WarningFilled /></el-icon>
              <span>{{ errorMsg }}</span>
            </div>

            <form
              v-if="availableMethods.length > 0"
              class="flex flex-col gap-4"
              @submit.prevent="handleLogin"
            >
              <!-- Method switcher -->
              <div
                v-if="displayMethods.length > 1"
                class="flex gap-1 p-1 bg-secondary rounded-xl"
              >
                <button
                  v-for="method in displayMethods"
                  :key="method"
                  type="button"
                  :disabled="!isMethodImplemented(method)"
                  :title="isMethodImplemented(method) ? '' : '暂未开放'"
                  :class="[
                    'flex-1 py-2 rounded-lg text-sm font-medium transition-all',
                    loginMode === method ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground',
                    isMethodImplemented(method) ? 'hover:text-foreground cursor-pointer' : 'opacity-50 cursor-not-allowed'
                  ]"
                  @click="isMethodImplemented(method) && (loginMode = method)"
                >
                  {{ methodLabel(method) }}
                </button>
              </div>

              <template v-if="loginMode === 'password'">
                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-foreground">手机号或邮箱</label>
                  <div class="relative flex items-center">
                    <span class="absolute left-3.5 text-muted pointer-events-none">
                      <el-icon size="16"><User /></el-icon>
                    </span>
                    <input
                      v-model="loginForm.account"
                      type="text"
                      required
                      placeholder="请输入手机号或邮箱"
                      class="w-full bg-secondary border border-transparent focus:border-border rounded-xl pl-10 pr-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:bg-card focus:outline-none transition-all"
                    >
                  </div>
                </div>

                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-foreground">密码</label>
                  <div class="relative flex items-center">
                    <span class="absolute left-3.5 text-muted pointer-events-none">
                      <el-icon size="16"><Lock /></el-icon>
                    </span>
                    <input
                      v-model="loginForm.password"
                      :type="showPassword ? 'text' : 'password'"
                      required
                      placeholder="请输入 8-32 位密码"
                      class="w-full bg-secondary border border-transparent focus:border-border rounded-xl pl-10 pr-10 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:bg-card focus:outline-none transition-all"
                    >
                    <button
                      type="button"
                      class="absolute right-3.5 text-muted hover:text-foreground transition-colors"
                      @click="showPassword = !showPassword"
                    >
                      <el-icon size="16">
                        <View v-if="showPassword" /><Hide v-else />
                      </el-icon>
                    </button>
                  </div>
                </div>
              </template>

              <template v-if="loginMode === 'email_code'">
                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-foreground">邮箱</label>
                  <div class="relative flex items-center">
                    <span class="absolute left-3.5 text-muted pointer-events-none">
                      <el-icon size="16"><Message /></el-icon>
                    </span>
                    <input
                      v-model="emailCodeForm.email"
                      type="email"
                      required
                      placeholder="请输入邮箱"
                      class="w-full bg-secondary border border-transparent focus:border-border rounded-xl pl-10 pr-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:bg-card focus:outline-none transition-all"
                    >
                  </div>
                </div>

                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-foreground">验证码</label>
                  <div class="flex gap-2">
                    <input
                      v-model="emailCodeForm.code"
                      type="text"
                      required
                      maxlength="6"
                      placeholder="6 位验证码"
                      class="flex-1 bg-secondary border border-transparent focus:border-border rounded-xl px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:bg-card focus:outline-none transition-all"
                    >
                    <button
                      type="button"
                      class="border border-border rounded-xl hover:bg-secondary text-foreground px-4 py-2 text-sm font-medium transition-colors whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
                      :disabled="countdown > 0 || codeSending"
                      @click="handleSendEmailCode"
                    >
                      {{ countdown > 0 ? `${countdown}s 后重发` : codeSending ? '发送中…' : '获取验证码' }}
                    </button>
                  </div>
                </div>
              </template>

              <template v-if="loginMode === 'sms_code'">
                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-foreground">手机号</label>
                  <div class="relative flex items-center">
                    <span class="absolute left-3.5 text-muted pointer-events-none">
                      <el-icon size="16"><Iphone /></el-icon>
                    </span>
                    <input
                      v-model="smsCodeForm.phone"
                      type="tel"
                      required
                      maxlength="11"
                      placeholder="请输入手机号"
                      class="w-full bg-secondary border border-transparent focus:border-border rounded-xl pl-10 pr-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:bg-card focus:outline-none transition-all"
                    >
                  </div>
                </div>

                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-foreground">验证码</label>
                  <div class="flex gap-2">
                    <input
                      v-model="smsCodeForm.code"
                      type="text"
                      required
                      maxlength="6"
                      placeholder="6 位验证码"
                      class="flex-1 bg-secondary border border-transparent focus:border-border rounded-xl px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:bg-card focus:outline-none transition-all"
                    >
                    <button
                      type="button"
                      class="border border-border rounded-xl hover:bg-secondary text-foreground px-4 py-2 text-sm font-medium transition-colors whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
                      :disabled="countdown > 0 || codeSending || !isValidPhone(smsCodeForm.phone)"
                      @click="handleSendSmsCode"
                    >
                      {{ countdown > 0 ? `${countdown}s 后重发` : codeSending ? '发送中…' : '获取验证码' }}
                    </button>
                  </div>
                </div>
              </template>

              <button
                type="submit"
                class="mt-2 w-full bg-foreground text-primary-foreground px-4 py-3 rounded-xl font-medium text-sm flex items-center justify-center gap-2 shadow-md hover:opacity-90 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="loading"
              >
                <el-icon
                  v-if="loading"
                  class="animate-spin"
                  size="16"
                >
                  <Loading />
                </el-icon>
                <span>{{ loading ? '登录中...' : submitLabel }}</span>
              </button>

              <!-- OAuth separator -->
              <div
                v-if="displayOAuthProviders.length > 0"
                class="relative flex items-center my-2"
              >
                <div class="flex-1 h-px bg-border" />
                <span class="px-3 text-xs text-muted-foreground font-medium">或</span>
                <div class="flex-1 h-px bg-border" />
              </div>

              <!-- OAuth buttons -->
              <div
                v-if="displayOAuthProviders.length > 0"
                class="flex items-center justify-center gap-3"
              >
                <button
                  v-for="provider in displayOAuthProviders"
                  :key="provider"
                  type="button"
                  :disabled="!isOAuthProviderImplemented(provider)"
                  :title="isOAuthProviderImplemented(provider) ? `使用 ${providerLabel(provider)} 账号登录` : '暂未开放'"
                  :class="[
                    'w-10 h-10 flex items-center justify-center rounded-xl border border-border bg-card transition-all',
                    isOAuthProviderImplemented(provider) ? 'hover:bg-secondary hover:scale-105 cursor-pointer' : 'opacity-50 cursor-not-allowed'
                  ]"
                  @click="isOAuthProviderImplemented(provider) && handleOAuth(provider)"
                >
                  <BrandIcon
                    :name="provider"
                    :size="20"
                  />
                </button>
              </div>
            </form>

            <!-- Loading methods state -->
            <div
              v-else-if="methodsLoading"
              class="mt-12 flex items-center justify-center gap-2 text-muted-foreground"
            >
              <el-icon class="animate-spin" size="18"><Loading /></el-icon>
              <span class="text-sm">正在加载登录方式…</span>
            </div>

            <!-- Fallback when no methods available -->
            <div
              v-else
              class="mt-12 text-center"
            >
              <p class="text-sm text-muted-foreground">
                暂无可用登录方式，请联系管理员。
              </p>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuthModalStore } from '@/stores/authModal'
import { authApi, oauthAuthorizeUrl } from '@/api/auth'
import BrandIcon from '@/components/common/BrandIcon.vue'
import { User, Lock, View, Hide, Loading, Message, Iphone, WarningFilled, Close } from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const authModalStore = useAuthModalStore()

type LoginMethod = 'password' | 'email_code' | 'sms_code'
type OAuthProvider = 'github' | 'google' | 'qq'

const loginMode = ref<LoginMethod>('password')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const methodsLoading = ref(true)
const availableMethods = ref<LoginMethod[]>([])
const configuredOAuthProviders = ref<OAuthProvider[]>([])

const displayMethods = computed<LoginMethod[]>(() => {
  return availableMethods.value
})

const displayOAuthProviders = computed<OAuthProvider[]>(() => {
  return configuredOAuthProviders.value
})

function isMethodImplemented(method: LoginMethod): boolean {
  return availableMethods.value.includes(method)
}

function isOAuthProviderImplemented(provider: OAuthProvider): boolean {
  return configuredOAuthProviders.value.includes(provider)
}

const loginForm = reactive({ account: '', password: '' })
const emailCodeForm = reactive({ email: '', code: '' })
const smsCodeForm = reactive({ phone: '', code: '' })

function isValidPhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}

const submitLabel = computed(() => {
  if (loading.value) return '登录中...'
  if (loginMode.value === 'password') return '登录'
  if (loginMode.value === 'email_code') return '验证码登录'
  if (loginMode.value === 'sms_code') return '验证码登录'
  return '登录'
})

function methodLabel(method: string): string {
  const map: Record<string, string> = {
    password: '账号密码',
    email_code: '邮箱验证码',
    sms_code: '手机验证码'
  }
  return map[method] || method
}

function providerLabel(provider: string): string {
  const map: Record<string, string> = {
    github: 'GitHub',
    google: 'Google',
    qq: 'QQ'
  }
  return map[provider] || provider
}

function close() {
  emit('update:modelValue', false)
  errorMsg.value = ''
}

function applyAuth(res: { userId: string; accessToken: string; refreshToken: string; expiresIn: number; isGuest?: boolean }) {
  // 游客升级为正式账号前，吊销游客会话的刷新令牌（fire-and-forget，失败静默）
  revokeGuestSession()
  userStore.setUser({
    userId: res.userId,
    accessToken: res.accessToken,
    refreshToken: res.refreshToken,
    isGuest: res.isGuest
  })
  close()
  router.push('/workbench/dashboard')
}

/** 当前为游客登录态时，异步调用登出接口吊销其刷新令牌；不等待结果、失败不阻塞正式登录。 */
function revokeGuestSession() {
  if (!userStore.isGuest) return
  const guestRefreshToken = userStore.refreshToken
  if (!userStore.accessToken || !guestRefreshToken) return
  authApi.logout(guestRefreshToken).catch(() => {
    // 服务端吊销失败不阻塞正式登录流程
  })
}

async function loadLoginMethods() {
  methodsLoading.value = true
  try {
    const data = await authApi.loginMethods()
    availableMethods.value = (data.loginMethods || [])
      .filter((m) => m.configured)
      .map((m) => m.method as LoginMethod)

    configuredOAuthProviders.value = (data.oauthProviders || [])
      .filter((p) => p.configured)
      .map((p) => p.provider as OAuthProvider)

    if (availableMethods.value.length > 0 && !availableMethods.value.includes(loginMode.value)) {
      loginMode.value = availableMethods.value[0]
    }
  } catch (e: any) {
    availableMethods.value = ['password', 'email_code'] as LoginMethod[]
    configuredOAuthProviders.value = ['github', 'google'] as OAuthProvider[]
    errorMsg.value = e.message || '加载登录方式失败，已显示默认入口'
  } finally {
    methodsLoading.value = false
  }
}

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    if (loginMode.value === 'password') {
      const loginType = loginForm.account.includes('@') ? 'email' : 'phone'
      const res = await authApi.login({ account: loginForm.account, password: loginForm.password, loginType } as any)
      applyAuth(res)
    } else if (loginMode.value === 'email_code') {
      const res = await authApi.emailCodeLogin({ email: emailCodeForm.email, code: emailCodeForm.code })
      applyAuth(res)
    } else if (loginMode.value === 'sms_code') {
      const res = await authApi.smsCodeLogin({ phone: smsCodeForm.phone, code: smsCodeForm.code })
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
    startCountdown()
  } catch (e: any) {
    errorMsg.value = e.message || '验证码发送失败'
  } finally {
    codeSending.value = false
  }
}

async function handleSendSmsCode() {
  errorMsg.value = ''
  if (!isValidPhone(smsCodeForm.phone)) {
    errorMsg.value = '请输入正确的手机号'
    return
  }
  codeSending.value = true
  try {
    await authApi.smsCodeSend(smsCodeForm.phone)
    startCountdown()
  } catch (e: any) {
    errorMsg.value = e.message || '验证码发送失败'
  } finally {
    codeSending.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

function handleOAuth(provider: string) {
  errorMsg.value = ''
  window.location.href = oauthAuthorizeUrl(provider)
}

/** 处理 OAuth 回调：用一次性授权码换取令牌（JWT 不经 URL 传递），或 ?error=... */
async function handleOAuthCallback() {
  // 错误文本优先来自 authModalStore：/login 回调页会在 router.replace 清除
  // URL query 前转存，避免直接读 route.query.error 的时序竞争
  if (authModalStore.oauthError) {
    errorMsg.value = authModalStore.oauthError
    authModalStore.oauthError = ''
    return
  }

  const query = route.query
  if (query.error) {
    errorMsg.value = String(query.error)
    authModalStore.oauthError = ''
    return
  }

  const code = query.oauth_code as string | undefined
  if (!code) return
  try {
    const auth = await authApi.oauthExchange(code)
    userStore.setUser({
      userId: auth.userId || '',
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      isGuest: auth.isGuest ?? false
    })
    close()
    router.replace('/workbench/dashboard')
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '第三方登录失败，请重试'
  }
}

watch(() => props.modelValue, (open) => {
  if (open) {
    errorMsg.value = ''
    loadLoginMethods()
    handleOAuthCallback()
  } else {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }
})

onMounted(() => {
  if (props.modelValue) {
    loadLoginMethods()
    handleOAuthCallback()
  }
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>
