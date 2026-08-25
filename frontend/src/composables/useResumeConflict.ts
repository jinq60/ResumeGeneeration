import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 判断保存错误是否为乐观锁版本冲突：
 * 后端业务码 2012（RESUME_VERSION_CONFLICT），HTTP 状态 409。
 */
export function isVersionConflictError(error: unknown): boolean {
  const e = error as { code?: number | string; httpStatus?: number; status?: number } | null
  if (!e) return false
  return e.code === 2012 || e.code === '2012' || e.httpStatus === 409 || e.status === 409
}

interface ResolveConflictOptions {
  /** 冲突确认前备份当前本地草稿（写入 localStorage 等），以便用户之后恢复 */
  backupDraft: () => void
  /** 重新拉取服务器最新版本并覆盖本地状态 */
  fetchLatest: () => Promise<void>
}

/**
 * 简历乐观锁冲突处理：弹窗询问用户是否加载服务器最新版本，
 * 确认则先备份本地草稿再重新拉取；取消则保持现状。
 */
export function useResumeConflict() {
  const resolving = ref(false)

  async function resolveConflict(options: ResolveConflictOptions): Promise<boolean> {
    if (resolving.value) return false
    resolving.value = true
    try {
      try {
        await ElMessageBox.confirm(
          '简历已在其他窗口被修改，是否加载服务器最新版本？',
          '简历版本冲突',
          {
            confirmButtonText: '加载最新版本',
            cancelButtonText: '保留本地修改',
            type: 'warning'
          }
        )
      } catch {
        // 用户取消：保持现状
        return false
      }
      options.backupDraft()
      await options.fetchLatest()
      ElMessage.success('已加载服务器最新版本；你的本地草稿已备份，刷新页面时可选择恢复')
      return true
    } catch (e: any) {
      ElMessage.error(e?.message || '获取服务器最新版本失败')
      return false
    } finally {
      resolving.value = false
    }
  }

  return { resolving, resolveConflict }
}
