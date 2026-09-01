<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import client from '@/api/client'
  import { pushToast } from '@/composables/useToast'
  const stats = ref<any>({ totalUsers: 0, totalTemplates: 0, totalResumes: 0, pendingAudits: 0 })
  const audits = ref<any[]>([])
  const auditsLoading = ref(false)
  const auditPage = ref(1)
  async function fetchAudits() {
    auditsLoading.value = true
    try {
      const { data } = await client.get('/admin/audits', { params: { page: auditPage.value, size: 5, status: 'pending' } })
      const pg: any = (data as any)?.data ?? data
      audits.value = pg?.records ?? pg ?? []
    } catch {
      audits.value = []
    } finally {
      auditsLoading.value = false
    }
  }
  async function reviewAudit(id: string, action: 'approve' | 'reject') {
    try {
      await client.post(`/admin/audits/${id}/${action}`, { note: action === 'reject' ? 'admin reject' : '' })
      pushToast(action === 'approve' ? '已通过' : '已驳回')
      await fetchAudits()
      // refresh stats
      try {
        const { data } = await client.get('/admin/audits/stats')
        stats.value.pendingAudits = (data as any)?.data?.pending ?? stats.value.pendingAudits
      } catch {}
    } catch (e: any) {
      pushToast(e?.response?.data?.message || '操作失败')
    }
  }

  onMounted(async () => {
    try {
      const [u, t, r, a] = await Promise.all([
        client
          .get('/admin/users/stats')
          .then((r) => r.data.data)
          .catch(() => ({ totalUsers: 0 })),
        client
          .get('/admin/templates/stats')
          .then((r) => r.data.data)
          .catch(() => ({ totalTemplates: 0 })),
        client
          .get('/admin/resumes/stats')
          .then((r) => r.data.data)
          .catch(() => ({ totalResumes: 0 })),
        client
          .get('/admin/audits/stats')
          .then((r) => r.data.data)
          .catch(() => ({ pending: 0 })),
      ])
      stats.value = {
        totalUsers: u.totalUsers || u.total || 0,
        totalTemplates: t.totalTemplates || t.total || 0,
        totalResumes: r.totalResumes || r.total || 0,
        pendingAudits: a.pending || 0,
      }
    } catch {}
    fetchAudits()
  })
</script>
<template>
  <div class="p-10 lg:p-12 flex flex-col gap-8">
    <div class="flex justify-between items-end border-b border-[#eae8e3] pb-6">
      <div>
        <h1 class="font-[Newsreader] text-[28px] font-medium text-black">后台管理</h1>
        <p class="font-[JetBrains_Mono] text-[11px] tracking-widest uppercase text-[#464742] mt-2">
          用户 · 模板 · 简历 · 审核 · AI 规则
        </p>
      </div>
      <span class="font-[JetBrains_Mono] text-[11px] px-3 py-1 rounded-full bg-black text-white"
        >ADMIN</span
      >
    </div>

    <div class="grid grid-cols-4 gap-4">
      <div class="bg-white border border-[#eae8e3] rounded-xl p-6">
        <p class="font-[JetBrains_Mono] text-[11px] uppercase tracking-widest text-[#464742]">
          总用户
        </p>
        <p class="font-[JetBrains_Mono] text-[28px] text-black mt-2">{{ stats.totalUsers }}</p>
      </div>
      <div class="bg-white border border-[#eae8e3] rounded-xl p-6">
        <p class="font-[JetBrains_Mono] text-[11px] uppercase tracking-widest text-[#464742]">
          模板
        </p>
        <p class="font-[JetBrains_Mono] text-[28px] text-black mt-2">{{ stats.totalTemplates }}</p>
      </div>
      <div class="bg-white border border-[#eae8e3] rounded-xl p-6">
        <p class="font-[JetBrains_Mono] text-[11px] uppercase tracking-widest text-[#464742]">
          简历
        </p>
        <p class="font-[JetBrains_Mono] text-[28px] text-black mt-2">{{ stats.totalResumes }}</p>
      </div>
      <div class="bg-white border border-[#eae8e3] rounded-xl p-6 border-l-2 border-l-black">
        <p class="font-[JetBrains_Mono] text-[11px] uppercase tracking-widest text-[#464742]">
          待审核
        </p>
        <p class="font-[JetBrains_Mono] text-[28px] text-black mt-2 flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-[#FF3B1F]"></span>{{ stats.pendingAudits }}
        </p>
      </div>
    </div>

    <!-- Audit 队列 — Editorial hairline 12px -->
    <section class="bg-white border border-[#eae8e3] rounded-xl overflow-hidden">
      <div class="flex items-center justify-between px-6 py-4 border-b bg-[#fbf9f5]" style="border-color:#eae8e3">
        <div class="flex items-center gap-2">
          <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span>
          <h3 class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase font-medium">待审核队列 · pending</h3>
          <span class="font-[JetBrains_Mono] text-[11px] px-2 py-0.5 rounded-full bg-white" style="border:0.5px solid #eae8e3">{{ stats.pendingAudits }} 条</span>
        </div>
        <div class="flex gap-2">
          <button @click="fetchAudits" class="h-7 px-3 rounded-full border text-[11px] font-[JetBrains_Mono] bg-white hover:border-black" style="border-color:#eae8e3">刷新</button>
          <button @click="auditPage=Math.max(1,auditPage-1); fetchAudits()" class="h-7 w-7 rounded-full border bg-white flex items-center justify-center" style="border-color:#eae8e3"><span class="material-symbols-outlined text-[16px]">chevron_left</span></button>
          <button @click="auditPage++; fetchAudits()" class="h-7 w-7 rounded-full border bg-white flex items-center justify-center" style="border-color:#eae8e3"><span class="material-symbols-outlined text-[16px]">chevron_right</span></button>
        </div>
      </div>
      <div v-if="auditsLoading" class="p-6 flex flex-col gap-3">
        <div v-for="i in 3" :key="i" class="h-12 bg-[#f5f3f0] rounded-lg animate-pulse"></div>
      </div>
      <div v-else-if="audits.length" class="divide-y" style="border-color:#eae8e3">
        <div v-for="a in audits" :key="a.id || a.auditId" class="px-6 py-4 flex items-center justify-between gap-4 hover:bg-[#fbf9f5]">
          <div class="min-w-0">
            <p class="font-[Inter] text-[13px] font-medium text-black truncate">{{ a.title || a.targetTitle || a.targetId || '—' }}</p>
            <p class="font-[JetBrains_Mono] text-[11px] mt-1" style="color:#8a8a87">{{ a.status || 'pending' }} · {{ a.riskLevel || 'low' }} · {{ String(a.createdAt||'').slice(0,16).replace('T',' ') }}</p>
          </div>
          <div class="flex gap-2 shrink-0">
            <button @click="reviewAudit(a.id||a.auditId, 'approve')" class="h-7 px-3 rounded-full bg-black text-white text-[11px] font-[JetBrains_Mono]">通过</button>
            <button @click="reviewAudit(a.id||a.auditId, 'reject')" class="h-7 px-3 rounded-full border bg-white text-[11px] font-[JetBrains_Mono] hover:border-black" style="border-color:#eae8e3">驳回</button>
          </div>
        </div>
      </div>
      <div v-else class="p-12 text-center">
        <span class="material-symbols-outlined text-[20px]" style="color:#c7c7c0">verified</span>
        <p class="font-[JetBrains_Mono] text-[11px] mt-2" style="color:#8a8a87">暂无待审核</p>
      </div>
    </section>

    <div class="grid grid-cols-2 gap-4">
      <router-link
        to="/resumes"
        class="bg-white border border-[#eae8e3] rounded-xl p-4 hover:border-black transition-colors"
      >
        <p class="font-medium text-black">简历管理</p>
        <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-1">GET /admin/resumes</p>
      </router-link>
      <router-link
        to="/templates"
        class="bg-white border border-[#eae8e3] rounded-xl p-4 hover:border-black transition-colors"
      >
        <p class="font-medium text-black">模板管理</p>
        <p class="font-[JetBrains_Mono] text-[11px] text-[#464742] mt-1">GET /admin/templates</p>
      </router-link>
    </div>
  </div>
</template>
