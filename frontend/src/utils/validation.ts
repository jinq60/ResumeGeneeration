export function validatePhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}

export function validateEmail(email: string): boolean {
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)
}

export function validateUrl(url: string): boolean {
  return /^https?:\/\/([\w-]+\.)+[\w-]+(\/[\w-./?%&=]*)?$/.test(url)
}

export function validateDate(date: string): boolean {
  return /^(19|20)\d{2}-(0[1-9]|1[0-2])$/.test(date)
}

export function validateAvatarFile(file: File): { valid: boolean; message?: string } {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  const maxSize = 10 * 1024 * 1024

  if (!allowedTypes.includes(file.type)) {
    return { valid: false, message: '请上传 JPG、PNG 或 WEBP 格式图片。' }
  }

  if (file.size > maxSize) {
    return { valid: false, message: '图片大小不能超过 10MB。' }
  }

  return { valid: true }
}
