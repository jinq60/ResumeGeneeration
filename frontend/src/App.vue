<template>
  <el-config-provider :locale="zhCn">
    <router-view />
    <LoginModal v-model="authModalStore.isOpen" />
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import LoginModal from '@/components/auth/LoginModal.vue'
import { useAuthModalStore } from '@/stores/authModal'

const authModalStore = useAuthModalStore()
</script>

<style scoped>
#app {
  width: 100%;
  height: 100%;
}
</style>
