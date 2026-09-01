<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { useResumeStore } from '@/stores/resume'
import { useI18n } from 'vue-i18n'
import { pushToast } from '@/composables/useToast'
import client from '@/api/client'
const { t } = useI18n()
const store = useResumeStore()
const router = useRouter()
const deliveryTotal = ref<number | null>(null)
async function fetchDeliveryTotal() {
  try {
    const { data } = await client.get('/deliveries', { params: { page: 1, size: 1 } })
    const pg: any = (data as any)?.data ?? data
    deliveryTotal.value = typeof pg?.total === 'number' ? pg.total : null
  } catch {
    deliveryTotal.value = null
  }
}
onMounted(async () => {
  await Promise.all([store.fetchList(1, 6), fetchDeliveryTotal()])
})
async function createBlank() {
  try {
    const r = await store.create({
      scene: 'social_recruitment',
      templateId: 'template_classic_single',
      title: '我的简历',
    })
    pushToast('创建成功')
    router.push(`/editor/${r.id}`)
  } catch (e: any) {
    pushToast(e.response?.data?.message || '创建失败')
  }
}
</script>
<template>
  <div
    class="flex-1 overflow-y-auto bg-[#fbf9f5] p-10 md:p-16 flex flex-col gap-10 md:gap-16 font-[Inter] text-[#1b1c1a]"
  >
    <!-- Header -->
    <header class="flex justify-between items-end border-b border-[#eae8e3] pb-6">
      <div>
        <h2 class="font-[Newsreader] text-[28px] font-medium text-black">
          {{ t('workbench.title') }}
        </h2>
        <p class="text-[14px] text-[#464742] mt-2">{{ t('workbench.welcome') }}</p>
      </div>
      <div class="flex items-center gap-4">
        <button
          class="px-6 h-9 border border-[#eae8e3] rounded-full text-[13px] text-black hover:border-black transition-colors"
        >
          {{ t('workbench.importLinkedIn') }}
        </button>
        <button
          @click="createBlank"
          class="px-6 h-9 bg-black text-white rounded-full text-[13px] hover:opacity-90"
        >
          {{ t('workbench.newBlankDraft') }}
        </button>
      </div>
    </header>

     <!-- Stats — 真实优先，空时 fallback，不覆盖 -->
    <section class="grid grid-cols-3 gap-6">
      <div class="p-6 border border-[#eae8e3] rounded-lg bg-white">
        <p class="font-[JetBrains_Mono] text-[11px] uppercase tracking-widest text-[#464742] mb-4">
          {{ t('workbench.totalDrafts') }}
        </p>
        <p class="font-[JetBrains_Mono] text-[32px] leading-none text-black">
          {{ store.total || store.list.length || 0 }}
        </p>
      </div>
      <div class="p-6 border border-[#eae8e3] rounded-lg bg-white">
        <p class="font-[JetBrains_Mono] text-[11px] uppercase tracking-widest text-[#464742] mb-4">
          {{ t('workbench.lastPolished') }}
        </p>
        <p class="font-[JetBrains_Mono] text-[32px] leading-none text-black">
          {{ store.list[0]?.updatedAt ? store.list[0].updatedAt.slice(0, 10) : '—' }}
        </p>
      </div>
      <div class="p-6 border border-[#eae8e3] rounded-lg bg-white">
        <p class="font-[JetBrains_Mono] text-[11px] uppercase tracking-widest text-[#464742] mb-4">
          {{ t('workbench.deliveryRate') }}
        </p>
        <p class="font-[JetBrains_Mono] text-[32px] leading-none text-black">
          {{ deliveryTotal !== null ? `${deliveryTotal}` : (store.list.length ? '—' : '94%') }}
        </p>
        <p v-if="deliveryTotal !== null" class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] uppercase mt-1" style="color:#8a8a87">deliveries total</p>
      </div>
    </section>

    <!-- Grid -->
    <div class="grid grid-cols-12 gap-6">
      <!-- Recent -->
      <section class="col-span-8 flex flex-col gap-4">
        <div class="flex justify-between items-center">
          <h3 class="font-[Newsreader] text-[20px] font-semibold text-black">
            {{ t('workbench.recent') }}
          </h3>
          <router-link to="/resumes" class="text-[13px] text-[#464742] hover:text-black">{{
            t('workbench.viewAll')
          }}</router-link>
        </div>
        <div class="grid grid-cols-3 gap-4">
          <div
            v-for="r in store.list.slice(0, 2)"
            :key="r.id"
            class="group cursor-pointer"
            @click="router.push(`/editor/${r.id}`)"
          >
            <div
              class="aspect-[210/297] bg-white border border-[#eae8e3] shadow-[0_4px_24px_rgba(0,0,0,0.04)] p-3 mb-3 relative overflow-hidden transition-transform duration-300 group-hover:-translate-y-1"
            >
              <div class="w-full h-2 bg-[#e9e8e4] mb-2"></div>
              <div class="w-3/4 h-2 bg-[#e9e8e4] mb-6"></div>
              <div class="space-y-2">
                <div class="h-1 bg-[#f5f3f0] w-full"></div>
                <div class="h-1 bg-[#f5f3f0] w-full"></div>
                <div class="h-1 bg-[#f5f3f0] w-5/6"></div>
              </div>
              <span class="absolute bottom-2 right-2 w-2 h-2 rounded-full bg-[#FF3B1F]"></span>
            </div>
            <p class="text-[13px] font-medium text-black truncate">{{ r.title }}</p>
            <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-1">
              {{ t('workbench.edited') }} {{ r.updatedAt.slice(0, 10) }}
            </p>
          </div>
          <!-- New Draft Card -->
          <div
            @click="createBlank"
            class="group cursor-pointer flex flex-col justify-center items-center border border-dashed border-[#c7c7c0] aspect-[210/297] rounded-sm hover:border-black hover:bg-[#f5f3f0] transition-colors"
          >
            <span class="material-symbols-outlined text-[#777871] mb-2">add</span>
            <p class="text-[13px] text-[#464742]">{{ t('workbench.newDraft') }}</p>
          </div>
        </div>
      </section>

      <!-- AI Insights -->
      <section class="col-span-4 flex flex-col gap-6">
        <div class="border border-[#eae8e3] rounded-xl p-6 bg-white">
          <div class="flex items-center gap-2 mb-6">
            <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span>
            <h3 class="font-[Newsreader] text-[20px] font-semibold text-black">
              {{ t('workbench.ai') }}
            </h3>
          </div>
          <div class="flex flex-col gap-3">
            <button
              @click="router.push('/ai/review/demo')"
              class="w-full text-left p-3 border border-[#eae8e3] rounded-lg hover:border-black group text-left"
            >
              <div class="flex justify-between items-center">
                <span class="text-[13px] font-medium text-black">{{ t('workbench.scan') }}</span>
                <span
                  class="material-symbols-outlined text-[#777871] group-hover:text-black text-[18px]"
                  >document_scanner</span
                >
              </div>
              <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-1">
                {{ t('workbench.scanDesc') }}
              </p>
            </button>
            <button
              class="w-full text-left p-3 border border-[#eae8e3] rounded-lg hover:border-black group"
            >
              <div class="flex justify-between items-center">
                <span class="text-[13px] font-medium text-black">{{ t('workbench.gap') }}</span>
                <span
                  class="material-symbols-outlined text-[#777871] group-hover:text-black text-[18px]"
                  >radar</span
                >
              </div>
              <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-1">
                {{ t('workbench.gapDesc') }}
              </p>
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
