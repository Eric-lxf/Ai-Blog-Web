export function getSelectionRangeInContainer(container) {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0 || sel.isCollapsed) {
    return null
  }
  const range = sel.getRangeAt(0)
  if (!container.contains(range.commonAncestorContainer)) {
    return null
  }

  const text = sel.toString()
  if (!text.trim()) {
    return null
  }

  const pre = document.createRange()
  pre.selectNodeContents(container)
  pre.setEnd(range.startContainer, range.startOffset)
  const from = pre.toString().length
  const to = from + text.length

  return { from, to, text }
}

export function replaceRange(content, from, to, replacement) {
  return content.slice(0, from) + replacement + content.slice(to)
}

export function getCursorOffset(container) {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) {
    return null
  }
  const range = sel.getRangeAt(0)
  if (!container.contains(range.commonAncestorContainer)) {
    return null
  }
  const pre = document.createRange()
  pre.selectNodeContents(container)
  pre.setEnd(range.startContainer, range.startOffset)
  return pre.toString().length
}

export function getCursorRect() {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) {
    return null
  }
  const range = sel.getRangeAt(0)
  const rect = range.getBoundingClientRect()
  if (rect.width === 0 && rect.height === 0) {
    const span = document.createElement('span')
    span.textContent = '\u200b'
    range.insertNode(span)
    const r = span.getBoundingClientRect()
    span.remove()
    return r
  }
  return rect
}

export function isSlashTrigger(content, cursor) {
  if (cursor <= 0 || content[cursor - 1] !== '/') {
    return false
  }
  const lineStart = content.lastIndexOf('\n', cursor - 2) + 1
  const before = content.slice(lineStart, cursor - 1)
  return before.trim() === ''
}
