<template>
  <div class="py-24">
    <div class="max-w-[1440px] mx-auto px-margin-page">
      <div class="text-center mb-16">
        <h1 class="text-headline-md font-headline-md text-on-surface mb-4">
          定价方案
        </h1>
        <p class="text-body-md font-body-md text-on-surface-variant max-w-2xl mx-auto">
          灵活的会员计划，满足不同阶段求职者的需求。
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-gutter max-w-5xl mx-auto">
        <div
          v-for="plan in plans"
          :key="plan.name"
          class="bg-surface-container-lowest p-8 rounded-2xl border border-outline-variant"
          :class="{ 'ring-2 ring-primary': plan.highlight }"
        >
          <h3 class="text-title-lg font-title-lg text-on-surface mb-2">
            {{ plan.name }}
          </h3>
          <p class="text-body-md text-on-surface-variant mb-6">
            {{ plan.desc }}
          </p>
          <div class="mb-6">
            <span class="text-[48px] font-bold text-on-surface">{{ plan.price }}</span>
            <span class="text-body-md text-on-surface-variant">{{ plan.unit }}</span>
          </div>
          <ul class="space-y-3 mb-8">
            <li
              v-for="feature in plan.features"
              :key="feature"
              class="text-body-md text-on-surface-variant"
            >
              {{ feature }}
            </li>
          </ul>
          <button
            class="w-full py-3 rounded-lg font-label-md"
            :class="plan.highlight ? 'bg-primary text-on-primary' : 'border border-outline-variant text-on-surface'"
            @click="goToLogin"
          >
            {{ plan.cta }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const plans = [
  {
    name: '免费版',
    desc: '适合初次体验',
    price: '¥0',
    unit: '/月',
    features: ['创建 3 份简历', '基础模板 5 款', 'PDF 导出'],
    cta: '免费开始',
    highlight: false
  },
  {
    name: '专业版',
    desc: '适合认真求职的你',
    price: '¥29',
    unit: '/月',
    features: ['无限简历', '全部模板', 'AI 点评', '投递管理'],
    cta: '立即订阅',
    highlight: true
  },
  {
    name: '团队版',
    desc: '适合企业与高校',
    price: '¥199',
    unit: '/月',
    features: ['多人协作', '统一管理', '品牌定制', '专属客服'],
    cta: '联系我们',
    highlight: false
  }
]

function goToLogin() {
  router.push('/login')
}
</script>
