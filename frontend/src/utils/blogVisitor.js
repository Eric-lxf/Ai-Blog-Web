const STORAGE_KEY = 'blog_visitor_id'

function randomId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `v_${Date.now()}_${Math.random().toString(36).slice(2, 12)}`
}

export function getVisitorId() {
  try {
    let id = localStorage.getItem(STORAGE_KEY)
    if (!id) {
      id = randomId()
      localStorage.setItem(STORAGE_KEY, id)
    }
    return id
  } catch {
    return randomId()
  }
}
