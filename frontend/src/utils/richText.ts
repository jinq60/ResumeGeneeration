const ALLOWED_TAGS = new Set(['P', 'BR', 'STRONG', 'B', 'EM', 'I', 'U', 'UL', 'OL', 'LI', 'A'])
const DROP_TAGS = new Set(['SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED', 'SVG', 'MATH', 'TEMPLATE'])

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * Keep only the small formatting vocabulary supported by resume templates.
 * This is a client-side guard; the backend repeats the same allowlist.
 */
export function sanitizeRichText(html: string) {
  if (!html || typeof document === 'undefined') return ''

  const template = document.createElement('template')
  template.innerHTML = html

  const visit = (parent: Node) => {
    for (const child of Array.from(parent.childNodes)) {
      if (child.nodeType === Node.COMMENT_NODE) {
        parent.removeChild(child)
        continue
      }
      if (!(child instanceof HTMLElement)) continue

      const tagName = child.tagName.toUpperCase()
      if (DROP_TAGS.has(tagName)) {
        parent.removeChild(child)
        continue
      }
      if (!ALLOWED_TAGS.has(tagName)) {
        visit(child)
        while (child.firstChild) {
          parent.insertBefore(child.firstChild, child)
        }
        parent.removeChild(child)
        continue
      }

      for (const attribute of Array.from(child.attributes)) {
        if (tagName === 'A' && attribute.name === 'href') {
          const href = attribute.value.trim()
          try {
            const url = new URL(href, window.location.origin)
            if (url.protocol === 'http:' || url.protocol === 'https:') {
              child.setAttribute('href', url.href)
              child.setAttribute('rel', 'noopener noreferrer')
              child.setAttribute('target', '_blank')
              continue
            }
          } catch {
            // invalid URL, drop
          }
        }
        child.removeAttribute(attribute.name)
      }
      visit(child)
    }
  }

  visit(template.content)
  return template.innerHTML
}

export function plainTextToRichHtml(text: string) {
  if (!text) return ''
  return text
    .split(/\r?\n/)
    .map((line) => `<p>${line ? escapeHtml(line) : '<br>'}</p>`)
    .join('')
}

export function richTextToPlainText(html: string) {
  if (!html || typeof document === 'undefined') return ''

  const container = document.createElement('div')
  container.innerHTML = sanitizeRichText(html)
  container.querySelectorAll('br').forEach((node) => node.replaceWith('\n'))
  container.querySelectorAll('p, li').forEach((node) => node.append('\n'))

  return (container.textContent || '')
    .replace(/\u00a0/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}
