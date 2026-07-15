<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>仪表板</h1>
      <p>系统概览与统计数据</p>
    </div>

    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon resume-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalResumes }}</div>
              <div class="stat-label">简历总数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon template-icon">
              <el-icon><Grid /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalTemplates }}</div>
              <div class="stat-label">模板数量</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-icon activity-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.todayActive }}</div>
              <div class="stat-label">今日活跃</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="dashboard-content">
      <el-row :gutter="20">
        <el-col :span="16">
          <div class="content-card">
            <div class="card-header">
              <h3>用户增长趋势</h3>
            </div>
            <div class="chart-container">
              <!-- TODO: 集成图表组件 -->
              <div class="chart-placeholder">图表区域</div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="content-card">
            <div class="card-header">
              <h3>最近活动</h3>
            </div>
            <div class="activity-list">
              <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
                <div class="activity-icon">
                  <el-icon><Clock /></el-icon>
                </div>
                <div class="activity-content">
                  <div class="activity-title">{{ activity.title }}</div>
                  <div class="activity-time">{{ activity.time }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Document, Grid, TrendCharts, Clock } from '@element-plus/icons-vue'

const stats = ref({
  totalUsers: 0,
  totalResumes: 0,
  totalTemplates: 0,
  todayActive: 0
})

const recentActivities = ref([
  { id: 1, title: '用户 张三 创建了新简历', time: '5分钟前' },
  { id: 2, title: '管理员 上架了新模板', time: '15分钟前' },
  { id: 3, title: '用户 李四 导出了PDF', time: '30分钟前' },
  { id: 4, title: '系统 完成了数据备份', time: '1小时前' },
  { id: 5, title: '用户 王五 更新了个人资料', time: '2小时前' }
])

onMounted(() => {
  // TODO: 获取统计数据
  loadStats()
})

const loadStats = () => {
  // 模拟数据
  stats.value = {
    totalUsers: 1234,
    totalResumes: 5678,
    totalTemplates: 12,
    todayActive: 89
  }
}
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.dashboard-header {
  margin-bottom: 20px;
}

.dashboard-header h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 5px;
}

.dashboard-header p {
  font-size: 14px;
  color: #666;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
}

.user-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.resume-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.template-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.activity-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.dashboard-content {
  margin-top: 20px;
}

.content-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.card-header h3 {
  font-size: 16px;
  color: #333;
  margin: 0;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-placeholder {
  color: #999;
  font-size: 14px;
}

.activity-list {
  max-height: 300px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: #666;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: #999;
}
</style>
