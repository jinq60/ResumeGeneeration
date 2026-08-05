<template>
  <div class="py-24">
    <div class="max-w-[1440px] mx-auto px-margin-page">
      <div class="text-center mb-16">
        <h1 class="text-headline-md font-headline-md text-on-surface mb-4">
          模板中心
        </h1>
        <p class="text-body-md font-body-md text-on-surface-variant max-w-2xl mx-auto">
          精选多种行业风格的简历模板，适用于不同岗位与求职场景。
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-gutter">
        <div
          v-for="i in 8"
          :key="i"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant overflow-hidden hover:shadow-md transition-shadow"
        >
          <div class="aspect-[3/4] bg-surface-container flex items-center justify-center">
            <span class="text-title-md text-on-surface-variant">模板 {{ i }}</span>
          </div>
          <div class="p-4">
            <h3 class="text-title-md font-title-md text-on-surface mb-1">
              商务风格 {{ i }}
            </h3>
            <p class="text-body-md text-on-surface-variant mb-4">
              简洁专业，适合大多数岗位
            </p>
            <button
              class="w-full py-2 rounded-lg bg-primary text-on-primary font-label-md"
              @click="goToLogin"
            >
              立即使用
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goToLogin() {
  router.push('/login')
}
</script>
