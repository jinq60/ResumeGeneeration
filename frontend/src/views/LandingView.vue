<script setup lang="ts">
  import LangSwitch from '@/components/atelier/LangSwitch.vue'
  import { useI18n } from 'vue-i18n'
  const { t } = useI18n()
</script>

<template>
  <div
    class="min-h-screen bg-paper text-ink flex flex-col antialiased selection:bg-surface-high selection:text-ink"
    style="background-color: #fbf9f5"
  >
    <!-- TopAppBar — Stitch 1:1: hairline 1pt, paper/80 blur, sticky -->
    <header
      class="bg-paper/80 backdrop-blur-sm flex justify-between items-center px-6 h-14 w-full sticky top-0 z-40 border-b"
      style="border-color: #eae8e3; background-color: rgba(251, 249, 245, 0.8)"
    >
      <div class="flex items-center gap-8">
        <span
          class="font-newsreader text-[20px] leading-none tracking-[-0.01em] font-semibold"
          style="font-family: 'Newsreader', serif; color: #0f0f0e"
          >Resume Atelier</span
        >
        <nav class="hidden md:flex gap-6 h-14 items-center">
          <router-link
            to="/templates"
            class="font-inter text-[14px] leading-[22px] text-[#636563] hover:text-ink transition-colors h-14 flex items-center border-b-2 border-transparent hover:border-ink/20"
            >Templates</router-link
          >
          <router-link
            to="/resumes"
            class="font-inter text-[14px] leading-[22px] text-ink font-semibold border-b-2 h-14 flex items-center"
            style="border-color: #0f0f0e"
            >Resumes</router-link
          >
        </nav>
      </div>
      <div class="flex items-center gap-3">
        <LangSwitch />
        <router-link
          to="/login"
          class="hidden sm:inline-flex font-inter text-[13px] leading-[20px] text-[#464742] hover:text-ink transition-colors px-3 py-1.5"
          >{{ t('login.login') }}</router-link
        >
        <router-link
          to="/workbench"
          class="ink-pill font-inter text-[13px] leading-[20px] font-medium"
          style="font-family: 'Inter', sans-serif"
        >
          <span class="signal-dot"></span> {{ t('common.export') }}
        </router-link>
        <router-link
          to="/workbench"
          class="w-9 h-9 rounded-full flex items-center justify-center text-ink hover:bg-stone transition-colors"
          aria-label="AI"
        >
          <span
            class="material-symbols-outlined text-[20px]"
            style="font-variation-settings: 'FILL' 0"
            >auto_awesome</span
          >
        </router-link>
      </div>
    </header>

    <main class="flex-grow flex flex-col items-center">
      <!-- Hero Section — exact Stitch layout: max-w-7xl px-lg py-4xl lg:flex-row gap-4xl -->
      <section
        class="w-full max-w-7xl px-6 py-16 lg:py-[64px] flex flex-col lg:flex-row items-center gap-10 lg:gap-16 animate-entry"
      >
        <!-- Left: Editorial copy -->
        <div class="flex-1 flex flex-col items-start gap-6 max-w-xl w-full">
          <h1
            class="font-newsreader text-[40px] leading-[1.1] tracking-[-0.03em] font-medium"
            style="font-family: 'Newsreader', serif; color: #0f0f0e"
          >
            {{ t('landing.heroTitle') }}
          </h1>
          <p
            class="font-inter text-[14px] leading-[22px] text-[#464742] max-w-md"
            style="color: #464742"
          >
            {{ t('landing.heroDesc') }}
          </p>
          <router-link
            to="/workbench"
            class="ink-pill mt-2 group hover:bg-[#2a2a28] transition-colors font-inter text-[13px] leading-[20px] font-medium"
          >
            {{ t('landing.cta') }}
          </router-link>
          <router-link
            to="/login"
            class="sm:hidden inline-flex items-center justify-center h-9 px-5 text-sm rounded-full border border-line text-graphite hover:text-ink hover:bg-stone transition font-inter font-medium"
            >Log in to continue</router-link
          >
          <div
            class="mt-4 flex items-center gap-3 font-mono text-[11px] leading-[14px] tracking-[0.06em] font-medium text-[#5d5f5d]"
          >
            <span class="signal-dot !m-0"></span>
            <span>AI-ASSISTED COMPOSITION</span>
          </div>
        </div>

        <!-- Right: Outer Rail / A4 Canvas — Stitch faithful -->
        <div class="flex-1 w-full max-w-2xl relative flex justify-center">
          <!-- Stone rail -->
          <div
            class="w-full max-w-[600px] p-6 rounded-[4px] relative overflow-hidden flex justify-center"
            style="background-color: #f5f3f0; border: 1px solid #eae8e3"
          >
            <!-- Marginalia Rail -->
            <div
              class="absolute left-0 top-0 bottom-0 w-16 hidden sm:flex flex-col items-center py-8 justify-between"
              style="border-right: 1px solid #eae8e3; color: #c7c7c0"
            >
              <span class="font-newsreader text-[20px] font-semibold" style="color: #c7c7c0"
                >I.</span
              >
              <div class="w-px h-32" style="background-color: #c7c7c0"></div>
              <span
                class="font-mono text-[11px] tracking-[0.12em] whitespace-nowrap origin-bottom-left -rotate-90 translate-x-6"
                style="color: #c7c7c0"
                >MASTER PAPER</span
              >
            </div>

            <!-- The A4 Canvas — a4-ratio 210/297, paper shadow -->
            <div
              class="a4-ratio w-full max-w-[420px] bg-white paper-shadow p-6 sm:p-8 sm:ml-12 relative"
              style="background-color: #ffffff"
            >
              <!-- Mock Resume Content -->
              <div class="w-full h-full flex flex-col gap-4 opacity-80">
                <!-- Header -->
                <div class="border-b pb-3 mb-2" style="border-color: #e5e2e0">
                  <div class="h-6 w-1/2 rounded mb-2" style="background-color: #e9e8e4"></div>
                  <div class="h-3 w-1/3 rounded" style="background-color: #efeeea"></div>
                </div>
                <!-- Experience 1 -->
                <div class="flex flex-col gap-2 mb-3 relative">
                  <div class="flex justify-between items-baseline">
                    <div class="h-4 w-1/3 rounded" style="background-color: #e9e8e4"></div>
                    <div class="h-3 w-1/6 rounded" style="background-color: #efeeea"></div>
                  </div>
                  <div class="h-3 w-1/4 rounded" style="background-color: #efeeea"></div>
                  <div class="space-y-1 mt-1">
                    <div class="h-2 w-full rounded" style="background-color: #e3e2df"></div>
                    <div class="h-2 w-11/12 rounded" style="background-color: #e3e2df"></div>
                    <div class="h-2 w-4/5 rounded" style="background-color: #e3e2df"></div>
                  </div>
                </div>
                <!-- Experience 2 -->
                <div class="flex flex-col gap-2">
                  <div class="flex justify-between items-baseline">
                    <div class="h-4 w-2/5 rounded" style="background-color: #e9e8e4"></div>
                    <div class="h-3 w-1/6 rounded" style="background-color: #efeeea"></div>
                  </div>
                  <div class="h-3 w-1/5 rounded" style="background-color: #efeeea"></div>
                  <div class="space-y-1 mt-1">
                    <div class="h-2 w-full rounded" style="background-color: #e3e2df"></div>
                    <div class="h-2 w-10/12 rounded" style="background-color: #e3e2df"></div>
                  </div>
                </div>
                <!-- Footer of canvas -->
                <div
                  class="mt-auto pt-4 border-t flex justify-between items-end"
                  style="border-color: #e5e2e0"
                >
                  <div
                    class="h-8 w-24 rounded-sm flex items-center justify-center"
                    style="background-color: #f5f3f0; border: 1px solid #eae8e3"
                  >
                    <span class="font-mono text-[8px] tracking-widest" style="color: #5d5f5d"
                      >QR CODE</span
                    >
                  </div>
                  <span class="font-newsreader text-[20px] font-semibold" style="color: #c7c7c0"
                    >01</span
                  >
                </div>
              </div>

              <!-- Floating editor active state -->
              <div
                class="absolute top-[33%] -right-3 w-6 h-6 rounded-full flex items-center justify-center paper-shadow"
                style="background-color: #0f0f0e"
              >
                <span class="material-symbols-outlined text-white text-[14px] leading-none"
                  >edit</span
                >
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Atelier Precision Section — 3-col Bento -->
      <section
        class="w-full border-y py-16"
        style="background-color: #f5f3f0; border-color: #eae8e3"
      >
        <div class="max-w-7xl mx-auto px-6">
          <div class="flex flex-col items-center text-center mb-12">
            <span
              class="font-mono text-[11px] tracking-[0.12em] uppercase mb-3"
              style="color: #5d5f5d; font-family: 'JetBrains Mono', monospace"
              >The Philosophy</span
            >
            <h2
              class="font-newsreader text-[28px] leading-[1.2] tracking-[-0.02em] font-medium"
              style="font-family: 'Newsreader', serif; color: #0f0f0e"
            >
              Atelier Precision
            </h2>
            <p class="font-inter text-[14px] leading-[22px] max-w-lg mt-3" style="color: #464742">
              A minimalist, editorial approach prioritizing whitespace and impeccable typography
              over decorative noise.
            </p>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <!-- Bento 1 -->
            <div
              class="bg-white p-8 rounded-lg flex flex-col gap-5 hover:border-ink transition-colors duration-300"
              style="border: 1px solid #eae8e3; background-color: #ffffff"
            >
              <div
                class="w-12 h-12 rounded-full flex items-center justify-center"
                style="background-color: #efeeea; color: #0f0f0e"
              >
                <span class="material-symbols-outlined text-xl">format_size</span>
              </div>
              <div>
                <h3
                  class="font-newsreader text-[20px] leading-[1.3] font-semibold mb-1"
                  style="font-family: 'Newsreader', serif; color: #0f0f0e"
                >
                  Editorial Typography
                </h3>
                <p class="font-inter text-[13px] leading-[20px]" style="color: #464742">
                  Newsreader serifs paired with Inter sans-serif for clear, authoritative reading
                  experiences.
                </p>
              </div>
            </div>
            <!-- Bento 2 -->
            <div
              class="bg-white p-8 rounded-lg flex flex-col gap-5 hover:border-ink transition-colors duration-300"
              style="border: 1px solid #eae8e3; background-color: #ffffff"
            >
              <div
                class="w-12 h-12 rounded-full flex items-center justify-center"
                style="background-color: #efeeea; color: #0f0f0e"
              >
                <span class="material-symbols-outlined text-xl">space_dashboard</span>
              </div>
              <div>
                <h3
                  class="font-newsreader text-[20px] leading-[1.3] font-semibold mb-1"
                  style="font-family: 'Newsreader', serif; color: #0f0f0e"
                >
                  Intentional Whitespace
                </h3>
                <p class="font-inter text-[13px] leading-[20px]" style="color: #464742">
                  The A4 canvas dictates layout. Ample margins allow content to breathe and command
                  attention.
                </p>
              </div>
            </div>
            <!-- Bento 3 -->
            <div
              class="bg-white p-8 rounded-lg flex flex-col gap-5 hover:border-ink transition-colors duration-300"
              style="border: 1px solid #eae8e3; background-color: #ffffff"
            >
              <div
                class="w-12 h-12 rounded-full flex items-center justify-center relative"
                style="background-color: #efeeea; color: #0f0f0e"
              >
                <span class="signal-dot absolute top-2 right-2 !m-0"></span>
                <span class="material-symbols-outlined text-xl">precision_manufacturing</span>
              </div>
              <div>
                <h3
                  class="font-newsreader text-[20px] leading-[1.3] font-semibold mb-1"
                  style="font-family: 'Newsreader', serif; color: #0f0f0e"
                >
                  Mechanical Processing
                </h3>
                <p class="font-inter text-[13px] leading-[20px]" style="color: #464742">
                  AI integration operates quietly in the background, offering refined suggestions
                  without visual clutter.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Templates Preview — Minimalist-Editorial (新增以补足落地页闭环，复用 A4/line/vermilion) -->
      <section class="w-full max-w-7xl mx-auto px-6 py-16">
        <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
          <div>
            <span
              class="font-mono text-[11px] tracking-[0.12em] uppercase"
              style="color: #5d5f5d; font-family: 'JetBrains Mono', monospace"
              >Selected Works</span
            >
            <h2
              class="font-newsreader text-[28px] leading-[1.2] tracking-[-0.02em] font-medium mt-2"
              style="font-family: 'Newsreader', serif; color: #0f0f0e"
            >
              Templates, set in type.
            </h2>
            <p class="font-inter text-[14px] leading-[22px] mt-2 max-w-md" style="color: #464742">
              Three master papers — same grid, different voice. Hairline rules, ink black, vermilion
              accent.
            </p>
          </div>
          <router-link
            to="/templates"
            class="inline-flex items-center gap-2 font-mono text-[11px] tracking-[0.06em] font-medium hover:text-ink transition-colors shrink-0"
            style="color: #0f0f0e; font-family: 'JetBrains Mono', monospace"
          >
            VIEW ALL TEMPLATES
            <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
          </router-link>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <!-- Template 1: Editorial Classic -->
          <router-link
            to="/templates"
            class="group bg-white rounded-lg overflow-hidden flex flex-col hover:shadow-paper transition-all duration-300"
            style="border: 1px solid #eae8e3"
          >
            <div
              class="aspect-[210/297] max-h-[360px] m-4 mb-0 p-5 flex flex-col gap-3"
              style="background-color: #fbf9f5; border: 1px solid #eae8e3"
            >
              <div class="h-3 w-2/3 rounded" style="background-color: #0f0f0e"></div>
              <div class="h-px w-full" style="background-color: #eae8e3"></div>
              <div class="flex gap-3">
                <div class="flex-1 space-y-1.5">
                  <div class="h-2 w-full rounded" style="background-color: #e9e8e4"></div>
                  <div class="h-2 w-5/6 rounded" style="background-color: #e3e2df"></div>
                  <div class="h-2 w-full rounded" style="background-color: #e3e2df"></div>
                </div>
                <div
                  class="w-16 h-16 rounded-sm shrink-0"
                  style="background-color: #efeeea; border: 1px solid #eae8e3"
                ></div>
              </div>
              <div class="mt-auto flex items-center gap-2">
                <span class="w-1.5 h-1.5 rounded-full" style="background-color: #ff3b1f"></span>
                <span class="font-mono text-[9px] tracking-widest" style="color: #8a8a87"
                  >SERIF · A4</span
                >
              </div>
            </div>
            <div class="p-4 flex justify-between items-baseline">
              <span class="font-newsreader text-[16px] font-semibold" style="color: #0f0f0e"
                >Editorial Classic</span
              >
              <span class="font-mono text-[11px]" style="color: #8a8a87">01 — Newsreader</span>
            </div>
          </router-link>

          <!-- Template 2: Modernist -->
          <router-link
            to="/templates"
            class="group bg-white rounded-lg overflow-hidden flex flex-col hover:shadow-paper transition-all duration-300"
            style="border: 1px solid #eae8e3"
          >
            <div
              class="aspect-[210/297] max-h-[360px] m-4 mb-0 p-5 flex flex-col gap-3"
              style="background-color: #fbf9f5; border: 1px solid #eae8e3"
            >
              <div class="flex justify-between items-start">
                <div class="h-3 w-1/2 rounded" style="background-color: #0f0f0e"></div>
                <span class="w-2 h-2 rounded-full mt-0.5" style="background-color: #ff3b1f"></span>
              </div>
              <div class="h-px w-full" style="background-color: #eae8e3"></div>
              <div class="grid grid-cols-[1fr_1.2fr] gap-3 flex-1">
                <div class="space-y-1.5">
                  <div class="h-2 w-full rounded" style="background-color: #1b1c1a"></div>
                  <div class="h-2 w-3/4 rounded" style="background-color: #e3e2df"></div>
                  <div class="h-2 w-full rounded" style="background-color: #e3e2df"></div>
                  <div class="h-2 w-5/6 rounded" style="background-color: #e3e2df"></div>
                </div>
                <div class="space-y-1.5">
                  <div class="h-2 w-full rounded" style="background-color: #e9e8e4"></div>
                  <div class="h-2 w-full rounded" style="background-color: #e3e2df"></div>
                  <div class="h-2 w-4/5 rounded" style="background-color: #e3e2df"></div>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <span class="w-1.5 h-1.5 rounded-full" style="background-color: #0f0f0e"></span>
                <span class="font-mono text-[9px] tracking-widest" style="color: #8a8a87"
                  >GRID · SWISS</span
                >
              </div>
            </div>
            <div class="p-4 flex justify-between items-baseline">
              <span class="font-newsreader text-[16px] font-semibold" style="color: #0f0f0e"
                >Modernist</span
              >
              <span class="font-mono text-[11px]" style="color: #8a8a87">02 — Inter</span>
            </div>
          </router-link>

          <!-- Template 3: Compact — vermilion accent -->
          <router-link
            to="/templates"
            class="group bg-white rounded-lg overflow-hidden flex flex-col hover:shadow-paper transition-all duration-300"
            style="border: 1px solid #eae8e3"
          >
            <div
              class="aspect-[210/297] max-h-[360px] m-4 mb-0 p-5 flex flex-col gap-3"
              style="background-color: #fbf9f5; border: 1px solid #eae8e3"
            >
              <div class="h-2 w-full rounded" style="background-color: #ff3b1f"></div>
              <div class="h-3 w-1/2 rounded" style="background-color: #0f0f0e"></div>
              <div class="h-px w-full" style="background-color: #eae8e3"></div>
              <div class="space-y-1.5 flex-1">
                <div class="h-2 w-full rounded" style="background-color: #e9e8e4"></div>
                <div class="h-2 w-11/12 rounded" style="background-color: #e3e2df"></div>
                <div class="h-2 w-full rounded" style="background-color: #e3e2df"></div>
                <div class="h-2 w-10/12 rounded" style="background-color: #e3e2df"></div>
                <div class="h-2 w-full rounded" style="background-color: #e3e2df"></div>
              </div>
              <div class="flex items-center gap-2">
                <span class="w-1.5 h-1.5 rounded-full" style="background-color: #ff3b1f"></span>
                <span class="font-mono text-[9px] tracking-widest" style="color: #8a8a87"
                  >DENSE · MONO</span
                >
              </div>
            </div>
            <div class="p-4 flex justify-between items-baseline">
              <span class="font-newsreader text-[16px] font-semibold" style="color: #0f0f0e"
                >Compact</span
              >
              <span class="font-mono text-[11px]" style="color: #8a8a87">03 — Mono</span>
            </div>
          </router-link>
        </div>
      </section>
    </main>

    <!-- Footer — Stitch faithful: bg-surface-container-low border-t -->
    <footer
      class="border-t flex flex-col md:flex-row justify-between items-center gap-4 px-6 lg:px-10 w-full py-6"
      style="background-color: #f5f3f0; border-color: #c7c7c0"
    >
      <span
        class="font-newsreader text-[20px] font-semibold"
        style="font-family: 'Newsreader', serif; color: #0f0f0e"
        >Resume Atelier</span
      >
      <span
        class="font-mono text-[11px] tracking-[0.04em]"
        style="color: #5d5f5d; font-family: 'JetBrains Mono', monospace"
        >© {{ new Date().getFullYear() }} Resume Atelier. Master Paper Workshop.</span
      >
      <div class="flex gap-6">
        <a
          href="#"
          class="font-mono text-[11px] tracking-[0.06em] hover:text-ink transition-colors"
          style="color: #5d5f5d; font-family: 'JetBrains Mono', monospace"
          >Terms</a
        >
        <a
          href="#"
          class="font-mono text-[11px] tracking-[0.06em] hover:text-ink transition-colors"
          style="color: #5d5f5d; font-family: 'JetBrains Mono', monospace"
          >Privacy</a
        >
        <a
          href="#"
          class="font-mono text-[11px] tracking-[0.06em] hover:text-ink transition-colors"
          style="color: #5d5f5d; font-family: 'JetBrains Mono', monospace"
          >Support</a
        >
      </div>
    </footer>
  </div>
</template>

<style scoped>
  .a4-ratio {
    aspect-ratio: 210 / 297;
  }
  .paper-shadow {
    box-shadow:
      0px 4px 24px rgba(0, 0, 0, 0.04),
      0px 2px 8px rgba(0, 0, 0, 0.02);
  }
  .ink-pill {
    background-color: #0f0f0e;
    color: white;
    border-radius: 9999px;
    height: 36px;
    padding: 0 24px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
  .signal-dot {
    width: 6px;
    height: 6px;
    background-color: #ff3b1f;
    border-radius: 50%;
    display: inline-block;
    margin-right: 8px;
  }
  @keyframes pageEntry {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  .animate-entry {
    animation: pageEntry 380ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
</style>
