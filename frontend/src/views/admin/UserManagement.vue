<template>
  <div class="admin-user-management">
    <!-- Header -->
    <div class="mb-8">
      <h2 class="text-headline-md font-bold">
        用户管理
      </h2>
      <p class="text-body-md text-on-surface-variant mt-2">
        管理平台注册用户，查看用户信息、会员状态与使用情况
      </p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-gutter mb-8">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant flex items-center gap-4 hover:shadow-md transition-shadow"
      >
        <div
          class="w-12 h-12 rounded-xl flex items-center justify-center text-[28px]"
          :class="stat.iconBg"
        >
          <el-icon
            :class="stat.iconColor"
            size="28"
          >
            <component :is="stat.icon" />
          </el-icon>
        </div>
        <div>
          <p class="text-label-md text-on-surface-variant">
            {{ stat.label }}
          </p>
          <div class="flex items-baseline gap-2 mt-1">
            <span class="text-headline-md font-bold">{{ stat.value }}</span>
            <span class="text-[12px] text-secondary flex items-center font-bold">
              <el-icon size="14"><ArrowUp /></el-icon>{{ stat.growth }}
            </span>
          </div>
          <p class="text-[11px] text-outline mt-1">
            {{ stat.compare }}
          </p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 mb-8">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-gutter">
        <div class="space-y-2">
          <label class="text-label-md font-bold text-on-surface-variant">关键词搜索</label>
          <el-input
            v-model="filters.keyword"
            placeholder="搜索用户昵称、手机号、邮箱"
            clearable
          >
            <template #prefix>
              <el-icon size="18">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
        <div class="space-y-2">
          <label class="text-label-md font-bold text-on-surface-variant">账户状态</label>
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
          >
            <el-option
              label="全部状态"
              value=""
            />
            <el-option
              label="正常"
              value="active"
            />
            <el-option
              label="已禁用"
              value="disabled"
            />
          </el-select>
        </div>
        <div class="space-y-2 flex flex-col justify-end">
          <div class="flex gap-2">
            <el-button
              type="primary"
              class="flex-1"
              @click="handleSearch"
            >
              <el-icon size="16">
                <Search />
              </el-icon>查询
            </el-button>
            <el-button
              class="px-3"
              @click="resetFilters"
            >
              重置
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Actions -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <el-button
          type="primary"
          @click="createDialogVisible = true"
        >
          <el-icon size="18">
            <Plus />
          </el-icon>新增用户
        </el-button>
        <el-divider direction="vertical" />
        <el-dropdown @command="handleBatchCommand">
          <el-button :loading="batchLoading">
            批量操作<el-icon
              class="ml-1"
              size="16"
            >
              <ArrowDown />
            </el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                command="enable"
                :disabled="selectedIds.length === 0"
              >
                批量启用
              </el-dropdown-item>
              <el-dropdown-item
                command="disable"
                :disabled="selectedIds.length === 0"
              >
                批量禁用
              </el-dropdown-item>
              <el-dropdown-item
                command="delete"
                :disabled="selectedIds.length === 0"
                divided
              >
                批量删除
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <span class="text-label-md text-outline ml-2">已选择 {{ selectedIds.length }} 项</span>
      </div>
      <div class="flex items-center gap-2">
        <el-button
          :loading="exporting"
          @click="handleExport"
        >
          <el-icon size="16">
            <Download />
          </el-icon>导出
        </el-button>
        <el-button @click="loadUserList">
          <el-icon size="16">
            <Refresh />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- Table -->
    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden shadow-sm">
      <el-table
        v-loading="loading"
        :data="userList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="48"
        />
        <el-table-column
          label="用户ID"
          prop="userId"
          width="120"
        >
          <template #default="{ row }">
            <span class="text-body-md text-outline font-mono">{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="用户昵称"
          min-width="160"
        >
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <el-avatar
                :size="32"
                :src="row.avatarUrl || ''"
              >
                <el-icon size="16">
                  <UserIcon />
                </el-icon>
              </el-avatar>
              <span class="text-body-md font-bold">{{ row.nickname || '未设置昵称' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="手机号 / 邮箱"
          min-width="180"
        >
          <template #default="{ row }">
            {{ formatContact(row as User) }}
          </template>
        </el-table-column>
        <el-table-column
          label="角色"
          width="130"
          align="center"
        >
          <template #default="{ row }">
            <el-dropdown
              trigger="click"
              @command="(cmd: string) => handleRoleChange(row as User, cmd as 'USER' | 'ADMIN')"
            >
              <span
                class="px-2 py-0.5 rounded text-[10px] font-bold cursor-pointer hover:opacity-80 inline-flex items-center gap-1"
                :class="roleClass(row.role)"
                title="点击调整角色"
              >
                {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
                <el-icon size="10"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    command="USER"
                    :disabled="row.role === 'USER'"
                  >
                    设为普通用户
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="ADMIN"
                    :disabled="row.role === 'ADMIN'"
                  >
                    设为管理员
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <el-table-column
          label="类型"
          prop="isGuest"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            {{ row.isGuest ? '游客' : '注册用户' }}
          </template>
        </el-table-column>
        <el-table-column
          label="注册时间"
          prop="createdAt"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="账户状态"
          width="110"
        >
          <template #default="{ row }">
            <span
              class="flex items-center gap-1.5 text-[12px] font-bold"
              :class="statusClass(row.status)"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="statusDotClass(row.status)"
              />
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="170"
          align="right"
          fixed="right"
        >
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-3">
              <el-button
                link
                type="primary"
                @click="handleViewDetail(row as User)"
              >
                查看
              </el-button>
              <el-button
                link
                type="warning"
                @click="handleResetPassword(row as User)"
              >
                重置密码
              </el-button>
              <el-dropdown>
                <el-icon
                  class="text-on-surface-variant hover:text-primary cursor-pointer"
                  size="18"
                >
                  <More />
                </el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleViewDetail(row as User)">
                      详情
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleToggleStatus(row as User)">
                      {{ row.status === 'active' ? '禁用' : '启用' }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="px-6 py-4 flex items-center justify-between bg-surface-container-low border-t border-outline-variant">
        <span class="text-label-md text-on-surface-variant">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailVisible"
      title="用户详情"
      width="600px"
    >
      <div
        v-if="currentUser"
        class="space-y-6"
      >
        <div class="flex items-center gap-4 pb-6 border-b border-outline-variant">
          <el-avatar
            :size="64"
            :src="currentUser.avatarUrl || ''"
          >
            <el-icon size="24">
              <User />
            </el-icon>
          </el-avatar>
          <div>
            <h3 class="text-title-md font-bold">
              {{ currentUser.nickname || '未设置昵称' }}
            </h3>
            <span class="text-label-md text-on-surface-variant">{{ currentUser.userId }}</span>
          </div>
        </div>
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item label="手机号 / 邮箱">
            {{ formatContact(currentUser) }}
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            {{ currentUser.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-descriptions-item>
          <el-descriptions-item label="账户类型">
            {{ currentUser.isGuest ? '游客' : '注册用户' }}
          </el-descriptions-item>
          <el-descriptions-item label="账户状态">
            <el-tag :type="currentUser.status === 'active' ? 'success' : 'info'">
              {{ statusLabel(currentUser.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatDate(currentUser.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="最近更新">
            {{ formatDate(currentUser.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 新增用户 Dialog -->
    <el-dialog
      v-model="createDialogVisible"
      title="新增用户"
      width="520px"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item
          label="邮箱"
          prop="email"
        >
          <el-input
            v-model="createForm.email"
            placeholder="登录账号（必填）"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="createForm.nickname"
            placeholder="昵称（可选）"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="createForm.phone"
            placeholder="手机号（可选）"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input
            v-model="createForm.initialPassword"
            type="password"
            show-password
            placeholder="留空则自动生成临时密码"
            maxlength="32"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="creating"
          @click="submitCreate"
        >
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 临时密码 Dialog -->
    <el-dialog
      v-model="tempPasswordVisible"
      title="用户创建成功"
      width="480px"
    >
      <div class="space-y-4">
        <p class="text-body-md text-on-surface-variant">
          用户 {{ createdResult?.email }} 已创建。
        </p>
        <div
          v-if="createdResult?.temporaryPassword"
          class="p-4 bg-surface-container-low rounded-lg border border-outline-variant"
        >
          <p class="text-label-md font-bold text-on-surface-variant mb-2">
            一次性临时密码（仅显示一次，请立即转交用户）
          </p>
          <p class="text-headline-md font-bold text-primary font-mono">
            {{ createdResult.temporaryPassword }}
          </p>
        </div>
        <p class="text-body-md text-on-surface-variant">
          {{ createdResult?.message }}
        </p>
      </div>
      <template #footer>
        <el-button
          type="primary"
          @click="tempPasswordVisible = false"
        >
          知道了
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码结果 Dialog -->
    <el-dialog
      v-model="resetPasswordVisible"
      title="密码重置成功"
      width="480px"
    >
      <div class="space-y-4">
        <p class="text-body-md text-on-surface-variant">
          已为用户 {{ resetTargetLabel }} 重置密码，其原有登录会话已全部失效。
        </p>
        <div class="p-4 bg-surface-container-low rounded-lg border border-outline-variant">
          <p class="text-label-md font-bold text-on-surface-variant mb-2">
            一次性临时密码（仅显示一次，请立即转交用户）
          </p>
          <p class="text-headline-md font-bold text-primary font-mono">
            {{ resetResult?.temporaryPassword }}
          </p>
        </div>
        <p class="text-body-md text-on-surface-variant">
          请提醒用户使用临时密码登录后立即修改。
        </p>
      </div>
      <template #footer>
        <el-button
          type="primary"
          @click="resetPasswordVisible = false"
        >
          知道了
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { userApi, type User, type UserStats, type CreateUserResponse, type ResetPasswordResponse } from '@/api/admin/users'
import { adminDownload } from '@/utils/adminDownload'
import {
  User as UserIcon,
  Search,
  Plus,
  ArrowDown,
  Download,
  Refresh,
  More,
  ArrowUp,
  UserFilled,
  StarFilled,
  WarningFilled
} from '@element-plus/icons-vue'

const loading = ref(false)
const selectedIds = ref<string[]>([])
const batchLoading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const route = useRoute()

const filters = reactive({
  keyword: (route.query.keyword as string) || '',
  status: ''
})

const userList = ref<User[]>([])
const detailVisible = ref(false)
const currentUser = ref<User | null>(null)

const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  email: '',
  nickname: '',
  phone: '',
  initialPassword: ''
})
const createRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const tempPasswordVisible = ref(false)
const createdResult = ref<CreateUserResponse | null>(null)
const exporting = ref(false)

const resetPasswordVisible = ref(false)
const resetResult = ref<ResetPasswordResponse | null>(null)
const resetTargetLabel = ref('')

const stats = reactive([
  { label: '用户总数', value: '-', growth: '', compare: '', icon: UserIcon, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '今日新增', value: '-', growth: '', compare: '', icon: UserFilled, iconBg: 'bg-secondary/10', iconColor: 'text-secondary' },
  { label: '正式用户', value: '-', growth: '', compare: '', icon: StarFilled, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '禁用账号', value: '-', growth: '', compare: '', icon: WarningFilled, iconBg: 'bg-error/10', iconColor: 'text-error' }
])

function statusClass(status: string) {
  switch (status) {
    case 'active': return 'text-secondary'
    case 'disabled': return 'text-outline'
    default: return 'text-on-surface-variant'
  }
}
function statusDotClass(status: string) {
  switch (status) {
    case 'active': return 'bg-secondary'
    case 'disabled': return 'bg-outline'
    default: return 'bg-outline'
  }
}
function statusLabel(status: string) {
  switch (status) {
    case 'active': return '正常'
    case 'disabled': return '已禁用'
    default: return status
  }
}
function roleClass(role: string) {
  if (role === 'ADMIN') return 'bg-tertiary/10 text-tertiary'
  return 'bg-surface-container text-on-surface-variant'
}
function formatContact(row: User) {
  return row.phone || row.email || '-'
}
function formatDate(date: string | null) {
  return date ? date.replace('T', ' ').substring(0, 19) : '-'
}

function handleSelectionChange(rows: User[]) {
  selectedIds.value = rows.map(r => r.userId)
}

async function handleViewDetail(row: User) {
  try {
    currentUser.value = await userApi.getUser(row.userId)
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '获取用户详情失败')
  }
}

async function handleToggleStatus(row: User) {
  const nextStatus = row.status === 'active' ? 'disabled' : 'active'
  const action = nextStatus === 'active' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', { type: 'warning' })
    await userApi.updateUserStatus(row.userId, nextStatus)
    ElMessage.success(`${action}成功`)
    loadUserList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || `${action}失败`)
    }
  }
}

async function handleRoleChange(row: User, role: 'USER' | 'ADMIN') {
  if (row.role === role) return
  const label = role === 'ADMIN' ? '管理员' : '普通用户'
  try {
    await ElMessageBox.confirm(
      `确定将该用户调整为「${label}」吗？${role === 'USER' ? '降权后其已有的管理端会话将随令牌过期失效。' : ''}`,
      '调整角色',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await userApi.updateUserRole(row.userId, role)
    row.role = role
    ElMessage.success(`角色已调整为${label}`)
  } catch (e: any) {
    ElMessage.error(e.message || '角色调整失败')
  }
}

async function handleResetPassword(row: User) {
  const label = row.nickname || formatContact(row)
  try {
    await ElMessageBox.confirm(
      `确定重置用户「${label}」的密码吗？原密码将立即失效。`,
      '重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    resetResult.value = await userApi.resetUserPassword(row.userId)
    resetTargetLabel.value = label
    resetPasswordVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '重置密码失败')
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  page.value = 1
  loadUserList()
}

/** 查询：先回到第一页再加载，避免筛选结果落在后续页 */
function handleSearch() {
  if (page.value === 1) {
    loadUserList()
  } else {
    page.value = 1
  }
}

async function loadUserList() {
  loading.value = true
  try {
    const res = await userApi.getUsers({
      page: page.value,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    userList.value = res.records
    total.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const s: UserStats = await userApi.getUserStats()
    stats[0].value = String(s.totalUsers)
    stats[1].value = String(s.todayNewUsers)
    stats[2].value = String(s.registeredUsers)
    stats[3].value = String(s.disabledUsers)
  } catch (e: any) {
    ElMessage.error(e.message || '加载统计数据失败')
  }
}

async function submitCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  creating.value = true
  try {
    const result = await userApi.createUser({
      email: createForm.email.trim(),
      nickname: createForm.nickname.trim() || undefined,
      phone: createForm.phone.trim() || undefined,
      initialPassword: createForm.initialPassword || undefined
    })
    createdResult.value = result
    createDialogVisible.value = false
    createForm.email = ''
    createForm.nickname = ''
    createForm.phone = ''
    createForm.initialPassword = ''
    tempPasswordVisible.value = true
    loadUserList()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function handleBatchCommand(command: string) {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个用户吗？`, '批量删除', { type: 'warning' })
    } catch {
      return
    }
  }
  if (batchLoading.value) return
  batchLoading.value = true
  // 逐条执行但收集每个 id 的成败，避免中途失败即中断导致剩余项被跳过
  const failedIds: string[] = []
  try {
    for (const id of selectedIds.value) {
      try {
        if (command === 'enable' || command === 'disable') {
          await userApi.updateUserStatus(id, command === 'enable' ? 'active' : 'disabled')
        } else if (command === 'delete') {
          await userApi.deleteUser(id)
        }
      } catch {
        failedIds.push(id)
      }
    }
    const successCount = selectedIds.value.length - failedIds.length
    if (failedIds.length === 0) {
      ElMessage.success(`批量操作完成：${successCount} 成功`)
    } else {
      ElMessage.warning(`批量操作完成：${successCount} 成功 / ${failedIds.length} 失败（${failedIds.join('、')}）`)
    }
    loadUserList()
  } finally {
    batchLoading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const fileName = await adminDownload(
      userApi.exportUrl({ status: filters.status || undefined, keyword: filters.keyword || undefined }),
      '用户数据.csv'
    )
    ElMessage.success(`已导出 ${fileName}`)
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

watch([page, pageSize], loadUserList)

onMounted(() => {
  loadStats()
  loadUserList()
})
</script>

<style scoped lang="scss">
.admin-user-management {
  padding-bottom: var(--st-margin-page);
}
</style>
