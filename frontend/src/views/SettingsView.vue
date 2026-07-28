<template>
  <MainLayout>
    <main class="min-h-screen bg-surface-container">
      <div class="max-w-[1080px] mx-auto px-margin-page py-stack-lg">
        <header class="flex justify-between items-center mb-stack-lg">
          <div class="flex items-center gap-3">
            <button
              class="w-8 h-8 flex items-center justify-center rounded-lg bg-surface-container-lowest border border-outline-variant text-on-surface-variant hover:text-on-surface transition-colors"
              title="返回"
              @click="router.back()"
            >
              <el-icon size="16">
                <ArrowLeft />
              </el-icon>
            </button>
            <h1 class="text-headline-md font-headline-md text-on-surface">
              账号设置
            </h1>
          </div>
        </header>

        <div class="grid grid-cols-1 lg:grid-cols-[220px_1fr] gap-stack-lg items-start">
          <aside class="lg:sticky lg:top-20 flex flex-col sm:flex-row lg:flex-col gap-1 bg-surface-container-lowest border border-outline-variant rounded-xl p-2">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              :class="[
                'flex items-center gap-2 px-4 py-2.5 rounded-lg text-body-md font-label-md transition-colors text-left whitespace-nowrap',
                activeTab === tab.key
                  ? 'bg-primary text-on-primary'
                  : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
              ]"
              @click="activeTab = tab.key"
            >
              <el-icon size="16">
                <component :is="tab.icon" />
              </el-icon>
              <span>{{ tab.label }}</span>
            </button>
          </aside>

          <section class="flex flex-col gap-stack-lg">
            <!-- 个人资料 -->
            <div
              v-if="activeTab === 'profile'"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm"
            >
              <h2 class="text-title-lg font-title-lg text-on-surface">
                个人资料
              </h2>
              <p class="text-body-md text-on-surface-variant mt-1">
                管理你的头像、昵称和联系方式
              </p>

              <div class="flex items-center gap-4 my-6 p-4 bg-surface-container-low rounded-lg">
                <img
                  v-if="userStore.avatar"
                  class="w-[72px] h-[72px] rounded-full object-cover border-2 border-outline-variant"
                  :src="userStore.avatar"
                  alt="avatar"
                >
                <div
                  v-else
                  class="w-[72px] h-[72px] rounded-full flex items-center justify-center bg-primary text-on-primary text-2xl font-bold border-2 border-outline-variant"
                >
                  {{ userInitial }}
                </div>
                <div class="flex flex-col gap-2">
                  <button
                    class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform w-fit"
                    @click="goAvatarUpload"
                  >
                    <el-icon size="14">
                      <Upload />
                    </el-icon>
                    <span>上传头像</span>
                  </button>
                  <p class="text-label-md text-on-surface-variant">
                    支持 JPG / PNG / WEBP，≤ 10 MB
                  </p>
                </div>
              </div>

              <div class="flex flex-col gap-1.5 mt-4">
                <label class="text-label-md font-label-md text-on-surface-variant">昵称</label>
                <input
                  v-model="profileForm.nickname"
                  type="text"
                  class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="请输入昵称"
                >
              </div>

              <div class="flex flex-col gap-1.5 mt-4">
                <label class="text-label-md font-label-md text-on-surface-variant">手机号</label>
                <input
                  v-model="profileForm.phone"
                  type="tel"
                  class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="请输入手机号"
                >
              </div>

              <div class="flex flex-col gap-1.5 mt-4">
                <label class="text-label-md font-label-md text-on-surface-variant">邮箱</label>
                <input
                  v-model="profileForm.email"
                  type="email"
                  class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="请输入邮箱"
                >
              </div>

              <div class="flex justify-end mt-6">
                <button
                  class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-55 disabled:cursor-not-allowed"
                  :disabled="saving"
                  @click="saveProfile"
                >
                  <el-icon
                    v-if="saving"
                    class="animate-spin"
                    size="14"
                  >
                    <Loading />
                  </el-icon>
                  <span>{{ saving ? '保存中...' : '保存修改' }}</span>
                </button>
              </div>
            </div>

            <!-- 账号安全 -->
            <div
              v-if="activeTab === 'security'"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm"
            >
              <h2 class="text-title-lg font-title-lg text-on-surface">
                账号安全
              </h2>
              <p class="text-body-md text-on-surface-variant mt-1">
                修改密码并管理登录状态
              </p>

              <div class="flex flex-col gap-1.5 mt-4">
                <label class="text-label-md font-label-md text-on-surface-variant">当前密码</label>
                <input
                  v-model="securityForm.oldPassword"
                  type="password"
                  class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="请输入当前密码"
                >
              </div>

              <div class="flex flex-col gap-1.5 mt-4">
                <label class="text-label-md font-label-md text-on-surface-variant">新密码</label>
                <input
                  v-model="securityForm.newPassword"
                  type="password"
                  class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="请输入 6-32 位新密码"
                >
              </div>

              <div class="flex flex-col gap-1.5 mt-4">
                <label class="text-label-md font-label-md text-on-surface-variant">确认新密码</label>
                <input
                  v-model="securityForm.confirmPassword"
                  type="password"
                  class="w-full bg-surface-container-low border-none rounded-lg px-3 py-2 text-body-md focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="请再次输入新密码"
                >
              </div>

              <div class="flex justify-end mt-6">
                <button
                  class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-55 disabled:cursor-not-allowed"
                  :disabled="saving"
                  @click="savePassword"
                >
                  <el-icon
                    v-if="saving"
                    class="animate-spin"
                    size="14"
                  >
                    <Loading />
                  </el-icon>
                  <span>{{ saving ? '保存中...' : '修改密码' }}</span>
                </button>
              </div>
            </div>

            <!-- 偏好设置 -->
            <div
              v-if="activeTab === 'preference'"
              class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 shadow-sm"
            >
              <h2 class="text-title-lg font-title-lg text-on-surface">
                偏好设置
              </h2>
              <p class="text-body-md text-on-surface-variant mt-1">
                自定义通知和默认行为
              </p>

              <div
                v-for="(item, idx) in preferenceList"
                :key="item.key"
                class="flex items-center justify-between gap-4 py-4 border-b border-outline-variant/30"
                :class="{ 'border-b-0': idx === preferenceList.length - 1 }"
              >
                <div>
                  <div class="text-body-md font-label-md text-on-surface">
                    {{ item.title }}
                  </div>
                  <div class="text-label-md text-on-surface-variant mt-0.5">
                    {{ item.hint }}
                  </div>
                </div>
                <el-switch v-model="preferenceForm[item.key as keyof typeof preferenceForm]" />
              </div>

              <div class="flex justify-end mt-6">
                <button
                  class="bg-primary text-on-primary px-4 py-2 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform disabled:opacity-55 disabled:cursor-not-allowed"
                  :disabled="saving"
                  @click="savePreference"
                >
                  <el-icon
                    v-if="saving"
                    class="animate-spin"
                    size="14"
                  >
                    <Loading />
                  </el-icon>
                  <span>{{ saving ? '保存中...' : '保存偏好' }}</span>
                </button>
              </div>
            </div>

            <!-- 退出登录 -->
            <div class="bg-surface-container-lowest rounded-xl border border-error/25 p-6 shadow-sm">
              <h2 class="text-title-lg font-title-lg text-error">
                危险操作
              </h2>
              <p class="text-body-md text-on-surface-variant mt-1">
                退出后需要重新登录才能访问你的简历
              </p>
              <button
                class="bg-error text-on-error px-4 py-2 rounded-lg font-label-md flex items-center gap-2 mt-6 hover:scale-[0.98] transition-transform"
                @click="handleLogout"
              >
                <el-icon size="14">
                  <SwitchButton />
                </el-icon>
                <span>退出登录</span>
              </button>
            </div>
          </section>
        </div>
      </div>
    </main>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  User,
  Lock,
  Setting,
  Upload,
  Loading,
  SwitchButton
} from '@element-plus/icons-vue'
import MainLayout from '@/components/layout/MainLayout.vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'profile' | 'security' | 'preference'>('profile')
const saving = ref(false)

const tabs = [
  { key: 'profile', label: '个人资料', icon: User },
  { key: 'security', label: '账号安全', icon: Lock },
  { key: 'preference', label: '偏好设置', icon: Setting }
] as const

const preferenceList = [
  {
    key: 'emailNotify' as const,
    title: '接收邮件通知',
    hint: '简历导出完成、AI 点评完成时发送邮件'
  },
  {
    key: 'autoSaveNotify' as const,
    title: '自动保存提醒',
    hint: '编辑器自动保存失败时弹出提示'
  },
  {
    key: 'keepGuestData' as const,
    title: '游客模式保留数据',
    hint: '注册后将临时简历迁移到正式账号'
  }
]

const userInitial = computed(() => {
  const name = userStore.nickname || '用户'
  return name.charAt(0).toUpperCase()
})

const profileForm = ref({
  nickname: userStore.nickname || '',
  phone: '',
  email: ''
})

const securityForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const preferenceForm = ref({
  emailNotify: true,
  autoSaveNotify: true,
  keepGuestData: true
})

onMounted(() => {
  profileForm.value.nickname = userStore.nickname || ''
})

function goAvatarUpload() {
  router.push('/avatar/upload')
}

async function saveProfile() {
  saving.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 600))
    userStore.setUser({
      userId: userStore.userId || '',
      nickname: profileForm.value.nickname,
      accessToken: userStore.accessToken,
      isGuest: userStore.isGuest
    })
    ElMessage.success('个人资料已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  if (!securityForm.value.oldPassword || !securityForm.value.newPassword) {
    ElMessage.warning('请输入当前密码和新密码')
    return
  }
  if (securityForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于 6 位')
    return
  }
  if (securityForm.value.newPassword !== securityForm.value.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 600))
    ElMessage.success('密码修改成功')
    securityForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e: any) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    saving.value = false
  }
}

async function savePreference() {
  saving.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 400))
    ElMessage.success('偏好设置已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.clearUser()
    router.push('/login')
  })
}
</script>
