import request, { rawAxios } from '@/utils/request'

export interface ShareInfo {
  token: string
  url: string
  status: string
  hideContact?: boolean
  expiresAt?: string
  createdAt?: string
}

export interface CreateSharePayload {
  hideContact?: boolean
  expiresAt?: string
}

export const shareApi = {
  create(resumeId: string, payload?: CreateSharePayload): Promise<ShareInfo> {
    return request.post(`/resumes/${resumeId}/share`, payload || {}) as Promise<ShareInfo>
  },
  get(resumeId: string): Promise<ShareInfo | null> {
    return request.get(`/resumes/${resumeId}/share`) as Promise<ShareInfo | null>
  },
  revoke(resumeId: string): Promise<void> {
    return request.delete(`/resumes/${resumeId}/share`) as Promise<void>
  },
  /**
   * 匿名获取分享只读 HTML（api-spec §7.12）。返回原始字符串，调用方负责渲染。
   * 走独立 axios 实例以绕过 R<T> 解包 + JSON 拦截器。
   */
  fetchSharePage(token: string): Promise<string> {
    return rawAxios
      .get(`/share/${token}`, { responseType: 'text', transformResponse: [(d) => d] })
      .then((res) => (typeof res.data === 'string' ? res.data : String(res.data ?? '')))
  }
}
