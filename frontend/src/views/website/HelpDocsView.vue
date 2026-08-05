<template>
  <div class="py-24">
    <div class="max-w-[1440px] mx-auto px-margin-page">
      <div class="text-center mb-16">
        <h1 class="text-headline-md font-headline-md text-on-surface mb-4">
          帮助文档
        </h1>
        <p class="text-body-md font-body-md text-on-surface-variant max-w-2xl mx-auto">
          快速上手智能简历，了解常见问题与使用技巧。
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-gutter max-w-4xl mx-auto">
        <div
          v-for="doc in docs"
          :key="doc.title"
          class="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant"
        >
          <h3 class="text-title-md font-title-md text-on-surface mb-2">
            {{ doc.title }}
          </h3>
          <p class="text-body-md text-on-surface-variant">
            {{ doc.summary }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const docs = [
  { title: '如何创建第一份简历？', summary: '登录后进入工作台，选择模板并填写个人信息，即可生成简历。' },
  { title: '如何导出 PDF？', summary: '在编辑器页面点击导出按钮，选择 PDF 格式并等待生成完成。' },
  { title: 'AI 点评如何使用？', summary: '在简历详情页点击 AI 点评，系统会自动分析并给出优化建议。' },
  { title: '忘记密码怎么办？', summary: '目前请联系客服邮箱重置密码，后续将上线自助找回功能。' }
]
</script>
