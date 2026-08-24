<template>
  <div>
    <!-- Hero Section -->
    <section class="relative overflow-hidden pt-24 pb-32 lg:pt-32 lg:pb-40">
      <!-- Background blobs -->
      <div class="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
        <div class="absolute top-0 left-1/4 w-[500px] h-[500px] bg-secondary/60 rounded-full mix-blend-multiply filter blur-3xl opacity-70 animate-blob" />
        <div class="absolute top-0 right-1/4 w-[500px] h-[500px] bg-accent/80 rounded-full mix-blend-multiply filter blur-3xl opacity-70 animate-blob animation-delay-2000" />
        <div class="absolute -bottom-32 left-1/3 w-[500px] h-[500px] bg-secondary/50 rounded-full mix-blend-multiply filter blur-3xl opacity-70 animate-blob animation-delay-4000" />
      </div>

      <div class="max-w-[1200px] mx-auto px-margin-page text-center">
        <!-- Badge -->
        <div class="inline-flex items-center gap-2 px-4 py-2 bg-card border border-border rounded-full text-label-md font-medium text-muted-foreground mb-8 animate-fade-in-up">
          <span class="w-2 h-2 rounded-full bg-foreground animate-pulse" />
          AI 智能简历已上线
        </div>

        <!-- Headline -->
        <h1 class="text-4xl sm:text-5xl lg:text-7xl font-serif font-medium text-foreground leading-[1.1] tracking-tight mb-8 animate-fade-in-up animation-delay-200">
          把经历，<br class="sm:hidden">
          <span class="text-muted">整理成机会。</span>
        </h1>

        <!-- Subtitle -->
        <p class="text-lg sm:text-xl text-muted-foreground max-w-2xl mx-auto mb-10 leading-relaxed animate-fade-in-up animation-delay-400">
          智能简历助手帮助职场人高效打造专业简历。AI 实时点评、一键优化、多模板切换，让每一次投递都更有价值。
        </p>

        <!-- CTAs -->
        <div class="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16 animate-fade-in-up animation-delay-600">
          <button
            class="w-full sm:w-auto px-8 py-4 bg-foreground text-primary-foreground rounded-xl font-medium text-base hover:opacity-90 active:scale-[0.98] transition-all duration-200 flex items-center justify-center gap-2 shadow-lg"
            @click="goTo('/login')"
          >
            免费生成简历
            <el-icon size="18"><ArrowRight /></el-icon>
          </button>
          <button
            class="w-full sm:w-auto px-8 py-4 bg-card border border-border text-foreground rounded-xl font-medium text-base hover:bg-secondary/80 active:scale-[0.98] transition-all duration-200 flex items-center justify-center gap-2"
            @click="goTo('/templates')"
          >
            浏览模板
            <el-icon size="18"><Grid /></el-icon>
          </button>
        </div>

        <!-- Trust badges -->
        <div class="flex flex-wrap items-center justify-center gap-6 text-sm text-muted-foreground animate-fade-in">
          <div class="flex items-center gap-2">
            <el-icon class="text-foreground" size="18"><CircleCheckFilled /></el-icon>
            <span>免费使用</span>
          </div>
          <div class="flex items-center gap-2">
            <el-icon class="text-foreground" size="18"><CircleCheckFilled /></el-icon>
            <span>数据安全</span>
          </div>
          <div class="flex items-center gap-2">
            <el-icon class="text-foreground" size="18"><CircleCheckFilled /></el-icon>
            <span>AI 驱动</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Editor Preview Section -->
    <section class="py-24 bg-secondary/30">
      <div class="max-w-[1200px] mx-auto px-margin-page">
        <div class="text-center mb-16">
          <p class="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">产品演示</p>
          <h2 class="text-3xl sm:text-4xl lg:text-5xl font-serif font-medium text-foreground mb-4">
            高效编辑，专业呈现
          </h2>
          <p class="text-lg text-muted-foreground max-w-2xl mx-auto">
            所见即所得的编辑体验，结构化填写，实时预览，AI 实时辅助优化。
          </p>
        </div>

        <div class="relative mx-auto max-w-5xl">
          <!-- Browser mockup -->
          <div class="bg-card rounded-2xl border border-border shadow-xl overflow-hidden animate-fade-in-up">
            <div class="bg-secondary/70 px-4 py-3 flex items-center gap-2 border-b border-border">
              <span class="w-3 h-3 rounded-full bg-muted/40" />
              <span class="w-3 h-3 rounded-full bg-muted/40" />
              <span class="w-3 h-3 rounded-full bg-muted/40" />
              <span class="ml-4 text-sm text-muted-foreground">智能简历编辑器</span>
            </div>
            <div class="grid grid-cols-1 lg:grid-cols-12 h-[400px] lg:h-[500px]">
              <!-- Sidebar -->
              <div class="hidden lg:block lg:col-span-3 bg-card border-r border-border p-4">
                <div class="space-y-2">
                  <div
                    v-for="(item, idx) in sidebarItems"
                    :key="item"
                    class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm"
                    :class="idx === 0 ? 'bg-secondary text-foreground font-medium' : 'text-muted-foreground hover:bg-secondary/50'"
                  >
                    <div class="w-4 h-4 rounded bg-muted/20" />
                    {{ item }}
                  </div>
                </div>
              </div>
              <!-- Editor -->
              <div class="col-span-1 lg:col-span-5 bg-secondary/20 p-6 lg:p-8 overflow-hidden">
                <div class="bg-card rounded-xl shadow-sm p-5 lg:p-6 h-full space-y-4 border border-border">
                  <div class="flex items-center gap-3 pb-4 border-b border-border">
                    <div class="w-10 h-10 rounded-lg bg-foreground text-primary-foreground flex items-center justify-center font-serif text-lg font-medium">
                      Z
                    </div>
                    <div>
                      <div class="font-medium text-foreground">张一航</div>
                      <div class="text-xs text-muted-foreground">产品经理</div>
                    </div>
                  </div>
                  <div class="space-y-3">
                    <div class="h-3 bg-secondary rounded-full w-full" />
                    <div class="h-3 bg-secondary rounded-full w-5/6" />
                    <div class="h-3 bg-secondary rounded-full w-4/6" />
                  </div>
                  <div class="space-y-3 pt-2">
                    <div class="h-3 bg-secondary rounded-full w-full" />
                    <div class="h-3 bg-secondary rounded-full w-3/4" />
                  </div>
                </div>
              </div>
              <!-- Preview -->
              <div class="hidden lg:block lg:col-span-4 bg-card p-6 border-l border-border">
                <div class="text-title-md font-medium text-foreground mb-6">AI 简历评估</div>
                <div class="flex items-baseline gap-1 mb-2">
                  <span class="text-5xl font-serif font-medium text-foreground">82</span>
                  <span class="text-xl text-muted">/100</span>
                </div>
                <div class="h-2 bg-secondary rounded-full mb-6">
                  <div class="h-full bg-foreground rounded-full w-[82%]" />
                </div>
                <div class="space-y-3">
                  <div class="p-3 bg-destructive/5 rounded-lg border border-destructive/10 text-sm text-destructive font-medium">
                    最值得修改 (2)
                  </div>
                  <div class="p-3 bg-secondary rounded-lg text-sm text-foreground font-medium">
                    可增强 (3)
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="py-24 lg:py-32">
      <div class="max-w-[1200px] mx-auto px-margin-page">
        <div class="text-center mb-16">
          <p class="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">核心功能</p>
          <h2 class="text-3xl sm:text-4xl lg:text-5xl font-serif font-medium text-foreground mb-4">
            一站式简历解决方案
          </h2>
          <p class="text-lg text-muted-foreground max-w-2xl mx-auto">
            从撰写、排版到优化，AI 全程辅助，让简历脱颖而出。
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div
            v-for="(f, idx) in features"
            :key="f.title"
            class="group bg-card p-6 lg:p-8 rounded-2xl border border-border hover:shadow-lg hover:-translate-y-1 transition-all duration-300 animate-fade-in-up"
            :class="`animation-delay-${(idx + 1) * 100}`"
          >
            <div
              class="w-12 h-12 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300"
              :class="f.iconBg"
            >
              <el-icon
                :class="f.iconColor"
                size="24"
              >
                <component :is="f.icon" />
              </el-icon>
            </div>
            <h3 class="text-lg font-medium text-foreground mb-2">{{ f.title }}</h3>
            <p class="text-sm text-muted-foreground leading-relaxed">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- How it works -->
    <section class="py-24 bg-secondary/30">
      <div class="max-w-[1200px] mx-auto px-margin-page">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <div>
            <p class="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">使用流程</p>
            <h2 class="text-3xl sm:text-4xl font-serif font-medium text-foreground mb-6">
              三步生成专业简历
            </h2>
            <p class="text-lg text-muted-foreground mb-10">
              无需设计基础，跟随引导填写信息，AI 会自动帮你优化表达与排版。
            </p>

            <div class="space-y-8">
              <div
                v-for="(step, idx) in steps"
                :key="step.title"
                class="flex gap-4"
              >
                <div class="flex-shrink-0 w-10 h-10 rounded-full bg-foreground text-primary-foreground flex items-center justify-center font-medium">
                  {{ idx + 1 }}
                </div>
                <div>
                  <h3 class="text-lg font-medium text-foreground mb-1">{{ step.title }}</h3>
                  <p class="text-sm text-muted-foreground leading-relaxed">{{ step.desc }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="relative">
            <div class="absolute inset-0 bg-gradient-to-tr from-secondary via-accent to-secondary opacity-50 rounded-3xl blur-2xl" />
            <div class="relative bg-card rounded-2xl border border-border shadow-xl p-8 animate-float">
              <div class="space-y-6">
                <div class="flex items-center gap-4">
                  <div class="w-16 h-16 rounded-2xl bg-foreground text-primary-foreground flex items-center justify-center">
                    <el-icon size="32"><Document /></el-icon>
                  </div>
                  <div>
                    <div class="text-lg font-medium text-foreground">选择模板</div>
                    <div class="text-sm text-muted-foreground">20+ 专业模板随心切换</div>
                  </div>
                </div>
                <div class="h-px bg-border" />
                <div class="flex items-center gap-4">
                  <div class="w-16 h-16 rounded-2xl bg-secondary text-foreground flex items-center justify-center">
                    <el-icon size="32"><EditPen /></el-icon>
                  </div>
                  <div>
                    <div class="text-lg font-medium text-foreground">填写经历</div>
                    <div class="text-sm text-muted-foreground">模块化编辑，实时预览</div>
                  </div>
                </div>
                <div class="h-px bg-border" />
                <div class="flex items-center gap-4">
                  <div class="w-16 h-16 rounded-2xl bg-secondary text-foreground flex items-center justify-center">
                    <el-icon size="32"><Download /></el-icon>
                  </div>
                  <div>
                    <div class="text-lg font-medium text-foreground">导出投递</div>
                    <div class="text-sm text-muted-foreground">PDF / Word / Markdown 一键导出</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ Section -->
    <section class="py-24 lg:py-32">
      <div class="max-w-[800px] mx-auto px-margin-page">
        <div class="text-center mb-16">
          <p class="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">常见问题</p>
          <h2 class="text-3xl sm:text-4xl font-serif font-medium text-foreground">
            你可能想知道的
          </h2>
        </div>

        <div class="space-y-4">
          <div
            v-for="(faq, idx) in faqs"
            :key="idx"
            class="bg-card rounded-xl border border-border overflow-hidden"
          >
            <button
              class="w-full flex items-center justify-between p-6 text-left hover:bg-secondary/30 transition-colors"
              @click="toggleFaq(idx)"
            >
              <span class="font-medium text-foreground">{{ faq.q }}</span>
              <el-icon
                class="text-muted-foreground transition-transform duration-200"
                :class="{ 'rotate-180': openFaq === idx }"
              >
                <ArrowDown />
              </el-icon>
            </button>
            <div
              v-show="openFaq === idx"
              class="px-6 pb-6 text-sm text-muted-foreground leading-relaxed"
            >
              {{ faq.a }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="py-24 lg:py-32">
      <div class="max-w-[1200px] mx-auto px-margin-page">
        <div class="relative bg-foreground rounded-3xl px-8 py-16 lg:py-24 text-center overflow-hidden">
          <!-- Decorative circles -->
          <div class="absolute top-0 right-0 w-64 h-64 bg-primary-foreground/5 rounded-full -translate-y-1/2 translate-x-1/2" />
          <div class="absolute bottom-0 left-0 w-48 h-48 bg-primary-foreground/5 rounded-full translate-y-1/2 -translate-x-1/2" />

          <div class="relative z-10">
            <h2 class="text-3xl sm:text-4xl lg:text-5xl font-serif font-medium text-primary-foreground mb-4">
              开启你的新职业篇章
            </h2>
            <p class="text-lg text-primary-foreground/70 max-w-xl mx-auto mb-10">
              立即使用智能简历，创建一份令人印象深刻的简历
            </p>
            <button
              class="px-8 py-4 bg-primary-foreground text-foreground rounded-xl font-medium text-base hover:opacity-90 active:scale-[0.98] transition-all duration-200 inline-flex items-center gap-2"
              @click="goTo('/login')"
            >
              免费开始使用
              <el-icon size="18"><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthModalStore } from '@/stores/authModal'
import {
  ArrowRight,
  Grid,
  CircleCheckFilled,
  MagicStick,
  ChatDotRound,
  Download,
  Document,
  EditPen,
  ArrowDown
} from '@element-plus/icons-vue'

const router = useRouter()
const authModalStore = useAuthModalStore()
const openFaq = ref<number | null>(0)

const sidebarItems = ['基本信息', '教育背景', '项目经历', '工作经历', '技能清单']

const features = [
  {
    title: '智能生成',
    desc: '基于岗位需求与经历描述，智能生成专业词汇与工作亮点。',
    icon: MagicStick,
    iconBg: 'bg-secondary',
    iconColor: 'text-foreground'
  },
  {
    title: 'AI 点评优化',
    desc: '多维度深度解析，提供针对性的修改建议，提升面试邀请率。',
    icon: ChatDotRound,
    iconBg: 'bg-secondary',
    iconColor: 'text-foreground'
  },
  {
    title: '模板秒速切换',
    desc: '海量职场精英模板，内容排版自动适配，风格随心切换。',
    icon: Grid,
    iconBg: 'bg-secondary',
    iconColor: 'text-foreground'
  },
  {
    title: '一键导出 PDF',
    desc: '高清无损导出，支持多种主流格式，确保投递排版万无一失。',
    icon: Download,
    iconBg: 'bg-secondary',
    iconColor: 'text-foreground'
  }
]

const steps = [
  { title: '选择模板', desc: '从 20+ 专业模板中挑选最适合你行业与岗位的风格。' },
  { title: '填写经历', desc: '按模块填写教育、工作、项目经历，AI 实时给出优化建议。' },
  { title: '导出投递', desc: '一键导出 PDF / Word / Markdown，直接用于求职投递。' }
]

const faqs = [
  { q: '使用智能简历需要付费吗？', a: '基础功能完全免费，包括简历编辑、预览、PDF 导出和 AI 点评。部分高级模板和 AI 写作次数可能对登录用户开放更多额度。' },
  { q: '我的简历数据安全吗？', a: '我们采用加密传输和存储，你也可以随时导出或删除自己的简历数据。' },
  { q: '支持哪些导出格式？', a: '目前支持 PDF、Word 和 Markdown 三种格式导出，后续会增加更多格式。' },
  { q: 'AI 写作功能如何使用？', a: '在编辑器中选中任意文本字段，点击 AI 写作按钮，选择想要的场景（润色、扩写、翻译等），即可生成内容。' }
]

function goTo(path: string) {
  if (path === '/login') {
    authModalStore.open()
    return
  }
  router.push(path)
}

function toggleFaq(idx: number) {
  openFaq.value = openFaq.value === idx ? null : idx
}
</script>

<style scoped>
.font-serif {
  font-family: var(--font-serif);
}
</style>