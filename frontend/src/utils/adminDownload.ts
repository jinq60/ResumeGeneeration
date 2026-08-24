import axios from 'axios'

/**
 * 管理端文件下载：使用 admin_token 直接请求并保存为本地文件。
 * 适用于 CSV / JSON / Word / Markdown 等二进制或文本流下载。
 */
export async function adminDownload(url: string, fallbackName: string): Promise<string> {
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
  const token = localStorage.getItem('admin_token')
  const response = await axios.get(`${base}${url}`, {
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })

  const disposition = response.headers?.['content-disposition'] || null
  const match = disposition?.match(/filename\*=UTF-8''([^;]+)/)
  let fileName = fallbackName
  if (match) {
    try {
      fileName = decodeURIComponent(match[1])
    } catch {
      fileName = match[1]
    }
  }

  const blobUrl = window.URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(blobUrl)
  return fileName
}