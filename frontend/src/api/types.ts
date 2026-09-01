// 直译 docs/API.md §1.5 由 backend dto + R 生成

export interface ApiResp<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface Page<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// Auth
export interface AuthResponse {
  userId: string
  accessToken: string
  refreshToken: string
  expiresIn: number
  isGuest: boolean
}
export interface LoginRequest {
  account: string
  password: string
  loginType?: 'phone' | 'email'
}

// User
export interface UserInfoResponse {
  userId: string
  nickname?: string
  phone?: string
  email?: string
  avatarUrl?: string
  isGuest: boolean
}

// Resume
export interface SectionDTO {
  id: string
  type: 'profile' | 'education' | 'project' | 'work' | 'skill' | 'introduction' | 'custom'
  title: string
  order: number
  visible: boolean
  data: any
}
export interface RenderSettings {
  fontFamily?: string
  baseFontSize?: number
  lineHeight?: number
  pagePadding?: number
  sectionSpacing?: number
  accentColor?: string
  autoOnePage?: boolean
}
export interface ResumeDetailResponse {
  id: string
  userId: string
  title: string
  scene: string
  targetPosition?: string
  targetIndustry?: string
  templateId: string
  sections: SectionDTO[]
  renderSettings?: RenderSettings
  exportCount: number
  version: number
  lastEditedAt: string
  createdAt: string
  updatedAt: string
}
export interface CreateResumeRequest {
  title?: string
  scene: string
  targetPosition?: string
  targetIndustry?: string
  templateId: string
}
export type UpdateResumeRequest = Partial<CreateResumeRequest> & {
  sections?: SectionDTO[]
  renderSettings?: RenderSettings
  version?: number
}
export interface ResumeListItemResponse {
  id: string
  title: string
  scene: string
  targetPosition?: string
  templateId: string
  lastEditedAt: string
  createdAt: string
  updatedAt: string
}

// Template
export interface TemplateDTO {
  id: string
  code: string
  name: string
  category: string
  thumbnailUrl?: string
  description?: string
  config: any
  htmlTemplate: string
  renderEngine: string
  sortOrder: number
  isRecommended: boolean
  isBuiltin?: boolean
  status: string
  createdAt: string
  updatedAt: string
  version?: number
}

// AI
export interface ResumeAiWriteRequest {
  sectionType: string
  field: string
  action: 'generate' | 'polish' | 'shorten' | 'expand' | 'translate'
  originalText: string
  targetLang?: string
}
export interface ResumeAiWriteResponse {
  content: string
}
export interface GrammarCheckResponse {
  status: string
  model: string
  message?: string
  checkedAt: string
  issues: Array<{
    sectionType: string
    field: string
    itemIndex?: number
    severity: string
    originalText: string
    suggestion: string
    explanation: string
  }>
}

// Pdf
export interface PdfTaskResponse {
  taskId: string
  resumeId: string
  templateId: string
  status: 'pending' | 'processing' | 'success' | 'failed'
  fileName?: string
  fileSize?: number
  errorMsg?: string
  createdAt: string
  completedAt?: string
}

// Share
export interface ShareResponse {
  token: string
  url: string
  status: string
  hideContact: boolean
  expiresAt?: string
  createdAt: string
}

// Delivery
export interface DeliveryRecordResponse {
  id: string
  resumeId: string
  userId?: string
  company: string
  position: string
  channel?: string
  status: string
  applyDate?: string
  jdContent?: string
  note?: string
  interviewTime?: string
  interviewLocation?: string
  createdAt: string
  updatedAt?: string
}

// Notification — 兼容后端 boolean read 与旧 number readFlag
export interface NotificationResponse {
  id: string
  type: string
  title: string
  content: string
  read: boolean
  readFlag?: number
  createdAt: string
}
