<script setup lang="ts">
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import { useI18n } from 'vue-i18n'
  import AtButton from '@/components/atelier/AtButton.vue'
  import AtInput from '@/components/atelier/AtInput.vue'
  import LangSwitch from '@/components/atelier/LangSwitch.vue'
  const { t } = useI18n()
  const router = useRouter()
  const auth = useAuthStore()
  const account = ref('')
  const password = ref('')
  const err = ref('')
  async function submit() {
    try {
      await auth.login(account.value, password.value)
      router.push('/workbench')
    } catch (e: any) {
      err.value = e.response?.data?.message || '登录失败'
    }
  }
  async function guest() {
    await auth.guest()
    router.push('/workbench')
  }
</script>
<template>
  <div class="min-h-screen bg-[#fbf9f5] flex">
    <!-- Left — Editorial -->
    <div class="hidden lg:flex w-[48%] bg-[#0f0f0e] text-white p-12 flex-col justify-between">
      <div class="font-[Newsreader] text-[20px]">Resume Atelier</div>
      <div>
        <p class="font-[Newsreader] text-[40px] leading-[1.1] tracking-[-0.03em]">
          Precision,<br />not decoration.
        </p>
        <p class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] uppercase text-white/60 mt-6">
          Master Paper Workshop — 让简历回归纸张精度
        </p>
        <div class="mt-8 flex gap-2">
          <span class="w-8 h-[1px] bg-white/20 mt-2"></span>
          <p class="text-sm text-white/70 max-w-sm">
            A4 为画布，hairline 为结构，vermilion 为信号。极简 Editorial 只为让你的经历被看见。
          </p>
        </div>
      </div>
      <p class="font-[JetBrains_Mono] text-[11px] text-white/40">
        © 2026 Atelier Editorial · AI PRECISION
      </p>
    </div>
    <!-- Right — Form -->
    <div class="flex-1 flex items-center justify-center p-6 bg-[#fbf9f5] relative">
      <div class="absolute top-6 right-6"><LangSwitch /></div>
      <div
        class="w-full max-w-sm bg-white border border-[#eae8e3] rounded-xl p-6 shadow-[0_4px_24px_rgba(0,0,0,0.04)]"
      >
        <h1 class="font-[Newsreader] text-[24px] text-center text-black">{{ t('login.title') }}</h1>
        <p
          class="font-[JetBrains_Mono] text-[11px] text-center text-[#777871] mt-1 tracking-widest uppercase"
        >
          {{ t('login.welcome') }}
        </p>
        <div class="mt-6 space-y-3">
          <AtInput v-model="account" :label="t('login.account')" placeholder="13800000000" />
          <AtInput v-model="password" :label="t('login.password')" type="password" />
          <p
            v-if="err"
            class="text-sm text-[#ba1a1a] bg-[#ffdad6] border border-[#ffb4a6] rounded-lg px-3 py-2"
          >
            {{ err }}
          </p>
          <AtButton class="w-full" @click="submit">{{ t('login.login') }}</AtButton>
          <AtButton variant="ghost" class="w-full" @click="guest">
            <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span> {{ t('login.guest') }}
          </AtButton>
          <p class="font-[JetBrains_Mono] text-[11px] text-center text-[#777871]">
            50/day guest · 30/day AI
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
