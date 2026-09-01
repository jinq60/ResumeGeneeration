import { ref } from 'vue'
const msg = ref('')
let timer: any = null
export function useToast() {
  function toast(m: string) {
    msg.value = m
    clearTimeout(timer)
    timer = setTimeout(() => (msg.value = ''), 2500)
  }
  return { msg, toast }
}
// global singleton for AtelierLayout
import { reactive } from 'vue'
export const toastState = reactive<{ text: string }>({ text: '' })
export function pushToast(t: string) {
  toastState.text = t
  setTimeout(() => (toastState.text = ''), 2500)
}
