<template>
  <img
    v-if="name === 'google'"
    :src="googleIcon"
    :width="size"
    :height="size"
    alt="Google"
    class="select-none"
  >
  <img
    v-else-if="name === 'github'"
    :src="githubIcon"
    :width="size"
    :height="size"
    alt="GitHub"
    class="select-none"
  >
  <img
    v-else-if="name === 'qq'"
    :src="qqIcon"
    :width="size"
    :height="size"
    alt="QQ"
    class="select-none"
  >
</template>

<script setup lang="ts">
import googleIcon from '@/assets/icons/google.svg'
import githubIcon from '@/assets/icons/GitHub.svg'
import qqIcon from '@/assets/icons/QQ.svg'

withDefaults(defineProps<{ name: 'google' | 'github' | 'qq'; size?: number }>(), {
  size: 18
})
</script>
