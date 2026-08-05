<template>
  <div>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-fixed/50 to-surface-container-lowest pt-16 pb-24 overflow-hidden">
      <div class="max-w-[1440px] mx-auto px-margin-page grid grid-cols-1 lg:grid-cols-2 gap-stack-lg items-center">
        <div class="z-10">
          <div class="inline-flex items-center gap-2 px-3 py-1 bg-primary-fixed text-on-surface rounded-full text-label-md font-label-md mb-6">
            <el-icon size="14">
              <MagicStick />
            </el-icon>
            <span>AI 智能面试官已上线</span>
          </div>
          <h1 class="text-[56px] leading-tight font-bold text-on-surface mb-6">
            把经历，<br><span class="text-primary">整理成机会。</span>
          </h1>
          <p class="text-body-lg font-body-lg text-on-surface-variant mb-10 max-w-lg">
            智能简历助手帮助企业与职场人高效打造专业简历，用数据与 AI 让每一次投递更有价值。突破传统排版困扰，实时点评优化，助力你的职场起航。
          </p>
          <div class="flex flex-wrap gap-4 mb-10">
            <button
              class="bg-primary text-on-primary px-8 py-4 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              @click="goTo('/login')"
            >
              免费生成简历
              <el-icon size="20">
                <ArrowRight />
              </el-icon>
            </button>
            <button
              class="border border-outline-variant text-on-surface-variant hover:bg-surface-container-low px-8 py-4 rounded-lg font-label-md flex items-center gap-2 transition-colors"
              @click="goTo('/login')"
            >
              <el-icon size="20">
                <VideoPlay />
              </el-icon>
              查看产品演示
            </button>
          </div>
          <div class="flex items-center gap-8 text-on-surface-variant text-label-md font-label-md">
            <div class="flex items-center gap-2">
              <el-icon
                class="text-secondary"
                size="18"
              >
                <CircleCheckFilled />
              </el-icon>
              数据安全保障
            </div>
            <div class="flex items-center gap-2">
              <el-icon
                class="text-secondary"
                size="18"
              >
                <CircleCheckFilled />
              </el-icon>
              隐私严格保护
            </div>
            <div class="flex items-center gap-2">
              <el-icon
                class="text-secondary"
                size="18"
              >
                <CircleCheckFilled />
              </el-icon>
              专业简历服务
            </div>
          </div>
        </div>
        <div class="relative lg:h-[520px] flex items-center justify-center">
          <div class="relative w-full max-w-md bg-surface-container-lowest rounded-2xl overflow-hidden border border-outline-variant shadow-xl animate-float">
            <div class="aspect-[3/4] bg-surface-container flex items-center justify-center">
              <div class="p-8 bg-white rounded-xl shadow-lg w-3/4 h-3/4 flex flex-col gap-4">
                <div class="flex items-center gap-3">
                  <div class="w-12 h-12 rounded-lg bg-primary text-on-primary flex items-center justify-center text-xl font-bold">
                    Z
                  </div>
                  <div>
                    <div class="font-bold text-on-surface">
                      张一航
                    </div>
                    <div class="text-[12px] text-on-surface-variant">
                      产品经理
                    </div>
                  </div>
                </div>
                <div class="space-y-2">
                  <div class="h-2 bg-surface-container rounded w-full" />
                  <div class="h-2 bg-surface-container rounded w-5/6" />
                  <div class="h-2 bg-surface-container rounded w-4/6" />
                </div>
                <div class="space-y-2">
                  <div class="h-2 bg-surface-container rounded w-full" />
                  <div class="h-2 bg-surface-container rounded w-3/4" />
                </div>
                <div class="mt-auto flex gap-2">
                  <span class="px-2 py-1 bg-primary/10 text-primary rounded text-[10px] font-bold">需求分析</span>
                  <span class="px-2 py-1 bg-secondary/10 text-secondary rounded text-[10px] font-bold">数据分析</span>
                </div>
              </div>
            </div>
          </div>
          <div class="absolute top-10 -right-4 bg-white/70 backdrop-blur-md border border-white/50 p-4 rounded-xl shadow-lg animate-bounce">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center text-on-secondary-container">
                <el-icon size="18">
                  <CircleCheckFilled />
                </el-icon>
              </div>
              <div>
                <p class="text-label-md font-label-md text-on-surface">
                  AI 优化建议
                </p>
                <p class="text-[10px] text-on-surface-variant">
                  简历竞争力提升 35%
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Partners -->
    <section class="bg-surface py-12 border-y border-outline-variant">
      <div class="max-w-[1440px] mx-auto px-margin-page text-center">
        <p class="text-label-md font-label-md text-on-surface-variant mb-8 uppercase tracking-widest">
          众多知名企业与机构的信赖之选
        </p>
        <div class="flex flex-wrap justify-center items-center gap-12 opacity-50 grayscale hover:grayscale-0 transition-all duration-500 text-title-lg font-title-lg">
          <span>Tencent 腾讯</span>
          <span>Alibaba 阿里巴巴</span>
          <span>ByteDance 字节跳动</span>
          <span>HUAWEI 华为</span>
          <span>MEITUAN 美团</span>
          <span>XIAOMI 小米</span>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="py-24 bg-surface-container-low">
      <div class="max-w-[1440px] mx-auto px-margin-page">
        <div class="text-center mb-16">
          <h2 class="text-headline-md font-headline-md text-on-surface mb-4">
            核心功能，助你快人一步
          </h2>
          <p class="text-body-md font-body-md text-on-surface-variant max-w-2xl mx-auto">
            我们集成了最先进的 AI 技术，为你提供从撰写、排版到优化的一站式解决方案，让简历脱颖而出。
          </p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-gutter">
          <div
            v-for="f in features"
            :key="f.title"
            class="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant hover:border-primary transition-all group"
          >
            <div
              class="w-12 h-12 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform"
              :class="f.iconBg"
            >
              <el-icon
                :class="f.iconColor"
                size="28"
              >
                <component :is="f.icon" />
              </el-icon>
            </div>
            <h3 class="text-title-md font-title-md text-on-surface mb-3">
              {{ f.title }}
            </h3>
            <p class="text-body-md font-body-md text-on-surface-variant">
              {{ f.desc }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Demo -->
    <section class="py-24 bg-surface-container-lowest">
      <div class="max-w-[1440px] mx-auto px-margin-page">
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          <div class="lg:col-span-4">
            <p class="text-primary font-label-md text-label-md mb-2 uppercase tracking-wide">
              产品演示
            </p>
            <h2 class="text-headline-md font-headline-md text-on-surface mb-6">
              高效编辑，<br>专业呈现
            </h2>
            <p class="text-body-lg font-body-lg text-on-surface-variant mb-8">
              所见即所得的编辑体验，结构化填写，实时预览，AI 实时辅助优化。告别 Word 排版噩梦，把时间花在核心经历的打磨上。
            </p>
            <ul class="space-y-4 mb-10">
              <li class="flex items-center gap-3 text-body-md font-body-md text-on-surface">
                <el-icon
                  class="text-primary"
                  size="20"
                >
                  <CircleCheckFilled />
                </el-icon>
                模块化自由拖拽
              </li>
              <li class="flex items-center gap-3 text-body-md font-body-md text-on-surface">
                <el-icon
                  class="text-primary"
                  size="20"
                >
                  <CircleCheckFilled />
                </el-icon>
                AI 自动纠错与润色
              </li>
              <li class="flex items-center gap-3 text-body-md font-body-md text-on-surface">
                <el-icon
                  class="text-primary"
                  size="20"
                >
                  <CircleCheckFilled />
                </el-icon>
                多版本投递管理
              </li>
            </ul>
            <button
              class="bg-primary text-on-primary px-8 py-4 rounded-lg font-label-md flex items-center gap-2 shadow-sm hover:scale-[0.98] transition-transform"
              @click="goTo('/login')"
            >
              立即体验编辑器
              <el-icon size="20">
                <TopRight />
              </el-icon>
            </button>
          </div>
          <div class="lg:col-span-8">
            <div class="bg-surface-container-lowest rounded-2xl border border-outline-variant shadow-xl overflow-hidden">
              <div class="bg-surface-container-low p-3 flex items-center gap-2 border-b border-outline-variant">
                <span class="w-3 h-3 rounded-full bg-error" />
                <span class="w-3 h-3 rounded-full bg-yellow-500" />
                <span class="w-3 h-3 rounded-full bg-secondary" />
                <span class="ml-4 text-label-md font-label-md text-on-surface-variant">智能简历编辑器</span>
              </div>
              <div class="grid grid-cols-12 h-[400px]">
                <div class="col-span-3 bg-primary p-4 space-y-3">
                  <div
                    v-for="i in 4"
                    :key="i"
                    class="flex items-center gap-3 px-3 py-2 rounded-lg"
                    :class="i === 1 ? 'bg-primary-container text-on-primary' : 'text-on-primary/60'"
                  >
                    <div class="w-4 h-4 rounded bg-white/20" />
                    <span class="text-label-md font-label-md">模块 {{ i }}</span>
                  </div>
                </div>
                <div class="col-span-6 bg-surface-container-low p-8 overflow-hidden">
                  <div class="bg-white rounded-lg shadow-md p-6 h-full space-y-4">
                    <div class="h-4 bg-surface-container rounded w-1/3" />
                    <div class="h-3 bg-surface-container rounded w-full" />
                    <div class="h-3 bg-surface-container rounded w-5/6" />
                    <div class="h-3 bg-surface-container rounded w-4/6" />
                  </div>
                </div>
                <div class="col-span-3 bg-surface-container-lowest p-4 border-l border-outline-variant">
                  <div class="text-title-md font-title-md text-on-surface mb-4">
                    AI 简历评估
                  </div>
                  <div class="text-4xl font-bold text-primary mb-2">
                    82<span class="text-xl text-outline">/100</span>
                  </div>
                  <div class="h-2 bg-surface-container rounded-full mb-4">
                    <div class="h-full bg-primary rounded-full w-[82%]" />
                  </div>
                  <div class="space-y-2">
                    <div class="p-2 bg-error/5 rounded border border-error/20 text-[12px] font-bold text-error">
                      最值得修改 (2)
                    </div>
                    <div class="p-2 bg-secondary-container/10 rounded border border-secondary-container/20 text-[12px] font-bold text-secondary">
                      可增强 (3)
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-24 bg-primary">
      <div class="max-w-[1440px] mx-auto px-margin-page text-center">
        <h2 class="text-headline-md font-headline-md text-on-primary mb-6">
          准备好打造一份专业简历了吗？
        </h2>
        <p class="text-body-lg font-body-lg text-on-primary/80 mb-10 max-w-2xl mx-auto">
          立即注册，免费体验 AI 简历生成、点评与导出功能。
        </p>
        <button
          class="bg-white text-primary px-8 py-4 rounded-lg font-label-md shadow-sm hover:scale-[0.98] transition-transform"
          @click="goTo('/login')"
        >
          免费开始使用
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  MagicStick, ArrowRight, VideoPlay, CircleCheckFilled,
  TopRight, ChatDotRound, Grid, Download
} from '@element-plus/icons-vue'

const router = useRouter()

const features = [
  { title: '智能生成', desc: '基于岗位需求与经历描述，智能生成专业词汇与工作亮点。', icon: MagicStick, iconBg: 'bg-primary-fixed', iconColor: 'text-primary' },
  { title: 'AI 点评优化', desc: '多维度深度解析，提供针对性的修改建议，提升面试邀请率。', icon: ChatDotRound, iconBg: 'bg-tertiary-fixed', iconColor: 'text-tertiary' },
  { title: '模板秒速切换', desc: '海量职场精英模板，内容排版自动适配，风格随心切换。', icon: Grid, iconBg: 'bg-secondary-container', iconColor: 'text-secondary' },
  { title: '一键导出 PDF', desc: '高清无损导出，支持多种主流格式，确保投递排版万无一失。', icon: Download, iconBg: 'bg-primary-fixed', iconColor: 'text-primary' }
]

function goTo(path: string) {
  router.push(path)
}
</script>

<style scoped>
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
.animate-float {
  animation: float 6s ease-in-out infinite;
}
</style>
