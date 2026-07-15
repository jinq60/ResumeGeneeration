<template>
  <div class="system-settings">
    <div class="page-header">
      <h1>系统设置</h1>
    </div>

    <el-tabs
      v-model="activeTab"
      class="settings-tabs"
    >
      <el-tab-pane
        label="基本设置"
        name="basic"
      >
        <div class="settings-content">
          <el-form
            :model="basicSettings"
            label-width="150px"
          >
            <el-form-item label="系统名称">
              <el-input
                v-model="basicSettings.systemName"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item label="系统描述">
              <el-input
                v-model="basicSettings.systemDescription"
                type="textarea"
                :rows="3"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item label="联系邮箱">
              <el-input
                v-model="basicSettings.contactEmail"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item label="客服电话">
              <el-input
                v-model="basicSettings.contactPhone"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item label="网站Logo">
              <el-upload
                class="logo-uploader"
                :show-file-list="false"
                :on-success="handleLogoUploadSuccess"
              >
                <img
                  v-if="basicSettings.logoUrl"
                  :src="basicSettings.logoUrl"
                  class="logo"
                >
                <el-icon
                  v-else
                  class="uploader-icon"
                >
                  <Plus />
                </el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="saveBasicSettings"
              >
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane
        label="安全设置"
        name="security"
      >
        <div class="settings-content">
          <el-form
            :model="securitySettings"
            label-width="150px"
          >
            <el-form-item label="密码最小长度">
              <el-input-number
                v-model="securitySettings.minPasswordLength"
                :min="6"
                :max="20"
              />
            </el-form-item>
            <el-form-item label="密码复杂度">
              <el-checkbox-group v-model="securitySettings.passwordComplexity">
                <el-checkbox
                  label="包含数字"
                  value="number"
                />
                <el-checkbox
                  label="包含大写字母"
                  value="uppercase"
                />
                <el-checkbox
                  label="包含小写字母"
                  value="lowercase"
                />
                <el-checkbox
                  label="包含特殊字符"
                  value="special"
                />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="登录失败锁定">
              <el-switch v-model="securitySettings.loginLockEnabled" />
            </el-form-item>
            <el-form-item label="最大失败次数">
              <el-input-number
                v-model="securitySettings.maxFailedAttempts"
                :min="3"
                :max="10"
                :disabled="!securitySettings.loginLockEnabled"
              />
            </el-form-item>
            <el-form-item label="锁定时间（分钟）">
              <el-input-number
                v-model="securitySettings.lockDuration"
                :min="5"
                :max="60"
                :disabled="!securitySettings.loginLockEnabled"
              />
            </el-form-item>
            <el-form-item label="Token过期时间">
              <el-input-number
                v-model="securitySettings.tokenExpiration"
                :min="30"
                :max="1440"
              />
              <span style="margin-left: 10px">分钟</span>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="saveSecuritySettings"
              >
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane
        label="通知设置"
        name="notification"
      >
        <div class="settings-content">
          <el-form
            :model="notificationSettings"
            label-width="150px"
          >
            <el-form-item label="邮件通知">
              <el-switch v-model="notificationSettings.emailEnabled" />
            </el-form-item>
            <el-form-item label="SMTP服务器">
              <el-input
                v-model="notificationSettings.smtpHost"
                style="width: 400px"
                :disabled="!notificationSettings.emailEnabled"
              />
            </el-form-item>
            <el-form-item label="SMTP端口">
              <el-input-number
                v-model="notificationSettings.smtpPort"
                :min="1"
                :max="65535"
                :disabled="!notificationSettings.emailEnabled"
              />
            </el-form-item>
            <el-form-item label="发件人邮箱">
              <el-input
                v-model="notificationSettings.fromEmail"
                style="width: 400px"
                :disabled="!notificationSettings.emailEnabled"
              />
            </el-form-item>
            <el-form-item label="系统通知">
              <el-switch v-model="notificationSettings.systemEnabled" />
            </el-form-item>
            <el-form-item label="用户注册通知">
              <el-switch v-model="notificationSettings.userRegistrationEnabled" />
            </el-form-item>
            <el-form-item label="简历导出通知">
              <el-switch v-model="notificationSettings.resumeExportEnabled" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="saveNotificationSettings"
              >
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane
        label="存储设置"
        name="storage"
      >
        <div class="settings-content">
          <el-form
            :model="storageSettings"
            label-width="150px"
          >
            <el-form-item label="存储类型">
              <el-radio-group v-model="storageSettings.storageType">
                <el-radio label="local">
                  本地存储
                </el-radio>
                <el-radio label="minio">
                  MinIO
                </el-radio>
                <el-radio label="oss">
                  阿里云OSS
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item
              v-if="storageSettings.storageType === 'minio'"
              label="MinIO地址"
            >
              <el-input
                v-model="storageSettings.minioEndpoint"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item
              v-if="storageSettings.storageType === 'minio'"
              label="Access Key"
            >
              <el-input
                v-model="storageSettings.accessKey"
                style="width: 400px"
                type="password"
              />
            </el-form-item>
            <el-form-item
              v-if="storageSettings.storageType === 'minio'"
              label="Secret Key"
            >
              <el-input
                v-model="storageSettings.secretKey"
                style="width: 400px"
                type="password"
              />
            </el-form-item>
            <el-form-item
              v-if="storageSettings.storageType === 'minio'"
              label="Bucket名称"
            >
              <el-input
                v-model="storageSettings.bucketName"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item label="最大文件大小">
              <el-input-number
                v-model="storageSettings.maxFileSize"
                :min="1"
                :max="100"
              />
              <span style="margin-left: 10px">MB</span>
            </el-form-item>
            <el-form-item label="允许的文件类型">
              <el-input
                v-model="storageSettings.allowedFileTypes"
                type="textarea"
                :rows="3"
                style="width: 400px"
                placeholder="例如：jpg,png,pdf,docx"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="saveStorageSettings"
              >
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('basic')

const basicSettings = reactive({
  systemName: '智能简历生成系统',
  systemDescription: '基于AI的在线简历生成工具',
  contactEmail: 'support@resume.com',
  contactPhone: '400-123-4567',
  logoUrl: ''
})

const securitySettings = reactive({
  minPasswordLength: 6,
  passwordComplexity: ['number', 'lowercase'],
  loginLockEnabled: true,
  maxFailedAttempts: 5,
  lockDuration: 30,
  tokenExpiration: 60
})

const notificationSettings = reactive({
  emailEnabled: false,
  smtpHost: '',
  smtpPort: 587,
  fromEmail: '',
  systemEnabled: true,
  userRegistrationEnabled: true,
  resumeExportEnabled: true
})

const storageSettings = reactive({
  storageType: 'minio',
  minioEndpoint: '',
  accessKey: '',
  secretKey: '',
  bucketName: 'resume-storage',
  maxFileSize: 10,
  allowedFileTypes: 'jpg,jpeg,png,gif,pdf,docx'
})

onMounted(() => {
  loadSettings()
})

const loadSettings = () => {
  // TODO: 调用API加载系统设置
}

const saveBasicSettings = () => {
  // TODO: 调用API保存基本设置
  ElMessage.success('基本设置保存成功')
}

const saveSecuritySettings = () => {
  // TODO: 调用API保存安全设置
  ElMessage.success('安全设置保存成功')
}

const saveNotificationSettings = () => {
  // TODO: 调用API保存通知设置
  ElMessage.success('通知设置保存成功')
}

const saveStorageSettings = () => {
  // TODO: 调用API保存存储设置
  ElMessage.success('存储设置保存成功')
}

const handleLogoUploadSuccess = (response: any) => {
  basicSettings.logoUrl = response.url
  ElMessage.success('Logo上传成功')
}
</script>

<style scoped>
.system-settings {
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

.settings-tabs {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.settings-content {
  max-width: 600px;
  padding: 20px 0;
}

.logo-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 150px;
  height: 80px;
}

.logo-uploader:hover {
  border-color: #409eff;
}

.logo-uploader .logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 150px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
