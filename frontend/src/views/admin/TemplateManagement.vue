<template>
  <div class="admin-template-management">
    <div class="mb-8">
      <h2 class="text-headline-md font-bold">
        模板管理
      </h2>
      <p class="text-body-md text-on-surface-variant mt-2">
        管理平台简历模板，控制上架状态与排序
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
          class="w-12 h-12 rounded-xl flex items-center justify-center"
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
          <p class="text-headline-md font-bold mt-1">
            {{ stat.value }}
          </p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 mb-8">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-gutter">
        <div class="space-y-2">
          <label class="text-label-md font-bold text-on-surface-variant">关键词搜索</label>
          <el-input
            v-model="filters.keyword"
            placeholder="搜索模板名称或编码"
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
          <label class="text-label-md font-bold text-on-surface-variant">模板分类</label>
          <el-select
            v-model="filters.category"
            placeholder="全部分类"
            clearable
          >
            <el-option
              label="全部"
              value=""
            />
            <el-option
              label="经典"
              value="classic"
            />
            <el-option
              label="技术"
              value="tech"
            />
            <el-option
              label="应届生"
              value="fresh"
            />
            <el-option
              label="商务"
              value="business"
            />
            <el-option
              label="学术"
              value="postgraduate"
            />
          </el-select>
        </div>
        <div class="space-y-2">
          <label class="text-label-md font-bold text-on-surface-variant">状态</label>
          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
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
        </div>
        <div class="space-y-2 flex flex-col justify-end">
          <div class="flex gap-2">
            <el-button
              type="primary"
              class="flex-1"
              @click="loadTemplates"
            >
              <el-icon size="16">
                <Search />
              </el-icon>查询
            </el-button>
            <el-button @click="resetFilters">
              重置
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Actions -->
    <div class="flex items-center justify-between mb-4">
      <el-button
        type="primary"
        @click="handleAdd"
      >
        <el-icon size="18">
          <Plus />
        </el-icon>新增模板
      </el-button>
      <div class="flex gap-2">
        <el-button>
          <el-icon size="16">
            <Download />
          </el-icon>导出
        </el-button>
        <el-button @click="loadTemplates">
          <el-icon size="16">
            <Refresh />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- Template Grid -->
    <div
      v-loading="loading"
      class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-gutter mb-8"
    >
      <div
        v-for="tpl in templateList"
        :key="tpl.id"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden hover:shadow-md transition-all group"
      >
        <div class="aspect-[3/4] bg-surface-container relative flex items-center justify-center overflow-hidden">
          <img
            v-if="tpl.thumbnailUrl"
            :src="tpl.thumbnailUrl"
            class="w-full h-full object-cover"
          >
          <div
            v-else
            class="flex flex-col items-center gap-3 text-outline"
          >
            <el-icon size="48">
              <Document />
            </el-icon>
            <span class="text-label-md">暂无预览</span>
          </div>
          <div class="absolute top-3 right-3 flex flex-col gap-1 items-end">
            <el-tag
              :type="tpl.status === 'active' ? 'success' : 'info'"
              size="small"
            >
              {{ tpl.status === 'active' ? '已上架' : '已下架' }}
            </el-tag>
            <el-tag
              v-if="tpl.isBuiltin"
              type="warning"
              size="small"
            >
              内置
            </el-tag>
          </div>
        </div>
        <div class="p-5">
          <div class="flex justify-between items-start mb-2">
            <div>
              <h3 class="text-title-md font-bold">
                {{ tpl.name }}
              </h3>
              <p class="text-label-md text-outline font-mono mt-1">
                {{ tpl.code }}
              </p>
            </div>
            <el-dropdown>
              <el-icon
                class="text-on-surface-variant hover:text-primary cursor-pointer"
                size="18"
              >
                <More />
              </el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleEdit(tpl)">
                    编辑
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleToggleStatus(tpl)">
                    {{ tpl.status === 'active' ? '下架' : '上架' }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    divided
                    @click="handleDelete(tpl)"
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <p class="text-body-md text-on-surface-variant mb-4 line-clamp-2">
            {{ tpl.description || '暂无描述' }}
          </p>
          <div class="flex items-center justify-between text-label-md text-outline">
            <span>排序: {{ tpl.sortOrder }}</span>
            <span>{{ formatDate(tpl.updatedAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="模板编码"
          prop="code"
        >
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item
          label="模板名称"
          prop="name"
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item
          label="分类"
          prop="category"
        >
          <el-select
            v-model="form.category"
            placeholder="请选择"
          >
            <el-option
              label="经典"
              value="classic"
            />
            <el-option
              label="技术"
              value="tech"
            />
            <el-option
              label="应届生"
              value="fresh"
            />
            <el-option
              label="商务"
              value="business"
            />
            <el-option
              label="学术"
              value="postgraduate"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="缩略图">
          <el-upload
            class="thumbnail-uploader"
            :show-file-list="false"
            :http-request="handleUpload"
            :on-success="handleUploadSuccess"
          >
            <img
              v-if="form.thumbnailUrl"
              :src="form.thumbnailUrl"
              class="w-full h-full object-cover"
            >
            <div
              v-else
              class="flex flex-col items-center justify-center text-outline"
            >
              <el-icon size="28">
                <Plus />
              </el-icon><span class="text-label-md mt-2">上传缩略图</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number
            v-model="form.sortOrder"
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
import { Document, Search, Plus, Download, Refresh, More } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadProps, UploadRequestOptions } from 'element-plus'
import { templateApi, type Template, type TemplateRequest, type TemplateStats } from '@/api/admin/templates'

interface TemplateForm extends TemplateRequest {
  id?: string
}

const loading = ref(false)
const filters = reactive({ keyword: '', category: '', status: '' })
const templateList = ref<Template[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增模板')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<TemplateForm>({
  id: '',
  code: '',
  name: '',
  category: '',
  thumbnailUrl: '',
  description: '',
  htmlTemplate: '',
  renderEngine: 'server',
  config: {},
  sortOrder: 0,
  isRecommended: false
})
const formRules: FormRules = {
  code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const stats = reactive([
  { label: '模板总数', value: '-', icon: Document, iconBg: 'bg-primary/10', iconColor: 'text-primary' },
  { label: '已上架', value: '-', icon: Document, iconBg: 'bg-secondary/10', iconColor: 'text-secondary' },
  { label: '已下架', value: '-', icon: Plus, iconBg: 'bg-tertiary/10', iconColor: 'text-tertiary' },
  { label: '系统内置', value: '-', icon: Document, iconBg: 'bg-on-tertiary-fixed-variant/10', iconColor: 'text-on-tertiary-fixed-variant' }
])

onMounted(() => {
  loadStats()
  loadTemplates()
})

async function loadStats() {
  try {
    const s: TemplateStats = await templateApi.getTemplateStats()
    stats[0].value = String(s.totalTemplates)
    stats[1].value = String(s.activeTemplates)
    stats[2].value = String(s.inactiveTemplates)
    stats[3].value = String(s.builtinTemplates)
  } catch (e: any) {
    ElMessage.error(e.message || '加载统计数据失败')
  }
}

async function loadTemplates() {
  loading.value = true
  try {
    const res = await templateApi.getTemplates({
      page: 1,
      size: 100,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      category: filters.category || undefined
    })
    templateList.value = res.records
  } catch (e: any) {
    ElMessage.error(e.message || '加载模板列表失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.category = ''
  filters.status = ''
  loadTemplates()
}

function handleAdd() {
  dialogTitle.value = '新增模板'
  resetForm()
  dialogVisible.value = true
}

async function handleEdit(row: Template) {
  dialogTitle.value = '编辑模板'
  resetForm()
  try {
    const detail = await templateApi.getTemplate(row.id)
    Object.assign(form, detail, {
      config: detail.config || {},
      htmlTemplate: detail.thumbnailUrl || row.code,
      renderEngine: 'server',
      isRecommended: detail.isRecommended
    })
  } catch {
    Object.assign(form, row, {
      config: {},
      htmlTemplate: row.code,
      renderEngine: 'server',
      isRecommended: row.isRecommended
    })
  }
  dialogVisible.value = true
}

async function handleToggleStatus(row: Template) {
  const nextStatus = row.status === 'active' ? 'inactive' : 'active'
  const action = nextStatus === 'active' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${action}该模板吗？`, '提示', { type: 'warning' })
    await templateApi.updateTemplateStatus(row.id, nextStatus)
    ElMessage.success(`${action}成功`)
    loadTemplates()
    loadStats()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || `${action}失败`)
    }
  }
}

async function handleDelete(row: Template) {
  if (row.isBuiltin) {
    ElMessage.warning('系统内置模板不可删除')
    return
  }
  try {
    await ElMessageBox.confirm('确定要删除该模板吗？', '提示', { type: 'warning' })
    await templateApi.deleteTemplate(row.id)
    ElMessage.success('删除成功')
    loadTemplates()
    loadStats()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    id: '',
    code: '',
    name: '',
    category: '',
    thumbnailUrl: '',
    description: '',
    htmlTemplate: '',
    renderEngine: 'server',
    config: {},
    sortOrder: 0,
    isRecommended: false
  })
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload: TemplateRequest = {
        code: form.code,
        name: form.name,
        category: form.category,
        thumbnailUrl: form.thumbnailUrl,
        description: form.description,
        htmlTemplate: form.htmlTemplate || form.code,
        renderEngine: form.renderEngine || 'server',
        config: form.config || {},
        sortOrder: form.sortOrder ?? 0,
        isRecommended: form.isRecommended ?? false
      }
      if (form.id) {
        await templateApi.updateTemplate(form.id, payload)
      } else {
        await templateApi.createTemplate(payload)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      resetForm()
      loadTemplates()
      loadStats()
    } catch (e: any) {
      ElMessage.error(e.message || '保存失败')
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleUpload(options: UploadRequestOptions) {
  const res = await templateApi.uploadThumbnail(options.file)
  return res
}

const handleUploadSuccess: UploadProps['onSuccess'] = (response: any) => {
  form.thumbnailUrl = response.url
}

function formatDate(date: string | null) {
  return date ? date.replace('T', ' ').substring(0, 19) : '-'
}
</script>

<style scoped lang="scss">
.admin-template-management {
  padding-bottom: var(--st-margin-page);
}
.thumbnail-uploader {
  width: 120px;
  height: 160px;
  border: 1px dashed var(--st-outline-variant);
  border-radius: var(--st-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  &:hover { border-color: var(--st-primary); }
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
