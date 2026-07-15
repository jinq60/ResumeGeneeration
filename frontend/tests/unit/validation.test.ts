import { describe, it, expect } from 'vitest'
import { validatePhone, validateEmail, validateUrl } from '@/utils/validation'

describe('validation utils', () => {
  it('validates phone number', () => {
    expect(validatePhone('13800000000')).toBe(true)
    expect(validatePhone('1380000000')).toBe(false)
  })

  it('validates email', () => {
    expect(validateEmail('test@example.com')).toBe(true)
    expect(validateEmail('invalid')).toBe(false)
  })

  it('validates url', () => {
    expect(validateUrl('https://example.com')).toBe(true)
    expect(validateUrl('ftp://example.com')).toBe(false)
  })
})
