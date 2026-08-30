import type { PdfTask } from '@/api/pdf'
import type { AvatarTask } from '@/api/avatar'

const PDF_KEY = 'resume_pdf_tasks'
const AVATAR_KEY = 'resume_avatar_tasks'

export function getPdfTasks(): PdfTask[] {
  const raw = localStorage.getItem(PDF_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as PdfTask[]
  } catch {
    return []
  }
}

export function savePdfTasks(tasks: PdfTask[]) {
  try {
    localStorage.setItem(PDF_KEY, JSON.stringify(tasks.slice(0, 50)))
  } catch (e) {
    if (e instanceof DOMException && e.name === 'QuotaExceededError') {
      localStorage.setItem(PDF_KEY, JSON.stringify(tasks.slice(0, 20)))
    }
  }
}

export function addPdfTask(task: PdfTask) {
  const tasks = getPdfTasks()
  // 去重：同 taskId 不重复插入
  const existing = tasks.findIndex(t => t.taskId === task.taskId)
  if (existing >= 0) tasks.splice(existing, 1)
  tasks.unshift(task)
  savePdfTasks(tasks)
}

export function updatePdfTask(taskId: string, updater: Partial<PdfTask>) {
  const tasks = getPdfTasks()
  const idx = tasks.findIndex(t => t.taskId === taskId)
  if (idx >= 0) {
    tasks[idx] = { ...tasks[idx], ...updater }
    savePdfTasks(tasks)
  }
}

export function removePdfTask(taskId: string) {
  const tasks = getPdfTasks().filter(t => t.taskId !== taskId)
  savePdfTasks(tasks)
}

export function getAvatarTasks(): AvatarTask[] {
  const raw = localStorage.getItem(AVATAR_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as AvatarTask[]
  } catch {
    return []
  }
}

export function saveAvatarTasks(tasks: AvatarTask[]) {
  try {
    localStorage.setItem(AVATAR_KEY, JSON.stringify(tasks.slice(0, 50)))
  } catch (e) {
    if (e instanceof DOMException && e.name === 'QuotaExceededError') {
      localStorage.setItem(AVATAR_KEY, JSON.stringify(tasks.slice(0, 20)))
    }
  }
}

export function addAvatarTask(task: AvatarTask) {
  const tasks = getAvatarTasks()
  const existing = tasks.findIndex(t => t.taskId === task.taskId)
  if (existing >= 0) tasks.splice(existing, 1)
  tasks.unshift(task)
  saveAvatarTasks(tasks)
}

export function updateAvatarTask(taskId: string, updater: Partial<AvatarTask>) {
  const tasks = getAvatarTasks()
  const idx = tasks.findIndex(t => t.taskId === taskId)
  if (idx >= 0) {
    tasks[idx] = { ...tasks[idx], ...updater }
    saveAvatarTasks(tasks)
  }
}

export function removeAvatarTask(taskId: string) {
  const tasks = getAvatarTasks().filter(t => t.taskId !== taskId)
  saveAvatarTasks(tasks)
}
