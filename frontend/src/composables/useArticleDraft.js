import { onUnmounted, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { autoSaveDraft } from '@/api/blog/article'

const DRAFT_PREFIX = 'nova-mall-draft:'

function draftKey(id) {
  return `${DRAFT_PREFIX}${id ?? 'new'}`
}

export function loadLocalDraft(id) {
  const raw = localStorage.getItem(draftKey(id))
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function saveLocalDraft(form) {
  localStorage.setItem(draftKey(form.id), JSON.stringify({ ...form, savedAt: Date.now() }))
}

export function clearLocalDraft(id) {
  localStorage.removeItem(draftKey(id))
  if (!id) {
    localStorage.removeItem(draftKey(undefined))
  }
}

export function useArticleDraft(form, options) {
  const saving = ref(false)
  const lastSavedAt = ref(null)
  const saveError = ref('')

  const persistLocal = useDebounceFn(() => {
    if (options.enabled.value) {
      saveLocalDraft(form.value)
    }
  }, 1500)

  const persistRemote = useDebounceFn(async () => {
    if (!options.enabled.value) return
    if (!form.value.title.trim() && !form.value.content.trim()) return
    saving.value = true
    saveError.value = ''
    try {
      const payload = {
        id: form.value.id,
        title: form.value.title || '无标题草稿',
        summary: form.value.summary,
        content: form.value.content || ' ',
        coverImage: form.value.coverImage,
        categoryId: form.value.categoryId,
        status: form.value.status === 1 ? 1 : 0,
        tagIds: form.value.tagIds,
        tagNames: form.value.tagNames,
      }
      const res = await autoSaveDraft(payload)
      if (res.data != null && res.data !== '') {
        form.value.id = res.data
      }
      lastSavedAt.value = new Date()
      saveLocalDraft(form.value)
    } catch {
      saveError.value = '自动保存失败'
    } finally {
      saving.value = false
    }
  }, 30000)

  const stopWatch = watch(
    form,
    () => {
      persistLocal()
      persistRemote()
    },
    { deep: true },
  )

  onUnmounted(() => {
    stopWatch()
  })

  return { saving, lastSavedAt, saveError }
}
