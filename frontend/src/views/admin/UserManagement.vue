<template>
  <div class="user-management">
    <div class="page-header">
      <h1>用户管理</h1>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名、邮箱或手机号"
        clearable
        style="width: 300px"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select
        v-model="statusFilter"
        placeholder="状态筛选"
        clearable
        style="width: 150px; margin-left: 10px"
      >
        <el-option
          label="全部"
          value=""
        />
        <el-option
          label="正常"
          value="active"
        />
        <el-option
          label="禁用"
          value="inactive"
        />
      </el-select>
      <el-select
        v-model="typeFilter"
        placeholder="用户类型"
        clearable
        style="width: 150px; margin-left: 10px"
      >
        <el-option
          label="全部"
          value=""
        />
        <el-option
          label="注册用户"
          value="registered"
        />
        <el-option
          label="游客"
          value="guest"
        />
      </el-select>
      <el-button
        type="primary"
        @click="handleSearch"
      >
        搜索
      </el-button>
    </div>

    <div class="user-list">
      <el-table
        v-loading="loading"
        :data="userList"
      >
        <el-table-column
          label="头像"
          width="80"
        >
          <template #default="{ row }">
            <el-avatar
              :size="40"
              :src="row.avatarUrl"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column
          prop="nickname"
          label="昵称"
          width="150"
        />
        <el-table-column
          prop="email"
          label="邮箱"
          width="200"
        />
        <el-table-column
          prop="phone"
          label="手机号"
          width="130"
        />
        <el-table-column
          label="用户类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.isGuest ? 'info' : 'primary'"
              size="small"
            >
              {{ row.isGuest ? '游客' : '注册用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'active' ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="resumeCount"
          label="简历数"
          width="80"
        />
        <el-table-column
          prop="createdAt"
          label="注册时间"
          width="180"
        />
        <el-table-column
          label="操作"
          fixed="right"
          width="200"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="用户详情"
      width="600px"
    >
      <div
        v-if="currentUser"
        class="user-detail"
      >
        <div class="detail-header">
          <el-avatar
            :size="80"
            :src="currentUser.avatarUrl"
          >
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="user-info">
            <h3>{{ currentUser.nickname }}</h3>
            <el-tag :type="currentUser.isGuest ? 'info' : 'primary'">
              {{ currentUser.isGuest ? '游客' : '注册用户' }}
            </el-tag>
          </div>
        </div>
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item label="用户ID">
            {{ currentUser.id }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ currentUser.email }}
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            {{ currentUser.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentUser.status === 'active' ? 'success' : 'danger'">
              {{ currentUser.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="简历数量">
            {{ currentUser.resumeCount }}
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ currentUser.createdAt }}
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">
            {{ currentUser.lastLoginAt }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'

interface UserItem {
  id: string
  nickname: string
  email: string
  phone: string
  avatarUrl: string
  isGuest: boolean
  status: string
  resumeCount: number
  createdAt: string
  lastLoginAt: string
}

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const userList = ref<UserItem[]>([])

const detailDialogVisible = ref(false)
const currentUser = ref<any>(null)

onMounted(() => {
  loadUserList()
})

const loadUserList = () => {
  loading.value = true
  // TODO: 调用API获取用户列表
  setTimeout(() => {
    userList.value = [
      {
        id: 'user_001',
        nickname: '张三',
        email: 'zhangsan@example.com',
        phone: '138****8000',
        avatarUrl: '',
        isGuest: false,
        status: 'active',
        resumeCount: 5,
        createdAt: '2024-01-10 09:30:00',
        lastLoginAt: '2024-01-15 08:20:00'
      },
      {
        id: 'user_002',
        nickname: '李四',
        email: 'lisi@example.com',
        phone: '139****9000',
        avatarUrl: '',
        isGuest: false,
        status: 'active',
        resumeCount: 3,
        createdAt: '2024-01-12 14:20:00',
        lastLoginAt: '2024-01-14 16:45:00'
      },
      {
        id: 'guest_001',
        nickname: '游客001',
        email: '',
        phone: '',
        avatarUrl: '',
        isGuest: true,
        status: 'active',
        resumeCount: 1,
        createdAt: '2024-01-15 10:00:00',
        lastLoginAt: '2024-01-15 10:00:00'
      }
    ]
    loading.value = false
  }, 500)
}

const handleSearch = () => {
  loadUserList()
}

const handleViewDetail = (row: any) => {
  currentUser.value = row
  detailDialogVisible.value = true
}

const handleToggleStatus = (row: any) => {
  const action = row.status === 'active' ? '禁用' : '启用'
  ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用API更新用户状态
    ElMessage.success(`${action}成功`)
    loadUserList()
  })
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.user-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-detail {
  padding: 10px 0;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.user-info {
  margin-left: 20px;
  flex: 1;
}

.user-info h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 10px 0;
}
</style>
