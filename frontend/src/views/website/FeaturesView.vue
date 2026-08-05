<template>
  <div class="py-24">
    <div class="max-w-[1440px] mx-auto px-margin-page">
      <div class="text-center mb-16">
        <h1 class="text-headline-md font-headline-md text-on-surface mb-4">
          功能特性
        </h1>
        <p class="text-body-md font-body-md text-on-surface-variant max-w-2xl mx-auto">
          从简历创建到投递管理，全流程 AI 赋能，让求职更高效。
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-gutter">
        <div
          v-for="item in features"
          :key="item.title"
          class="bg-surface-container-lowest p-8 rounded-2xl border border-outline-variant"
        >
          <div class="w-12 h-12 rounded-xl bg-primary-fixed flex items-center justify-center mb-6">
            <el-icon
              size="28"
              class="text-primary"
            >
              <component :is="item.icon" />
            </el-icon>
          </div>
          <h3 class="text-title-md font-title-md text-on-surface mb-3">
            {{ item.title }}
          </h3>
          <p class="text-body-md font-body-md text-on-surface-variant">
            {{ item.desc }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MagicStick, Document, ChatDotRound, Download, Avatar, DataAnalysis } from '@element-plus/icons-vue'

const features = [
  { title: '智能简历生成', desc: '基于岗位描述与个人信息，一键生成专业简历内容。', icon: MagicStick },
  { title: '海量精品模板', desc: '覆盖互联网、金融、制造等多行业模板，实时切换。', icon: Document },
  { title: 'AI 简历点评', desc: '多维度评估简历竞争力，给出可执行的优化建议。', icon: ChatDotRound },
  { title: '一键导出 PDF', desc: '高清 PDF 导出，完美适配招聘系统与邮件投递。', icon: Download },
  { title: '一寸照优化', desc: '上传头像自动裁剪优化，生成标准一寸照。', icon: Avatar },
  { title: '投递管理', desc: '记录投递公司与岗位，追踪面试进度与结果。', icon: DataAnalysis }
]
</script>
