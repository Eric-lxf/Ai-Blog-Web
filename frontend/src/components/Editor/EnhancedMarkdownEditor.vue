<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import AiSelectionToolbar from '@/components/editor/AiSelectionToolbar.vue'
import AiSlashMenu from '@/components/editor/AiSlashMenu.vue'
import { useAiTransform } from '@/composables/useAiTransform'
import {
  getCursorOffset,
  getCursorRect,
  getSelectionRangeInContainer,
  isSlashTrigger,
} from '@/utils/editorSelection'

const props = defineProps({
  modelValue: { type: String, default: '' },
  articleTitle: { type: String, default: '' },
  aiConfigured: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const wrapperRef = ref(null)
const { loading: aiLoading, run: runAi, stop: stopAi } = useAiTransform()

const toolbarVisible = ref(false)
const toolbarPos = ref({ top: 0, left: 0 })
const slashVisible = ref(false)
const slashPos = ref({ top: 0, left: 0 })

let selectionRange = null
let slashCursor = 0

function getEditorContentEl() {
  const root = wrapperRef.value
  if (!root) return null
  return (
    root.querySelector('.cm-content') ||
    root.querySelector('.CodeMirror-line')?.parentElement ||
    root.querySelector('textarea')
  )
}

function updateContent(next) {
  emit('update:modelValue', next)
}

function hideToolbar() {
  toolbarVisible.value = false
  selectionRange = null
}

function hideSlash() {
  slashVisible.value = false
}

function onMouseUp() {
  if (!props.aiConfigured || aiLoading.value) return
  nextTick(() => {
    const container = getEditorContentEl()
    if (!container) return
    const range = getSelectionRangeInContainer(container)
    if (!range || range.text.trim().length < 2) {
      hideToolbar()
      return
    }
    selectionRange = range
    const sel = window.getSelection()
    if (sel && sel.rangeCount > 0) {
      const rect = sel.getRangeAt(0).getBoundingClientRect()
      toolbarPos.value = {
        top: rect.top,
        left: rect.left + rect.width / 2,
      }
      toolbarVisible.value = true
    }
  })
}

function onKeyUp(e) {
  if (!props.aiConfigured || aiLoading.value) return
  if (e.key === 'Escape') {
    hideSlash()
    hideToolbar()
    return
  }
  const container = getEditorContentEl()
  if (!container) return
  const cursor = getCursorOffset(container)
  if (cursor == null) return

  if (isSlashTrigger(props.modelValue, cursor)) {
    slashCursor = cursor
    const rect = getCursorRect()
    if (rect) {
      slashPos.value = { top: rect.bottom, left: rect.left }
      slashVisible.value = true
      hideToolbar()
    }
  } else if (slashVisible.value && e.key !== '/') {
    if (e.key === ' ' && props.modelValue[cursor - 1] === '/') {
      hideSlash()
    }
  }
}

function onDocumentClick(e) {
  const target = e.target
  if (wrapperRef.value && !wrapperRef.value.contains(target)) {
    const toolbar = document.querySelector('.selection-toolbar')
    const slash = document.querySelector('.slash-menu')
    if (toolbar?.contains(target) || slash?.contains(target)) return
    hideToolbar()
    hideSlash()
  }
}

async function applyAiReplace(scene, prompt, range) {
  hideToolbar()
  hideSlash()
  stopAi()

  const original = props.modelValue
  const originalSlice = original.slice(range.from, range.to)

  const rebuild = text =>
    original.slice(0, range.from) + text + original.slice(range.to)

  updateContent(rebuild('✨ AI 生成中...'))

  try {
    const result = await runAi(
      {
        scene,
        prompt,
        includeContext: true,
        articleTitle: props.articleTitle,
        articleContent: original,
      },
      chunk => {
        updateContent(rebuild(chunk || ' '))
      },
    )
    updateContent(rebuild(result || originalSlice))
    ElMessage.success('AI 已应用')
  } catch (e) {
    updateContent(rebuild(originalSlice))
    ElMessage.error(e instanceof Error ? e.message : 'AI 处理失败')
  }
}

async function onSelectionAction(action) {
  if (!selectionRange) return
  const prompt = action.buildPrompt(selectionRange.text)
  await applyAiReplace(action.scene, prompt, {
    from: selectionRange.from,
    to: selectionRange.to,
  })
}

async function onSlashSelect(cmd, input) {
  const container = getEditorContentEl()
  if (!container) return

  const lineStart = props.modelValue.lastIndexOf('\n', slashCursor - 2) + 1
  const lineEnd = props.modelValue.indexOf('\n', slashCursor)
  const lineEndPos = lineEnd === -1 ? props.modelValue.length : lineEnd
  const linePrefix = props.modelValue.slice(lineStart, slashCursor - 1).trim()

  const prompt = cmd.buildPrompt(input, linePrefix)
  const range = { from: lineStart, to: lineEndPos }

  await applyAiReplace(cmd.scene, prompt, range)
}

onMounted(() => {
  const root = wrapperRef.value
  if (!root) return
  root.addEventListener('mouseup', onMouseUp)
  root.addEventListener('keyup', onKeyUp)
  document.addEventListener('mousedown', onDocumentClick)
})

onUnmounted(() => {
  const root = wrapperRef.value
  if (root) {
    root.removeEventListener('mouseup', onMouseUp)
    root.removeEventListener('keyup', onKeyUp)
  }
  document.removeEventListener('mousedown', onDocumentClick)
  stopAi()
})

watch(
  () => props.aiConfigured,
  v => {
    if (!v) {
      hideToolbar()
      hideSlash()
    }
  },
)
</script>

<template>
  <div ref="wrapperRef" class="enhanced-editor">
    <MarkdownEditor :model-value="modelValue" @update:model-value="updateContent" />
    <p v-if="!aiConfigured" class="ai-tip">
      配置 <code>DEEPSEEK_API_KEY</code> 后可使用划词润色与 <code>/</code> 指令
    </p>
    <p v-else class="ai-tip">
      选中文字可润色/扩写；行首输入 <code>/</code> 唤起 AI 指令菜单
    </p>

    <AiSelectionToolbar
      :visible="toolbarVisible && aiConfigured"
      :top="toolbarPos.top"
      :left="toolbarPos.left"
      :loading="aiLoading"
      @action="onSelectionAction"
    />
    <AiSlashMenu
      :visible="slashVisible && aiConfigured"
      :top="slashPos.top"
      :left="slashPos.left"
      @select="onSlashSelect"
      @close="hideSlash"
    />
  </div>
</template>

<style scoped>
.enhanced-editor {
  position: relative;
}

.ai-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #9ca3af;
}

.ai-tip code {
  background: #f3f4f6;
  padding: 1px 4px;
  border-radius: 4px;
}
</style>
