<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import client from '@/api/client'
import { pushToast } from '@/composables/useToast'
import type { UserInfoResponse } from '@/api/types'

const router = useRouter()
const auth = useAuthStore()

// ── profile ──
const loadingProfile = ref(false)
const savingProfile = ref(false)
const profileForm = ref({
  nickname: '',
  phone: '',
  email: '',
  avatarUrl: '',
})
const avatarPreviewError = ref(false)

const avatarPreviewValid = computed(() => {
  const u = profileForm.value.avatarUrl?.trim()
  return !!u && !avatarPreviewError.value && /^https?:\/\//.test(u)
})

async function fetchProfile() {
  loadingProfile.value = true
  try {
    const { data } = await client.get('/users/me')
    const user: UserInfoResponse = data?.data ?? data
    profileForm.value.nickname = user?.nickname ?? ''
    profileForm.value.phone = user?.phone ?? ''
    profileForm.value.email = user?.email ?? ''
    profileForm.value.avatarUrl = user?.avatarUrl ?? ''
    // sync auth store
    if (user) auth.user = user as UserInfoResponse
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '加载资料失败'
    pushToast(msg)
  } finally {
    loadingProfile.value = false
  }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    const payload: Record<string, string> = {}
    const f = profileForm.value
    // only send non-empty trimmed values; allow clearing? spec allows optional, send trimmed
    if (f.nickname.trim()) payload.nickname = f.nickname.trim()
    else payload.nickname = ''
    if (f.phone.trim()) payload.phone = f.phone.trim()
    if (f.email.trim()) payload.email = f.email.trim()
    if (f.avatarUrl.trim()) payload.avatarUrl = f.avatarUrl.trim()
    // if all empty we still send empty to clear? keep at least nickname
    const { data } = await client.put('/users/me', payload)
    const updated: UserInfoResponse = data?.data ?? data
    if (updated && updated.userId) auth.user = updated
    else await fetchProfile()
    pushToast('资料已保存')
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '保存失败'
    pushToast(msg)
  } finally {
    savingProfile.value = false
  }
}

// ── password ──
const pwdForm = ref({ oldPassword: '', newPassword: '' })
const savingPwd = ref(false)
const pwdError = ref('')

function validatePwd(pwd: string): string | null {
  if (!pwd) return '请输入密码'
  if (pwd.length < 8 || pwd.length > 32) return '密码需 8-32 位'
  const hasLetter = /[A-Za-z]/.test(pwd)
  const hasDigit = /[0-9]/.test(pwd)
  if (!hasLetter || !hasDigit) return '需包含字母和数字'
  return null
}

async function savePassword() {
  pwdError.value = ''
  const oldV = pwdForm.value.oldPassword
  const newV = pwdForm.value.newPassword
  if (!oldV) {
    pwdError.value = '请输入旧密码'
    return
  }
  const err = validatePwd(newV)
  if (err) {
    pwdError.value = err
    return
  }
  if (oldV === newV) {
    pwdError.value = '新旧密码不能相同'
    return
  }
  savingPwd.value = true
  try {
    await client.put('/users/me/password', {
      oldPassword: oldV,
      newPassword: newV,
    })
    pushToast('密码已修改，请重新登录')
    // clear and redirect after short delay to let toast show
    setTimeout(() => {
      auth.logout()
      router.push('/login')
    }, 600)
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '修改失败'
    pwdError.value = msg
    pushToast(msg)
  } finally {
    savingPwd.value = false
  }
}

// ── preferences ──
const prefsRaw = ref('{}')
const loadingPrefs = ref(false)
const savingPrefs = ref(false)
const prefsError = ref('')

async function fetchPrefs() {
  loadingPrefs.value = true
  prefsError.value = ''
  try {
    const { data } = await client.get('/users/me/preferences')
    const pref: Record<string, unknown> = data?.data ?? data ?? {}
    // ensure object
    const obj = pref && typeof pref === 'object' ? pref : {}
    prefsRaw.value = JSON.stringify(obj, null, 2)
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '加载偏好失败'
    prefsError.value = msg
  } finally {
    loadingPrefs.value = false
  }
}

async function savePrefs() {
  prefsError.value = ''
  let parsed: Record<string, unknown>
  try {
    parsed = JSON.parse(prefsRaw.value || '{}')
    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('偏好需为 JSON 对象')
    }
  } catch (e: unknown) {
    const msg = (e as Error).message || 'JSON 格式错误'
    prefsError.value = msg
    pushToast(msg)
    return
  }
  savingPrefs.value = true
  try {
    const { data } = await client.put('/users/me/preferences', parsed)
    const returned = data?.data ?? data
    if (returned && typeof returned === 'object') {
      prefsRaw.value = JSON.stringify(returned, null, 2)
    }
    pushToast('偏好已保存')
  } catch (e: unknown) {
    const msg =
      (e as { response?: { data?: { message?: string } } })?.response?.data?.message ||
      (e as Error)?.message ||
      '保存偏好失败'
    prefsError.value = msg
    pushToast(msg)
  } finally {
    savingPrefs.value = false
  }
}

function formatPrefsHint(): string {
  return 'PUT /users/me/preferences · 整体覆盖 Map<string,any>'
}

onMounted(async () => {
  await Promise.all([fetchProfile(), fetchPrefs()])
})
</script>

<template>
  <div class="flex-1 min-w-0 bg-[#fbf9f5] p-6 md:p-10 lg:p-12 flex flex-col gap-8">
    <!-- Header -->
    <header class="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b pb-6" style="border-color: #eae8e3">
      <div>
        <h1 class="font-[Newsreader] text-[28px] font-medium tracking-tight text-black">设置</h1>
        <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-[#464742] mt-2">
          Settings · 资料 · 密码 · 偏好 · Atelier 白卡 hairline
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span>
        <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color: #8a8a87">GET /users/me</span>
      </div>
    </header>

    <!-- Card ① Profile -->
    <section class="bg-white rounded-xl p-6 md:p-7 flex flex-col gap-6" style="border: 1px solid #eae8e3">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="w-1.5 h-6 rounded-full bg-black"></span>
          <div>
            <h2 class="font-[Newsreader] text-[18px] font-medium text-black leading-none">资料</h2>
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mt-1" style="color: #8a8a87">
              Profile · PUT /users/me · nickname / phone / email / avatarUrl
            </p>
          </div>
        </div>
        <span
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2.5 py-1 rounded-full bg-[#fbf9f5] hidden sm:inline"
          style="border: 1px solid #eae8e3; color: #464742"
          >CARD 01</span
        >
      </div>

      <div v-if="loadingProfile" class="py-8 flex flex-col gap-3 animate-pulse">
        <div class="h-4 w-32 bg-[#e9e8e4] rounded"></div>
        <div class="h-10 w-full bg-[#f5f3f0] rounded-[10px]"></div>
        <div class="h-10 w-full bg-[#f5f3f0] rounded-[10px]"></div>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-[200px_1fr] gap-8">
        <!-- Avatar preview -->
        <div class="flex flex-col items-center gap-4">
          <div
            class="w-24 h-24 rounded-full overflow-hidden bg-[#f5f3f0] flex items-center justify-center shrink-0"
            style="border: 1px solid #eae8e3"
          >
            <img
              v-if="avatarPreviewValid"
              :src="profileForm.avatarUrl.trim()"
              alt="avatar preview"
              class="w-full h-full object-cover"
              @error="avatarPreviewError = true"
              @load="avatarPreviewError = false"
            />
            <span
              v-else
              class="font-[JetBrains_Mono] text-[24px] font-medium text-[#464742]"
              >{{ (profileForm.nickname || auth.user?.nickname || 'A').trim().slice(0, 1).toUpperCase() }}</span
            >
          </div>
          <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-center" style="color: #8a8a87">
            头像预览<br />
            <span class="normal-case tracking-normal font-[Inter] text-[11px]">Avatar preview · URL</span>
          </p>
          <div
            v-if="avatarPreviewValid"
            class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] uppercase px-2 py-1 rounded-full bg-[#fbf9f5]"
            style="border: 1px solid #eae8e3; color: #464742"
          >
            已加载
          </div>
        </div>

        <!-- Fields -->
        <div class="flex flex-col gap-4">
          <label class="block">
            <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block" style="color: #464742">昵称 Nickname</span>
            <input
              v-model="profileForm.nickname"
              placeholder="输入昵称"
              class="w-full h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
              style="border: 1px solid #eae8e3"
              @focus="($event.target as HTMLInputElement).style.borderColor = '#0f0f0e'"
              @blur="($event.target as HTMLInputElement).style.borderColor = '#eae8e3'"
            />
          </label>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="block">
              <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block" style="color: #464742">手机 Phone</span>
              <input
                v-model="profileForm.phone"
                placeholder="13800000000"
                class="w-full h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
                style="border: 1px solid #eae8e3"
                @focus="($event.target as HTMLInputElement).style.borderColor = '#0f0f0e'"
                @blur="($event.target as HTMLInputElement).style.borderColor = '#eae8e3'"
              />
            </label>
            <label class="block">
              <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block" style="color: #464742">邮箱 Email</span>
              <input
                v-model="profileForm.email"
                placeholder="name@example.com"
                class="w-full h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
                style="border: 1px solid #eae8e3"
                @focus="($event.target as HTMLInputElement).style.borderColor = '#0f0f0e'"
                @blur="($event.target as HTMLInputElement).style.borderColor = '#eae8e3'"
              />
            </label>
          </div>

          <label class="block">
            <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block" style="color: #464742">头像 URL Avatar URL</span>
            <input
              v-model="profileForm.avatarUrl"
              placeholder="https://.../avatar.jpg"
              class="w-full h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition font-[JetBrains_Mono] text-[12px]"
              style="border: 1px solid #eae8e3"
              @input="avatarPreviewError = false"
              @focus="($event.target as HTMLInputElement).style.borderColor = '#0f0f0e'"
              @blur="($event.target as HTMLInputElement).style.borderColor = '#eae8e3'"
            />
            <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.04em] uppercase mt-1.5 block" style="color: #8a8a87">粘贴 https 链接自动预览 · hairline #eae8e3</span>
          </label>

          <div class="flex items-center justify-between pt-2 gap-3">
            <p class="font-[JetBrains_Mono] text-[10px] tracking-[0.04em] uppercase hidden sm:block" style="color: #8a8a87">
              PUT /users/me · UpdateProfileRequest
            </p>
            <button
              @click="saveProfile"
              :disabled="savingProfile"
              class="ml-auto h-9 px-6 rounded-full bg-black text-white text-[13px] font-medium hover:bg-[#1a1a18] disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2 transition-colors shrink-0"
            >
              <span
                v-if="savingProfile"
                class="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block"
              ></span>
              {{ savingProfile ? '保存中…' : '保存资料' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Card ② Password -->
    <section class="bg-white rounded-xl p-6 md:p-7 flex flex-col gap-6" style="border: 1px solid #eae8e3">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="w-1.5 h-6 rounded-full bg-[#FF3B1F]"></span>
          <div>
            <h2 class="font-[Newsreader] text-[18px] font-medium text-black leading-none">改密</h2>
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mt-1" style="color: #8a8a87">
              Password · PUT /users/me/password · 8-32 字母+数字 · 成功需重新登录
            </p>
          </div>
        </div>
        <span
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2.5 py-1 rounded-full bg-[#fbf9f5] hidden sm:inline"
          style="border: 1px solid #eae8e3; color: #464742"
          >CARD 02</span
        >
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <label class="block">
          <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block" style="color: #464742">旧密码 Old Password</span>
          <input
            v-model="pwdForm.oldPassword"
            type="password"
            placeholder="••••••••"
            class="w-full h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
            style="border: 1px solid #eae8e3"
            @focus="($event.target as HTMLInputElement).style.borderColor = '#0f0f0e'"
            @blur="($event.target as HTMLInputElement).style.borderColor = '#eae8e3'"
          />
        </label>
        <label class="block">
          <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mb-1.5 block" style="color: #464742">新密码 New Password</span>
          <input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="8-32 位 字母+数字"
            class="w-full h-[38px] px-3 rounded-[10px] bg-white text-[13px] placeholder:text-[#8a8a87] focus:outline-none transition"
            style="border: 1px solid #eae8e3"
            @focus="($event.target as HTMLInputElement).style.borderColor = '#0f0f0e'"
            @blur="($event.target as HTMLInputElement).style.borderColor = '#eae8e3'"
          />
        </label>
      </div>

      <div
        v-if="pwdError"
        class="bg-[#ffdad6] rounded-[10px] px-3 py-2 flex items-center gap-2"
        style="border: 1px solid #ffb4a6"
      >
        <span class="material-symbols-outlined text-[#ba1a1a] text-[16px]">error</span>
        <span class="text-[13px] text-[#93000a]">{{ pwdError }}</span>
      </div>

      <p class="font-[JetBrains_Mono] text-[10px] leading-[14px] tracking-[0.04em] uppercase" style="color: #8a8a87">
        契约 <span class="text-black">ChangePasswordRequest{oldPassword,newPassword}</span> · 后端旧会话全失效 · 前端需
        <span class="text-black">pushToast</span> 后 <span class="text-black">router.push('/login')</span>
      </p>

      <div class="flex justify-end">
        <button
          @click="savePassword"
          :disabled="savingPwd"
          class="h-9 px-6 rounded-full bg-black text-white text-[13px] font-medium hover:bg-[#1a1a18] disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2 transition-colors"
        >
          <span
            v-if="savingPwd"
            class="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block"
          ></span>
          {{ savingPwd ? '提交中…' : '修改密码' }}
        </button>
      </div>
    </section>

    <!-- Card ③ Preferences -->
    <section class="bg-white rounded-xl p-6 md:p-7 flex flex-col gap-6" style="border: 1px solid #eae8e3">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="w-1.5 h-6 rounded-full bg-[#777871]"></span>
          <div>
            <h2 class="font-[Newsreader] text-[18px] font-medium text-black leading-none">偏好</h2>
            <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase mt-1" style="color: #8a8a87">
              Preferences · GET / PUT /users/me/preferences · Map 整体覆盖
            </p>
          </div>
        </div>
        <span
          class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase px-2.5 py-1 rounded-full bg-[#fbf9f5] hidden sm:inline"
          style="border: 1px solid #eae8e3; color: #464742"
          >CARD 03</span
        >
      </div>

      <div class="flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color: #464742">JSON 文本域 · Preferences JSON</span>
          <span class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase" style="color: #8a8a87">{{ formatPrefsHint() }}</span>
        </div>

        <div v-if="loadingPrefs" class="h-[160px] bg-[#f5f3f0] rounded-[12px] animate-pulse" style="border: 1px solid #eae8e3"></div>
        <textarea
          v-else
          v-model="prefsRaw"
          rows="8"
          spellcheck="false"
          class="w-full min-h-[180px] p-4 bg-[#fbf9f5] rounded-[12px] text-[13px] leading-5 placeholder:text-[#8a8a87] focus:outline-none resize-y font-[JetBrains_Mono] whitespace-pre"
          style="border: 1px solid #eae8e3"
          placeholder='{"theme":"light","language":"zh"}'
          @focus="($event.target as HTMLTextAreaElement).style.borderColor = '#0f0f0e'; ($event.target as HTMLTextAreaElement).style.backgroundColor = '#ffffff'"
          @blur="($event.target as HTMLTextAreaElement).style.borderColor = '#eae8e3'; ($event.target as HTMLTextAreaElement).style.backgroundColor = '#fbf9f5'"
        />

        <div
          v-if="prefsError"
          class="bg-[#ffdad6] rounded-[10px] px-3 py-2 flex items-center gap-2"
          style="border: 1px solid #ffb4a6"
        >
          <span class="material-symbols-outlined text-[#ba1a1a] text-[16px]">error</span>
          <span class="text-[13px] text-[#93000a]">{{ prefsError }}</span>
        </div>

        <div class="flex items-center gap-2 justify-end">
          <button
            @click="fetchPrefs"
            :disabled="loadingPrefs"
            class="h-9 px-5 rounded-full bg-white text-[13px] font-medium hover:border-black hover:text-black transition-colors disabled:opacity-40"
            style="border: 1px solid #eae8e3; color: #464742"
          >
            重新加载
          </button>
          <button
            @click="savePrefs"
            :disabled="savingPrefs"
            class="h-9 px-6 rounded-full bg-black text-white text-[13px] font-medium hover:bg-[#1a1a18] disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2 transition-colors"
          >
            <span
              v-if="savingPrefs"
              class="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block"
            ></span>
            {{ savingPrefs ? '保存中…' : '保存偏好' }}
          </button>
        </div>

        <p class="font-[JetBrains_Mono] text-[10px] leading-[14px] tracking-[0.04em] uppercase" style="color: #8a8a87">
          展示 <span class="text-black">GET /users/me/preferences</span> 结果可直接编辑 · 保存时
          <span class="text-black">JSON.parse</span> 校验后 <span class="text-black">PUT</span> 整体覆盖 · 失败
          <span class="text-black">pushToast</span>
        </p>
      </div>
    </section>

    <!-- Footer hint -->
    <p class="font-[JetBrains_Mono] text-[10px] leading-[14px] tracking-[0.04em] uppercase text-center" style="color: #8a8a87">
      Atelier 白卡 hairline 1px #eae8e3 · rounded-xl · JetBrains Mono · 3 cards
    </p>
  </div>
</template>

<style scoped>
.material-symbols-outlined {
  font-variation-settings:
    'FILL' 0,
    'wght' 300,
    'GRAD' 0,
    'opsz' 20;
}
</style>
