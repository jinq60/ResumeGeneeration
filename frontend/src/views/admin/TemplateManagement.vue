<template>
  <div class="template-management">
    <div class="page-header">
      <h1>模板管理</h1>
      <el-button
        type="primary"
        @click="handleAdd"
      >
        <el-icon><Plus /></el-icon>
        新增模板
      </el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索模板名称或编码"
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
          label="已上架"
          value="active"
        />
        <el-option
          label="已下架"
          value="inactive"
        />
      </el-select>
      <el-button
        type="primary"
        @click="handleSearch"
      >
        搜索
      </el-button>
    </div>

    <div class="template-list">
      <el-table
        v-loading="loading"
        :data="templateList"
      >
        <el-table-column
          label="缩略图"
          width="120"
        >
          <template #default="{ row }">
            <div class="thumbnail">
              <img
                v-if="row.thumbnailUrl"
                :src="row.thumbnailUrl"
                alt="缩略图"
              >
              <div
                v-else
                class="no-thumbnail"
              >
                暂无图片
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="code"
          label="模板编码"
          width="150"
        />
        <el-table-column
          prop="name"
          label="模板名称"
          width="200"
        />
        <el-table-column
          prop="category"
          label="分类"
          width="120"
        />
        <el-table-column
          prop="sortOrder"
          label="排序"
          width="80"
        />
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">
              {{ row.status === 'active' ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="创建时间"
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
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '下架' : '上架' }}
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="templateFormRef"
        :model="templateForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="模板编码"
          prop="code"
        >
          <el-input
            v-model="templateForm.code"
            placeholder="请输入模板编码"
          />
        </el-form-item>
        <el-form-item
          label="模板名称"
          prop="name"
        >
          <el-input
            v-model="templateForm.name"
            placeholder="请输入模板名称"
          />
        </el-form-item>
        <el-form-item
          label="模板分类"
          prop="category"
        >
          <el-select
            v-model="templateForm.category"
            placeholder="请选择分类"
          >
            <el-option
              label="简约风格"
              value="simple"
            />
            <el-option
              label="商务风格"
              value="business"
            />
            <el-option
              label="创意风格"
              value="creative"
            />
            <el-option
              label="学术风格"
              value="academic"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="缩略图"
          prop="thumbnailUrl"
        >
          <el-upload
            class="thumbnail-uploader"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
          >
            <img
              v-if="templateForm.thumbnailUrl"
              :src="templateForm.thumbnailUrl"
              class="thumbnail"
            >
            <el-icon
              v-else
              class="uploader-icon"
            >
              <Plus />
            </el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item
          label="模板描述"
          prop="description"
        >
          <el-input
            v-model="templateForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
          />
        </el-form-item>
        <el-form-item
          label="排序"
          prop="sortOrder"
        >
          <el-input-number
            v-model="templateForm.sortOrder"
            :min="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadProps } from 'element-plus'

interface TemplateItem {
  id: string
  code: string
  name: string
  category: string
  thumbnailUrl: string
  status: string
  sortOrder: number
  createdAt: string
}

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const templateList = ref<TemplateItem[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增模板')
const submitLoading = ref(false)
const templateFormRef = ref<FormInstance>()

const templateForm = reactive({
  id: '',
  code: '',
  name: '',
  category: '',
  thumbnailUrl: '',
  description: '',
  sortOrder: 0
})

const formRules: FormRules = {
  code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择模板分类', trigger: 'change' }]
}

onMounted(() => {
  loadTemplateList()
})

const loadTemplateList = () => {
  loading.value = true
  // TODO: 调用API获取模板列表
  setTimeout(() => {
    templateList.value = [
      {
        id: '1',
        code: 'simple_blue',
        name: '简约蓝',
        category: '简约风格',
        thumbnailUrl: '',
        status: 'active',
        sortOrder: 1,
        createdAt: '2024-01-15 10:30:00'
      },
      {
        id: '2',
        code: 'business_gray',
        name: '商务灰',
        category: '商务风格',
        thumbnailUrl: '',
        status: 'active',
        sortOrder: 2,
        createdAt: '2024-01-16 14:20:00'
      }
    ]
    loading.value = false
  }, 500)
}

const handleSearch = () => {
  loadTemplateList()
}

const handleAdd = () => {
  dialogTitle.value = '新增模板'
  dialogVisible.value = true
  resetForm()
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑模板'
  dialogVisible.value = true
  Object.assign(templateForm, row)
}

const handleToggleStatus = (row: any) => {
  const action = row.status === 'active' ? '下架' : '上架'
  ElMessageBox.confirm(`确定要${action}该模板吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用API更新状态
    ElMessage.success(`${action}成功`)
    loadTemplateList()
  })
}

const handleDelete = () => {
  ElMessageBox.confirm('确定要删除该模板吗？此操作不可恢复。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // TODO: 调用API删除模板
    ElMessage.success('删除成功')
    loadTemplateList()
  })
}

const handleSubmit = async () => {
  if (!templateFormRef.value) return
  
  await templateFormRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      // TODO: 调用API保存模板
      setTimeout(() => {
        submitLoading.value = false
        dialogVisible.value = false
        ElMessage.success('保存成功')
        loadTemplateList()
      }, 500)
    }
  })
}

const handleDialogClose = () => {
  resetForm()
}

const resetForm = () => {
  templateFormRef.value?.resetFields()
  Object.assign(templateForm, {
    id: '',
    code: '',
    name: '',
    category: '',
    thumbnailUrl: '',
    description: '',
    sortOrder: 0
  })
}

const beforeUpload: UploadProps['beforeUpload'] = () => {
  // TODO: 文件上传前的验证
  return true
}

const handleUploadSuccess: UploadProps['onSuccess'] = (response) => {
  // TODO: 处理上传成功
  templateForm.thumbnailUrl = response.url
}
</script>

<style scoped>
.template-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.template-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.thumbnail {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #eee;
}

.no-thumbnail {
  width: 80px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  color: #999;
}

.thumbnail-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
}

.thumbnail-uploader:hover {
  border-color: #409eff;
}

.thumbnail-uploader .thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
