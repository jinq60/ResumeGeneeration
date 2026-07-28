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
        <div class="relative z-10 bg-surface-container-lowest text-on-surface rounded-lg p-5 shadow-2xl -rotate-2 hover:rotate-0 transition-transform duration-300 max-w-[360px] mx-auto my-8">
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

        <div class="relative z-10 mt-auto pt-4 border-t border-white/10 text-white/60 text-body-md flex items-center gap-2">
          <span>📁</span>
          <span>从第一条经历开始 —— 在线极速排版</span>
        </div>
      </aside>

      <!-- 右：纸白登录面 -->
      <section class="flex flex-col justify-between p-8 bg-surface-container-lowest">
        <div class="border-b border-outline-variant/30 flex justify-end gap-6 pb-3">
          <button
            :class="['text-body-md font-medium py-1.5 px-0 relative transition-colors', activeTab === 'login' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface']"
            @click="activeTab = 'login'"
          >
            登录
            <span
              v-if="activeTab === 'login'"
              class="absolute left-0 right-0 -bottom-[13px] h-0.5 bg-primary rounded-full"
            />
          </button>
          <button
            :class="['text-body-md font-medium py-1.5 px-0 relative transition-colors', activeTab === 'register' ? 'text-primary' : 'text-on-surface-variant hover:text-on-surface']"
            @click="activeTab = 'register'"
          >
            注册
            <span
              v-if="activeTab === 'register'"
              class="absolute left-0 right-0 -bottom-[13px] h-0.5 bg-primary rounded-full"
            />
          </button>
        </div>

        <div class="max-w-sm mx-auto my-auto w-full">
          <header class="mb-6">
            <h2 class="text-headline-md font-headline-md text-on-surface">
              {{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}
            </h2>
            <p class="text-body-md text-on-surface-variant mt-2">
              {{ activeTab === 'login'
                ? '登录你的账号，继续完善你的职业档案'
                : '注册后即可开始制作你的第一份简历' }}
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
            v-if="activeTab === 'login'"
            class="mt-6 flex flex-col gap-3"
            @submit.prevent="handleLogin"
          >
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
                placeholder="请输入 6-32 位密码"
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
              <span>{{ loading ? '登录中...' : '登录' }}</span>
            </button>

            <div class="relative flex items-center my-5">
              <div class="flex-1 h-px bg-outline-variant" />
              <span class="px-3 text-label-md text-on-surface-variant">或</span>
              <div class="flex-1 h-px bg-outline-variant" />
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
            <p class="text-center text-label-md text-on-surface-variant mt-3">
              可创建临时简历，随时注册保存。
            </p>
          </form>

          <!-- 注册表单 -->
          <form
            v-else
            class="mt-6 flex flex-col gap-3"
            @submit.prevent="handleRegister"
          >
            <label class="text-label-md font-label-md text-on-surface-variant">手机号</label>
            <div class="relative flex items-center">
              <span class="absolute left-3 text-outline pointer-events-none">
                <el-icon size="16"><Iphone /></el-icon>
              </span>
              <input
                v-model="registerForm.phone"
                type="tel"
                required
                placeholder="请输入手机号"
                class="w-full bg-surface-container-low border-none rounded-lg pl-10 pr-3 py-2.5 text-body-md placeholder:text-on-surface-variant focus:ring-1 focus:ring-primary focus:outline-none"
              >
            </div>

            <label class="text-label-md font-label-md text-on-surface-variant">验证码</label>
            <div class="flex gap-2">
              <input
                v-model="registerForm.verifyCode"
                type="text"
                required
                maxlength="6"
                placeholder="6 位验证码"
                class="flex-1 bg-surface-container-low border-none rounded-lg px-3 py-2.5 text-body-md placeholder:text-on-surface-variant focus:ring-1 focus:ring-primary focus:outline-none"
              >
              <button
                type="button"
                class="border border-outline-variant rounded-lg hover:bg-surface-container-low text-on-surface-variant px-4 py-2 font-label-md transition-colors whitespace-nowrap"
              >
                获取验证码
              </button>
            </div>

            <label class="text-label-md font-label-md text-on-surface-variant">密码</label>
            <input
              v-model="registerForm.password"
              type="password"
              required
              placeholder="6-32 位密码"
              class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2.5 text-body-md placeholder:text-on-surface-variant focus:ring-1 focus:ring-primary focus:outline-none"
            >

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
              <span>{{ loading ? '注册中...' : '注册' }}</span>
            </button>
          </form>
        </div>

        <footer class="mt-6 pt-5 border-t border-outline-variant/30 grid grid-cols-3 gap-3">
          <div class="flex flex-col items-center text-center gap-1">
            <el-icon
              size="14"
              class="text-primary"
            >
              <Lock />
            </el-icon>
            <div class="mt-1">
              <div class="text-label-md font-medium text-on-surface">
                数据安全
              </div>
              <div class="text-label-md text-on-surface-variant">
                银行级加密保护
              </div>
            </div>
          </div>
          <div class="flex flex-col items-center text-center gap-1">
            <el-icon
              size="14"
              class="text-secondary"
            >
              <Hide />
            </el-icon>
            <div class="mt-1">
              <div class="text-label-md font-medium text-on-surface">
                隐私保障
              </div>
              <div class="text-label-md text-on-surface-variant">
                简历仅你可见
              </div>
            </div>
          </div>
          <div class="flex flex-col items-center text-center gap-1">
            <el-icon
              size="14"
              class="text-on-surface"
            >
              <Download />
            </el-icon>
            <div class="mt-1">
              <div class="text-label-md font-medium text-on-surface">
                随时导出
              </div>
              <div class="text-label-md text-on-surface-variant">
                支持 PDF 格式
              </div>
            </div>
          </div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import { User, Lock, View, Hide, Loading, Iphone, Download, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const loginForm = reactive({ account: '', password: '' })
const registerForm = reactive({ phone: '', verifyCode: '', password: '' })

const skills = ['需求分析', '产品设计', '数据分析', 'Axure', 'SQL']

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const loginType = loginForm.account.includes('@') ? 'email' : 'phone'
    const res = await authApi.login({ account: loginForm.account, password: loginForm.password, loginType } as any)
    userStore.setUser({ userId: res.userId, accessToken: res.accessToken, isGuest: res.isGuest })
    router.push('/dashboard')
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await authApi.register({ phone: registerForm.phone, verifyCode: registerForm.verifyCode, password: registerForm.password })
    userStore.setUser({ userId: res.userId, accessToken: res.accessToken, isGuest: res.isGuest })
    router.push('/dashboard')
  } catch (e: any) {
    errorMsg.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}

async function handleGuest() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await authApi.guest()
    userStore.setUser({ userId: res.userId, accessToken: res.accessToken, isGuest: true })
    router.push('/dashboard')
  } catch (e: any) {
    errorMsg.value = e.message || '游客登录失败'
  } finally {
    loading.value = false
  }
}
</script>
