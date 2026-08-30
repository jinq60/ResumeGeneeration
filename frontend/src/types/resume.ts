import type { RenderSettings } from '@/utils/renderSettings'

export type SectionType = 'profile' | 'education' | 'work' | 'project' | 'skill' | 'introduction' | 'custom'

export type Scene = 'campus_recruitment' | 'internship' | 'social_recruitment' | 'postgraduate_reexam' | 'project_application' | 'custom'

export const GENDERS = ['male', 'female', 'other'] as const
export type Gender = (typeof GENDERS)[number]

export const WORK_TYPES = ['full_time', 'internship', 'part_time', 'campus_job', 'research_assistant', 'volunteer'] as const
export type WorkType = (typeof WORK_TYPES)[number]

export const PROJECT_TYPES = ['research', 'course', 'enterprise', 'competition', 'open_source', 'personal', 'other'] as const
export type ProjectType = (typeof PROJECT_TYPES)[number]

export const SKILL_CATEGORIES = ['programming_language', 'frontend', 'backend', 'database', 'ai_data', 'design', 'office', 'language', 'tool', 'other'] as const
export type SkillCategory = (typeof SKILL_CATEGORIES)[number]

export const SKILL_LEVELS = ['beginner', 'familiar', 'proficient', 'expert'] as const
export type SkillLevel = (typeof SKILL_LEVELS)[number]

export const INTRODUCTION_STYLES = ['concise_formal', 'tech_oriented', 'student', 'senior', 'postgraduate', 'project'] as const
export type IntroductionStyle = (typeof INTRODUCTION_STYLES)[number]

export interface BaseSection<T extends SectionType, D> {
  id: string; type: T; title: string; order: number; visible: boolean; data: D
}

export interface EducationItem {
  id?: string
  school: string; degree: string; major: string; college?: string
  startDate: string; endDate?: string; gpa?: string; rank?: string
  honors?: string[]; courses?: string[]
  honorsText?: string; coursesText?: string
}

export interface WorkItem {
  id?: string
  company: string; position: string; department?: string; type?: string; city?: string
  startDate: string; endDate?: string; description: string[]; achievements?: string[]
  descriptionHtml?: string; achievementsHtml?: string
  techStack?: string[]; leaveReason?: string; showLeaveReason?: boolean
  techStackText?: string
}

export interface ProjectItem {
  id?: string
  name: string; role?: string; type?: string; startDate?: string; endDate?: string
  techStack?: string[]; background?: string; responsibility?: string
  achievements?: string[]; description: string[]; link?: string; github?: string
  descriptionHtml?: string; achievementsHtml?: string
  techStackText?: string; descriptionText?: string; achievementsText?: string
}

export interface SkillItem {
  id?: string
  category: string
  items: Array<{ name: string; level?: string; highlight?: boolean }>
}

export type ProfileSection = BaseSection<'profile', Profile>
export type EducationSection = BaseSection<'education', EducationItem[]>
export type WorkSection = BaseSection<'work', WorkItem[]>
export type ProjectSection = BaseSection<'project', ProjectItem[]>
export type SkillSection = BaseSection<'skill', SkillItem[]>
export type IntroductionSection = BaseSection<'introduction', { content: string; contentHtml?: string; keywords?: string[]; style?: string; maxWords?: number }>
export type CustomSection = BaseSection<'custom', { content: string }>

export type Section = ProfileSection | EducationSection | WorkSection | ProjectSection | SkillSection | IntroductionSection | CustomSection

export interface Resume {
  id: string; userId: string; title: string; scene: Scene; targetPosition?: string
  targetIndustry?: string
  templateId: string; sections: Section[]; renderSettings?: RenderSettings | null
  createdAt: string; updatedAt: string
  exportCount?: number; lastEditedAt?: string
  /**
   * 乐观锁版本号（api-changelog v2.2 / V13 迁移）。
   * 更新/重命名请求需回传；并发编辑冲突返回 RESUME_VERSION_CONFLICT(2012)。
   */
  version?: number
}

export interface Profile {
  name: string; gender?: string; birthDate?: string; phone?: string; email?: string
  city?: string; targetPosition?: string; expectedSalary?: string; availability?: string
  personalWebsite?: string; github?: string; portfolio?: string; avatarUrl?: string
  customFields?: Array<{ id: string; label: string; value: string; showLabel?: boolean }>
  showGender?: boolean; showAge?: boolean; showSalary?: boolean; showAvatar?: boolean
}
