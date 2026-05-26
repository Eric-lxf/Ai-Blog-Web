<script setup>
import { computed, onMounted, ref } from 'vue'
import { ChatDotRound, Delete, Position, VideoPause } from '@element-plus/icons-vue'
import SimpleMarkdownViewer from '@/components/SimpleMarkdownViewer.vue'
import { fetchAiStatus, fetchAiTemplates } from '@/api/blog/ai'
import { useAiChat } from '@/composables/useAiChat'

const props = defineProps({
  articleTitle: { type: String, default: '' },
  articleContent: { type: String, default: '' },
})

const emit = defineEmits(['insert'])

const visible = defineModel('visible', { default: true })

const input = ref('')
const includeContext = ref(true)
const scene = ref('CHAT')
const templates = ref([])
const aiConfigured = ref(false)
const aiModel = ref('')

const { messages, loading, send, stop, clear } = useAiChat()

const quickPrompts = [
  '根据当前文章，帮我写一段 100 字以内的摘要',
  '为这篇文章想 3 个吸引人的标题',
  '用 Mermaid 画一张与文章主题相关的架构图（输出代码块）',
  '检查正文有没有逻辑问题或遗漏的技术点',
]

async function loadMeta() {
  try {
    const [statusRes, tplRes] = await Promise.all([fetchAiStatus(), fetchAiTemplates()])
    aiConfigured.value = statusRes?.data?.configured ?? false
    aiModel.value = statusRes?.data?.model ?? ''
    const list = tplRes?.data
    templates.value = Array.isArray(list) ? list : []
    if (templates.value.length && !templates.value.find(t => t.sceneType === scene.value)) {
      scene.value = templates.value[0].sceneType
    }
  } catch {
    aiConfigured.value = false
  }
}

async function handleSend(text) {
  const prompt = (text ?? input.value).trim()
  if (!prompt) return
  input.value = ''
  await send({
    prompt,
    scene: scene.value,
    includeContext: includeContext.value,
    articleTitle: props.articleTitle,
    articleContent: props.articleContent,
  })
}

function handleInsert(content) {
  if (!content.trim()) return
  emit('insert', content)
}

const statusTag = computed(() => {
  if (!aiConfigured.value) return { type: 'warning', text: '未配置 API Key' }
  if (loading.value) return { type: 'primary', text: '生成中...' }
  return { type: 'success', text: aiModel.value || 'DeepSeek 就绪' }
})

onMounted(loadMeta)
</script>

<template>
  <aside v-show="visible" class="ai-sidebar">
    <div class="sidebar-header">
      <div class="title">
        <el-icon><ChatDotRound /></el-icon>
        <span>AI 写作助手</span>
      </div>
      <el-tag :type="statusTag.type" size="small" effect="plain">{{ statusTag.text }}</el-tag>
    </div>

    <div class="sidebar-toolbar">
      <el-select v-model="scene" size="small" style="width: 130px" :disabled="loading">
        <el-option
          v-for="tpl in templates"
          :key="tpl.sceneType"
          :label="tpl.templateName"
          :value="tpl.sceneType"
        />
      </el-select>
      <el-checkbox v-model="includeContext" size="small" :disabled="loading">
        附带文章上下文
      </el-checkbox>
      <el-button size="small" :icon="Delete" text @click="clear">清空</el-button>
    </div>

    <div class="quick-prompts">
      <el-button
        v-for="q in quickPrompts"
        :key="q"
        size="small"
        round
        :disabled="loading || !aiConfigured"
        @click="handleSend(q)"
      >
        {{ q.length > 18 ? q.slice(0, 18) + '…' : q }}
      </el-button>
    </div>

    <div ref="messageListRef" class="message-list">
      <el-empty v-if="messages.length === 0" description="问我任何写作问题，或点击上方快捷指令" />

      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message"
        :class="msg.role"
      >
        <div class="bubble">
          <SimpleMarkdownViewer v-if="msg.role === 'assistant'" :value="msg.content || ' '" />
          <p v-else class="user-text">{{ msg.content }}</p>
          <span v-if="msg.streaming" class="cursor">▍</span>
        </div>
        <div v-if="msg.role === 'assistant' && msg.content && !msg.streaming" class="actions">
          <el-button size="small" type="primary" link @click="handleInsert(msg.content)">
            插入到正文
          </el-button>
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        placeholder="输入问题，Ctrl+Enter 发送"
        :disabled="loading || !aiConfigured"
        @keydown.ctrl.enter.prevent="handleSend()"
      />
      <div class="input-actions">
        <el-button
          v-if="loading"
          type="danger"
          :icon="VideoPause"
          @click="stop"
        >
          停止
        </el-button>
        <el-button
          v-else
          type="primary"
          :icon="Position"
          :disabled="!input.trim() || !aiConfigured"
          @click="handleSend()"
        >
          发送
        </el-button>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.ai-sidebar {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  border-radius: 0 8px 8px 0;
  max-height: calc(100vh - 100px);
  position: sticky;
  top: 12px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #f3f4f6;
}

.title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 15px;
}

.sidebar-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid #f3f4f6;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 14px;
  border-bottom: 1px solid #f3f4f6;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  min-height: 200px;
}

.message {
  margin-bottom: 14px;
}

.message.user .bubble {
  background: #ecfdf5;
  margin-left: 24px;
}

.message.assistant .bubble {
  background: #f9fafb;
  margin-right: 8px;
}

.bubble {
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
}

.bubble :deep(.markdown-body) {
  font-size: 14px;
}

.user-text {
  margin: 0;
  white-space: pre-wrap;
}

.cursor {
  animation: blink 1s step-end infinite;
  color: #059669;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.actions {
  margin-top: 6px;
  padding-left: 4px;
}

.input-area {
  padding: 12px 14px;
  border-top: 1px solid #e5e7eb;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
