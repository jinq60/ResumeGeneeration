import { describe, expect, it } from 'vitest'
import {
  plainTextToRichHtml,
  richTextToPlainText,
  sanitizeRichText
} from '@/utils/richText'

describe('richText utilities', () => {
  it('keeps supported formatting and removes unsafe markup', () => {
    const result = sanitizeRichText(
      '<p><strong>重点</strong> <span onclick="alert(1)">内容</span></p><script>alert(2)</script><a href="javascript:alert(3)">危险链接</a><a href="https://example.com">安全链接</a>'
    )

    expect(result).toContain('<strong>重点</strong>')
    expect(result).toContain('内容')
    expect(result).not.toContain('onclick')
    expect(result).not.toContain('<script')
    expect(result).not.toContain('javascript:')
    expect(result).toContain('https://example.com')
  })

  it('converts plain text into editable paragraphs', () => {
    expect(plainTextToRichHtml('第一行\n第二行')).toBe('<p>第一行</p><p>第二行</p>')
  })

  it('extracts readable text from rich content', () => {
    expect(richTextToPlainText('<p>第一段</p><ul><li>第二段</li><li>第三段</li></ul>'))
      .toBe('第一段\n第二段\n第三段')
  })
})
