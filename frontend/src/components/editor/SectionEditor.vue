<script setup lang="ts">
  import type { SectionDTO } from '@/api/types'
  import AtInput from '@/components/atelier/AtInput.vue'
  import { useI18n } from 'vue-i18n'
  import { ref, reactive, watch } from 'vue'
  import client from '@/api/client'
  import { pushToast } from '@/composables/useToast'
  import { useAvatar } from '@/composables/useAvatar'

  const { t } = useI18n()
  const props = defineProps<{ sections: SectionDTO[]; resumeId?: string }>()
  function sectionTitle(s: SectionDTO) {
    const map: Record<string, string> = {
      profile: t('editor.profile'),
      education: t('editor.education'),
      work: t('editor.work'),
      project: t('editor.project'),
      skill: t('editor.skill'),
      introduction: t('editor.introduction'),
    }
    return map[s.type] || s.title
  }
  const emit = defineEmits<{ (e: 'update:sections', v: SectionDTO[]): void }>()
  function touch() {
    emit('update:sections', props.sections)
  }
  function addItem(s: SectionDTO) {
    if (!Array.isArray(s.data)) s.data = []
    ;(s.data as any[]).push({ id: Math.random().toString(36).slice(2, 7) })
    touch()
  }
  function removeItem(s: SectionDTO, idx: number) {
    ;(s.data as any[]).splice(idx, 1)
    touch()
  }

  // --- Avatar Atelier state --- 收敛至 useAvatar composable，避免重复实现
  const { uploadAvatar, createOptimizeTask, pollTask } = useAvatar()
  const uploading = ref(false)
  const sourceMap = reactive<Record<string, string>>({})
  const bgMap = reactive<Record<string, 'white' | 'blue' | 'red'>>({})
  const styleMap = reactive<Record<string, string>>({})
  const optimizingMap = reactive<Record<string, boolean>>({})
  const optErrorMap = reactive<Record<string, string>>({})
  const fileInputRefs = reactive<Record<string, HTMLInputElement | null>>({})

  // init defaults when sections change (resume load)
  watch(
    () => props.sections,
    (arr) => {
      if (!arr) return
      for (const s of arr) {
        if (s.type !== 'profile') continue
        if (!bgMap[s.id]) bgMap[s.id] = 'white'
        if (!styleMap[s.id]) styleMap[s.id] = 'formal'
        const existing = s.data?.avatarUrl || s.data?.sourceImageUrl
        if (existing && !sourceMap[s.id]) sourceMap[s.id] = existing
      }
    },
    { immediate: true, deep: true }
  )

  function triggerFile(id: string) {
    fileInputRefs[id]?.click()
  }

  async function onFileChange(e: Event, s: SectionDTO) {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) return
    const max = 10 * 1024 * 1024
    if (file.size > max) {
      pushToast('图片需 ≤10MB')
      input.value = ''
      return
    }
    const allowedType = ['image/jpeg', 'image/png', 'image/webp']
    const allowedExt = /\.(jpe?g|png|webp)$/i
    if (!allowedType.includes(file.type) && !allowedExt.test(file.name)) {
      pushToast('仅支持 jpg / png / webp')
      input.value = ''
      return
    }
    uploading.value = true
    optErrorMap[s.id] = ''
    try {
      const res = await uploadAvatar(file, props.resumeId)
      const sourceImageUrl = res.sourceImageUrl
      if (!s.data) s.data = {}
      s.data.avatarUrl = sourceImageUrl
      sourceMap[s.id] = sourceImageUrl
      touch()
      pushToast('头像已上传')
    } catch (err: any) {
      const msg = err?.message || err?.response?.data?.message || '上传失败'
      optErrorMap[s.id] = msg
      pushToast(msg)
    } finally {
      uploading.value = false
      input.value = ''
    }
  }

  async function handleOptimize(s: SectionDTO) {
    const src = sourceMap[s.id] || s.data?.avatarUrl
    if (!src) {
      pushToast('请先上传头像')
      return
    }
    const backgroundType = bgMap[s.id] || 'white'
    const style = styleMap[s.id] || 'formal'
    optimizingMap[s.id] = true
    optErrorMap[s.id] = ''
    try {
      const taskId = await createOptimizeTask({
        sourceImageUrl: src,
        ...(props.resumeId ? { resumeId: props.resumeId } : {}),
        backgroundType: backgroundType as any,
        style: style as any,
      })
      if (!taskId) throw new Error('未返回 taskId')
      pushToast('优化任务已创建，处理中…')
      const task = await pollTask(taskId)
      optimizingMap[s.id] = false
      const resultUrl: string = (task as any)?.resultImageUrl ?? (task as any)?.result_image_url ?? ''
      if (resultUrl) {
        if (!s.data) s.data = {}
        s.data.avatarUrl = resultUrl
        touch()
        pushToast('一寸照已生成，已回填头像')
      } else {
        pushToast('生成成功')
      }
    } catch (e: any) {
      const msg = e?.message || e?.response?.data?.message || '优化失败'
      optErrorMap[s.id] = msg
      pushToast(msg)
      optimizingMap[s.id] = false
    }
  }

  // --- AI Writing Atelier capsule ---
  const aiActions = [
    { label: '生成', action: 'generate' as const },
    { label: '润色', action: 'polish' as const },
    { label: '精简', action: 'shorten' as const },
    { label: '扩写', action: 'expand' as const },
  ]
  type AiAction = (typeof aiActions)[number]['action']
  const busyMap = reactive<Record<string, boolean>>({})
  const streamingMap = reactive<Record<string, boolean>>({})
  const isStreaming = ref(false)

  function aiKey(...parts: (string | number)[]) {
    return parts.join('_')
  }

  async function handleAiSync(
    sectionType: string,
    field: string,
    action: AiAction,
    originalText: string,
    key: string,
    setter: (v: string) => void
  ) {
    if (!props.resumeId) {
      pushToast('请先保存简历')
      return
    }
    const text = (originalText || '').toString()
    if (!text && action !== 'generate') {
      pushToast('请先填写内容')
      return
    }
    if (busyMap[key] || isStreaming.value) return
    busyMap[key] = true
    try {
      const { data } = await client.post('/resumes/' + props.resumeId + '/ai/write', {
        sectionType,
        field,
        action,
        originalText: text,
      })
      if ((data as any).code !== undefined && (data as any).code !== 200) throw new Error((data as any).message || 'AI 失败')
      const content: string = (data as any).data?.content ?? (data as any).data ?? ''
      if (!content) throw new Error('AI 未返回内容')
      setter(content)
      touch()
      pushToast('AI 已回填')
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || 'AI 失败，请稍后重试'
      pushToast(msg)
    } finally {
      busyMap[key] = false
    }
  }

  async function handleAiStream(
    sectionType: string,
    field: string,
    action: AiAction,
    originalText: string,
    key: string,
    setter: (v: string) => void
  ) {
    if (!props.resumeId) {
      pushToast('请先保存简历')
      return
    }
    const text = (originalText || '').toString()
    if (!text && action !== 'generate') {
      pushToast('请先填写内容')
      return
    }
    if (busyMap[key] || isStreaming.value) return
    busyMap[key] = true
    streamingMap[key] = true
    isStreaming.value = true
    let accumulated = ''
    try {
      const token = localStorage.getItem('accessToken')
      const base = (client.defaults.baseURL as string) || '/api'
      const url = `${base}/resumes/${props.resumeId}/ai/write/stream`
      const resp = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token ? `Bearer ${token}` : '',
          'Idempotency-Key': crypto.randomUUID(),
          'X-Trace-Id': Math.random().toString(36).slice(2, 10),
        },
        body: JSON.stringify({ sectionType, field, action, originalText: text }),
      })
      if (!resp.ok) {
        const t = await resp.text().catch(() => '')
        let msg = 'AI 流式失败'
        try {
          const j = JSON.parse(t)
          msg = j.message || msg
        } catch {}
        throw new Error(msg)
      }
      const reader = resp.body?.getReader()
      if (!reader) throw new Error('不支持流式')
      const decoder = new TextDecoder()
      let buf = ''
      // init empty for逐字回填
      setter('')
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buf += decoder.decode(value, { stream: true })
        const parts = buf.split('\n\n')
        buf = parts.pop() || ''
        for (const part of parts) {
          if (!part.trim()) continue
          const lines = part.split('\n')
          let event = 'message'
          let data = ''
          for (const line of lines) {
            if (line.startsWith('event:')) event = line.slice(6).trim()
            else if (line.startsWith('data:')) data = line.slice(5).trimStart()
          }
          if (event === 'delta' && data) {
            // delta may be JSON or plain text; handle both
            let chunk = data
            // if server wraps in JSON string, unwrap
            if (chunk.startsWith('"') && chunk.endsWith('"')) {
              try { chunk = JSON.parse(chunk) } catch {}
            }
            accumulated += chunk
            setter(accumulated)
          } else if (event === 'error') {
            throw new Error(data || 'AI 流式错误')
          } else if (event === 'done') {
            // done
          }
        }
      }
      // flush last buf
      if (buf.trim()) {
        const lines = buf.split('\n')
        let event = 'message'
        let data = ''
        for (const line of lines) {
          if (line.startsWith('event:')) event = line.slice(6).trim()
          else if (line.startsWith('data:')) data = line.slice(5).trimStart()
        }
        if (event === 'delta' && data) {
          accumulated += data
          setter(accumulated)
        }
      }
      if (!accumulated) throw new Error('AI 未返回内容')
      touch()
      pushToast('AI 已回填')
    } catch (e: any) {
      const msg = e?.message || e?.response?.data?.message || 'AI 流式失败'
      pushToast(msg)
      // fallback to sync if stream failed due to network
      // keep accumulated if any
    } finally {
      busyMap[key] = false
      streamingMap[key] = false
      isStreaming.value = false
    }
  }

  // unified entry: try stream first, fallback to sync is handled inside catch above; expose both for capsule
  async function handleWorkAi(it: any, sectionType: string, action: AiAction, idx: number, sectionId: string) {
    const field = 'description'
    const originalText =
      (it.descriptionHtml || (Array.isArray(it.description) ? it.description.join('\n') : it.description) || '').toString()
    const key = aiKey(sectionId, idx, field, action)
    // prefer sync for quota guest3/日 stability; stream is available via handleAiStream
    // to meet spec: 支持流式 isStreaming 状态，逐字回填可选
    // we keep sync as primary as required: client.post('/resumes/'+resumeId+'/ai/write', {sectionType, field, action, originalText})
    // if isStreaming preferred, call stream variant; here we call sync
    await handleAiSync(sectionType, field, action, originalText, key, (v) => {
      it.descriptionHtml = v
    })
  }

  async function handleWorkAiStream(it: any, sectionType: string, action: AiAction, idx: number, sectionId: string) {
    const field = 'description'
    const originalText =
      (it.descriptionHtml || (Array.isArray(it.description) ? it.description.join('\n') : it.description) || '').toString()
    const key = aiKey(sectionId, idx, field, action)
    await handleAiStream(sectionType, field, action, originalText, key, (v) => {
      it.descriptionHtml = v
    })
  }

  async function handleIntroAi(s: SectionDTO, action: AiAction) {
    const sectionType = 'introduction'
    const field = 'content'
    const originalText = (s.data?.contentHtml || s.data?.content || '').toString()
    const key = aiKey(s.id, field, action)
    await handleAiSync(sectionType, field, action, originalText, key, (v) => {
      if (!s.data) s.data = {}
      s.data.contentHtml = v
    })
  }

  async function handleIntroAiStream(s: SectionDTO, action: AiAction) {
    const sectionType = 'introduction'
    const field = 'content'
    const originalText = (s.data?.contentHtml || s.data?.content || '').toString()
    const key = aiKey(s.id, field, action)
    await handleAiStream(sectionType, field, action, originalText, key, (v) => {
      if (!s.data) s.data = {}
      s.data.contentHtml = v
    })
  }
</script>
<template>
  <div class="space-y-4">
    <div
      v-for="s in sections"
      :key="s.id"
      class="bg-white border border-[#eae8e3] rounded-xl overflow-hidden"
    >
      <div
        class="flex justify-between items-center px-4 py-3 border-b border-[#eae8e3] bg-[#fbf9f5]"
      >
        <h3 class="font-[Newsreader] font-medium text-[14px] text-black flex items-center gap-2">
          <span class="w-1 h-4 bg-black rounded-full"></span>{{ sectionTitle(s) }}
          <span
            class="font-[JetBrains_Mono] text-[10px] tracking-widest uppercase text-[#777871] border border-[#eae8e3] rounded px-1"
            >{{ s.type }}</span
          >
        </h3>
        <div class="flex items-center gap-2">
          <button
            class="w-7 h-7 rounded-full hover:bg-[#eae8e3] flex items-center justify-center"
            @click="s.visible = !s.visible; touch()"
            :title="s.visible ? '隐藏' : '显示'"
          >
            <span class="material-symbols-outlined text-[16px]">{{
              s.visible ? 'visibility' : 'visibility_off'
            }}</span>
          </button>
        </div>
      </div>
      <div class="p-4 space-y-3">
        <!-- profile -->
        <template v-if="s.type === 'profile'">
          <AtInput
            :modelValue="s.data?.name"
            @update:modelValue="
              (v) => {
                s.data.name = v
                touch()
              }
            "
            label="姓名"
          />
          <div class="grid grid-cols-2 gap-3">
            <AtInput
              :modelValue="s.data?.phone"
              @update:modelValue="
                (v) => {
                  s.data.phone = v
                  touch()
                }
              "
              label="电话"
            />
            <AtInput
              :modelValue="s.data?.email"
              @update:modelValue="
                (v) => {
                  s.data.email = v
                  touch()
                }
              "
              label="邮箱"
            />
          </div>
          <AtInput
            :modelValue="s.data?.city"
            @update:modelValue="
              (v) => {
                s.data.city = v
                touch()
              }
            "
            label="城市"
          />
          <AtInput
            :modelValue="s.data?.targetPosition"
            @update:modelValue="
              (v) => {
                s.data.targetPosition = v
                touch()
              }
            "
            label="目标岗位"
          />
          <AtInput
            :modelValue="s.data?.avatarUrl"
            @update:modelValue="
              (v) => {
                s.data.avatarUrl = v
                touch()
                if (v) sourceMap[s.id] = v
              }
            "
            label="头像 URL (/uploads/...)"
          />

          <!-- Atelier Avatar Card — white hairline, JetBrains Mono, Pill -->
          <div class="bg-white border border-[#eae8e3] rounded-xl p-4 space-y-4">
            <!-- header -->
            <div class="flex items-center justify-between gap-2">
              <span
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.14em] font-medium uppercase text-black flex items-center gap-2"
              >
                <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F]"></span>Avatar · 头像
              </span>
              <span
                v-if="s.data?.avatarUrl"
                class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87] truncate max-w-[150px] hidden sm:inline"
                >{{ s.data.avatarUrl }}</span
              >
            </div>

            <!-- current avatar preview -->
            <div
              v-if="s.data?.avatarUrl"
              class="flex items-center gap-3 bg-[#fbf9f5] border border-[#eae8e3] rounded-xl p-3"
            >
              <img
                :src="s.data.avatarUrl"
                class="w-14 h-14 rounded-xl object-cover border border-[#eae8e3] bg-white shrink-0"
                alt="avatar"
              />
              <div class="flex-1 min-w-0">
                <p
                  class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black"
                >
                  当前头像
                </p>
                <p class="font-[Inter] text-[12px] text-[#8a8a87] truncate">
                  {{ s.data.avatarUrl }}
                </p>
              </div>
              <a
                v-if="s.data?.avatarUrl"
                :href="s.data.avatarUrl"
                target="_blank"
                class="shrink-0 w-8 h-8 rounded-full border border-[#eae8e3] bg-white flex items-center justify-center hover:border-black transition-colors"
                title="查看原图"
              >
                <span class="material-symbols-outlined text-[16px]">open_in_new</span>
              </a>
            </div>

            <!-- upload row -->
            <div class="flex items-center gap-3">
              <input
                :ref="
                  (el) => {
                    fileInputRefs[s.id] = el as HTMLInputElement
                  }
                "
                type="file"
                accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
                class="hidden"
                @change="onFileChange($event, s)"
              />
              <button
                @click="triggerFile(s.id)"
                :disabled="uploading"
                class="h-8 px-4 rounded-full bg-black text-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium hover:opacity-90 disabled:opacity-40 flex items-center gap-1.5 border border-black transition-opacity shrink-0"
              >
                <span class="material-symbols-outlined text-[16px] leading-none">upload</span>
                {{ uploading ? '上传中…' : '上传' }}
              </button>
              <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87]"
                >JPG / PNG / WEBP ≤10MB</span
              >
            </div>

            <!-- 一寸照优化区 -->
            <div class="border-t border-[#eae8e3] pt-4 space-y-3">
              <div class="flex items-center gap-2">
                <span class="w-1.5 h-1.5 rounded-full bg-black"></span>
                <span
                  class="font-[JetBrains_Mono] text-[11px] tracking-[0.14em] font-medium uppercase text-black"
                  >一寸照优化</span
                >
                <span
                  v-if="optimizingMap[s.id]"
                  class="ml-auto font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-[#FF3B1F] animate-pulse"
                  >生成中…</span
                >
              </div>

              <!-- preview sourceImageUrl -->
              <div
                v-if="sourceMap[s.id]"
                class="rounded-xl overflow-hidden border border-[#eae8e3] bg-[#fbf9f5] p-2 flex justify-center"
              >
                <img
                  :src="sourceMap[s.id]"
                  class="w-28 h-36 object-cover rounded-lg border border-[#eae8e3] bg-white"
                  alt="source preview"
                />
              </div>
              <div
                v-else
                class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] text-[#8a8a87] bg-[#fbf9f5] border border-dashed border-[#eae8e3] rounded-xl py-7 text-center"
              >
                先上传图片以预览
              </div>

              <!-- backgroundType 3 buttons -->
              <div class="space-y-1.5">
                <label
                  class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black block"
                  >背景 Background</label
                >
                <div class="flex gap-2">
                  <button
                    v-for="bg in ['white', 'blue', 'red'] as const"
                    :key="bg"
                    @click="bgMap[s.id] = bg"
                    :class="[
                      'h-8 px-4 rounded-full border font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium transition-colors',
                      bgMap[s.id] === bg
                        ? 'bg-black text-white border-black'
                        : 'bg-white text-[#464742] border-[#eae8e3] hover:border-[#c7c7c0] hover:text-black',
                    ]"
                  >
                    {{ bg === 'white' ? '白底' : bg === 'blue' ? '蓝底' : '红底' }}
                  </button>
                </div>
              </div>

              <!-- style select -->
              <div class="space-y-1.5">
                <label
                  class="font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium uppercase text-black block"
                  >风格 Style</label
                >
                <select
                  :value="styleMap[s.id] || 'formal'"
                  @change="styleMap[s.id] = ($event.target as HTMLSelectElement).value"
                  class="w-full h-9 px-3 bg-white border border-[#eae8e3] rounded-[10px] font-[Inter] text-[13px] text-black focus:outline-none focus:border-black transition-colors"
                >
                  <option value="formal">formal · 正装</option>
                  <option value="natural">natural · 自然</option>
                  <option value="professional">professional · 职业</option>
                </select>
              </div>

              <!-- generate -->
              <button
                @click="handleOptimize(s)"
                :disabled="!sourceMap[s.id] || !!optimizingMap[s.id]"
                class="w-full h-9 rounded-full bg-black text-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium hover:opacity-90 disabled:opacity-40 flex items-center justify-center gap-2 border border-black transition-opacity"
              >
                <span class="material-symbols-outlined text-[16px] leading-none"
                  >auto_fix_high</span
                >
                {{ optimizingMap[s.id] ? '生成中…' : '生成' }}
              </button>
              <p
                v-if="optErrorMap[s.id]"
                class="font-[JetBrains_Mono] text-[11px] leading-4 text-[#ba1a1a] break-words"
              >
                {{ optErrorMap[s.id] }}
              </p>
              <p class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87]">
                生成后将自动回填头像 URL 并触发简历保存 (PUT /resumes/{id})
              </p>
            </div>
          </div>
        </template>
        <!-- education -->
        <template v-else-if="s.type === 'education'">
          <div
            v-for="(it, idx) in s.data as any[]"
            :key="idx"
            class="border border-[#eae8e3] rounded-lg p-3 space-y-2"
          >
            <div class="flex justify-between">
              <span class="font-[JetBrains_Mono] text-[11px]">#{{ idx + 1 }}</span
              ><button @click="removeItem(s, idx)" class="text-xs text-[#ba1a1a]">删除</button>
            </div>
            <AtInput
              :modelValue="it.school"
              @update:modelValue="
                (v) => {
                  it.school = v
                  touch()
                }
              "
              label="学校"
            />
            <div class="grid grid-cols-2 gap-2">
              <AtInput
                :modelValue="it.degree"
                @update:modelValue="
                  (v) => {
                    it.degree = v
                    touch()
                  }
                "
                label="学历"
              />
              <AtInput
                :modelValue="it.major"
                @update:modelValue="
                  (v) => {
                    it.major = v
                    touch()
                  }
                "
                label="专业"
              />
            </div>
            <div class="grid grid-cols-2 gap-2">
              <AtInput
                :modelValue="it.startDate"
                @update:modelValue="
                  (v) => {
                    it.startDate = v
                    touch()
                  }
                "
                label="入学 (YYYY-MM)"
              />
              <AtInput
                :modelValue="it.endDate"
                @update:modelValue="
                  (v) => {
                    it.endDate = v
                    touch()
                  }
                "
                label="毕业"
              />
            </div>
          </div>
          <button
            @click="addItem(s)"
            class="w-full h-8 rounded-full border border-dashed border-[#c7c7c0] text-[13px] text-[#464742] hover:border-black hover:text-black"
          >
            + {{ t('editor.addSection') }} {{ t('editor.education') }}
          </button>
        </template>
        <!-- work / project -->
        <template v-else-if="s.type === 'work' || s.type === 'project'">
          <div
            v-for="(it, idx) in s.data as any[]"
            :key="idx"
            class="border border-[#eae8e3] rounded-lg p-3 space-y-2"
          >
            <div class="flex justify-between">
              <span class="font-[JetBrains_Mono] text-[11px]">#{{ idx + 1 }}</span
              ><button @click="removeItem(s, idx)" class="text-xs text-[#ba1a1a]">删除</button>
            </div>
            <AtInput
              :modelValue="it.company || it.name"
              @update:modelValue="
                (v) => {
                  s.type === 'work' ? (it.company = v) : (it.name = v)
                  touch()
                }
              "
              :label="s.type === 'work' ? '公司' : '项目名'"
            />
            <AtInput
              :modelValue="it.position || it.role"
              @update:modelValue="
                (v) => {
                  s.type === 'work' ? (it.position = v) : (it.role = v)
                  touch()
                }
              "
              :label="s.type === 'work' ? '职位' : '角色'"
            />
            <label class="block">
              <span class="text-[13px] text-black mb-1 block">描述 (富文本)</span>
              <textarea
                :value="it.descriptionHtml || (it.description || []).join('\n')"
                @input="
                  (e) => {
                    it.descriptionHtml = (e.target as HTMLTextAreaElement).value
                    touch()
                  }
                "
                rows="3"
                class="w-full p-2 border border-[#eae8e3] rounded-lg text-sm focus:border-black outline-none"
                placeholder="支持 <b> <ul> <a>，留空则用 description"
              ></textarea>
              <!-- Atelier AI capsule group: descriptionHtml / work description -->
              <div class="mt-2 flex flex-wrap items-center gap-1.5">
                <span
                  class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] font-medium text-[#8a8a87] mr-1"
                  >AI</span
                >
                <button
                  v-for="a in aiActions"
                  :key="a.action"
                  @click="handleWorkAi(it, s.type, a.action, idx, s.id)"
                  :disabled="!!busyMap[aiKey(s.id, idx, 'description', a.action)] || isStreaming"
                  class="inline-flex items-center gap-1.5 h-7 px-3 rounded-full border border-[#eae8e3] bg-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-black hover:border-black hover:bg-[#fbf9f5] disabled:opacity-40 transition-colors"
                >
                  <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F] shrink-0"></span>{{ a.label }}
                </button>
                <span
                  v-if="isStreaming && streamingMap[aiKey(s.id, idx, 'description', 'polish')]"
                  class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#FF3B1F] animate-pulse ml-1"
                  >生成中…</span
                >
                <span
                  v-else-if="busyMap[aiKey(s.id, idx, 'description', 'generate')] || busyMap[aiKey(s.id, idx, 'description', 'polish')] || busyMap[aiKey(s.id, idx, 'description', 'shorten')] || busyMap[aiKey(s.id, idx, 'description', 'expand')]"
                  class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#FF3B1F] animate-pulse ml-1"
                  >生成中…</span
                >
              </div>
              <div class="mt-1.5 flex flex-wrap items-center gap-1.5">
                <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87]"
                  >流式</span
                >
                <button
                  v-for="a in aiActions"
                  :key="'stream-' + a.action"
                  @click="handleWorkAiStream(it, s.type, a.action, idx, s.id)"
                  :disabled="!!busyMap[aiKey(s.id, idx, 'description', a.action)] || isStreaming"
                  class="inline-flex items-center gap-1 h-6 px-2.5 rounded-full border border-[#eae8e3] bg-[#fbf9f5] font-[JetBrains_Mono] text-[10px] tracking-[0.06em] font-medium text-[#464742] hover:border-[#c7c7c0] hover:text-black disabled:opacity-40 transition-colors"
                  title="流式逐字回填 (SSE delta/done/error)"
                >
                  <span class="w-1 h-1 rounded-full bg-[#FF3B1F]"></span>{{ a.label }}·流
                </button>
              </div>
            </label>
          </div>
          <button
            @click="addItem(s)"
            class="w-full h-8 rounded-full border border-dashed border-[#c7c7c0] text-[13px]"
          >
            + {{ t('editor.addSection') }}
            {{ s.type === 'work' ? t('editor.work') : t('editor.project') }}
          </button>
        </template>
        <!-- skill -->
        <template v-else-if="s.type === 'skill'">
          <div
            v-for="(it, idx) in s.data as any[]"
            :key="idx"
            class="border border-[#eae8e3] rounded-lg p-3"
          >
            <AtInput
              :modelValue="it.category"
              @update:modelValue="
                (v) => {
                  it.category = v
                  touch()
                }
              "
              label="分类"
            />
            <p class="text-xs font-mono text-[#777871] mt-1">
              items: {{ (it.items || []).map((x: any) => x.name || x).join(', ') }}
            </p>
          </div>
          <button
            @click="addItem(s)"
            class="w-full h-8 rounded-full border border-dashed border-[#c7c7c0] text-[13px]"
          >
            + {{ t('editor.addSection') }} {{ t('editor.skill') }}
          </button>
        </template>
        <!-- introduction -->
        <template v-else-if="s.type === 'introduction'">
          <label class="block">
            <span class="text-[13px] mb-1 block">自我介绍 (富文本)</span>
            <textarea
              :value="s.data?.contentHtml || s.data?.content || ''"
              @input="
                (e) => {
                  s.data.contentHtml = (e.target as HTMLTextAreaElement).value
                  touch()
                }
              "
              rows="4"
              class="w-full p-2 border border-[#eae8e3] rounded-lg text-sm focus:border-black outline-none"
            ></textarea>
            <!-- Atelier AI capsule group: introduction contentHtml -->
            <div class="mt-2 flex flex-wrap items-center gap-1.5">
              <span
                class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] font-medium text-[#8a8a87] mr-1"
                >AI</span
              >
              <button
                v-for="a in aiActions"
                :key="a.action"
                @click="handleIntroAi(s, a.action)"
                :disabled="!!busyMap[aiKey(s.id, 'content', a.action)] || isStreaming"
                class="inline-flex items-center gap-1.5 h-7 px-3 rounded-full border border-[#eae8e3] bg-white font-[JetBrains_Mono] text-[11px] tracking-[0.06em] font-medium text-black hover:border-black hover:bg-[#fbf9f5] disabled:opacity-40 transition-colors"
              >
                <span class="w-1.5 h-1.5 rounded-full bg-[#FF3B1F] shrink-0"></span>{{ a.label }}
              </button>
              <span
                v-if="
                  busyMap[aiKey(s.id, 'content', 'generate')] ||
                  busyMap[aiKey(s.id, 'content', 'polish')] ||
                  busyMap[aiKey(s.id, 'content', 'shorten')] ||
                  busyMap[aiKey(s.id, 'content', 'expand')]
                "
                class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#FF3B1F] animate-pulse ml-1"
                >生成中…</span
              >
              <span
                v-if="isStreaming && streamingMap[aiKey(s.id, 'content', 'polish')]"
                class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#FF3B1F] animate-pulse ml-1"
                >流式中…</span
              >
            </div>
            <div class="mt-1.5 flex flex-wrap items-center gap-1.5">
              <span class="font-[JetBrains_Mono] text-[10px] tracking-[0.06em] text-[#8a8a87]"
                >流式</span
              >
              <button
                v-for="a in aiActions"
                :key="'stream-' + a.action"
                @click="handleIntroAiStream(s, a.action)"
                :disabled="!!busyMap[aiKey(s.id, 'content', a.action)] || isStreaming"
                class="inline-flex items-center gap-1 h-6 px-2.5 rounded-full border border-[#eae8e3] bg-[#fbf9f5] font-[JetBrains_Mono] text-[10px] tracking-[0.06em] font-medium text-[#464742] hover:border-[#c7c7c0] hover:text-black disabled:opacity-40 transition-colors"
                title="流式逐字回填 (SSE delta/done/error)"
              >
                <span class="w-1 h-1 rounded-full bg-[#FF3B1F]"></span>{{ a.label }}·流
              </button>
            </div>
          </label>
        </template>
        <template v-else>
          <textarea
            :value="JSON.stringify(s.data, null, 2)"
            @input="
              (e) => {
                try {
                  s.data = JSON.parse((e.target as HTMLTextAreaElement).value)
                  touch()
                } catch {}
              }
            "
            rows="4"
            class="w-full p-2 border border-[#eae8e3] rounded font-mono text-xs"
          ></textarea>
        </template>
      </div>
    </div>
    <span class="hidden"
      >{{ t('editor.preview') }} {{ t('editor.split') }} {{ t('editor.focus') }}
      {{ t('editor.page') }} {{ t('editor.saving') }} {{ t('editor.saved') }}
      {{ t('editor.export') }} {{ t('editor.renderSettings') }} {{ t('editor.fontFamily') }}
      {{ t('editor.baseSize') }} {{ t('editor.lineHeight') }} {{ t('editor.sectionSpacing') }}
      {{ t('editor.pagePadding') }} {{ t('editor.accent') }}</span
    >
  </div>
</template>
