import { ref } from 'vue'
import { streamAiChat } from '@/api/blog/ai'

export function useAiChat() {
  const messages = ref([])
  const loading = ref(false)
  const abortController = ref(null)

  function genId() {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }

  function stop() {
    abortController.value?.abort()
    abortController.value = null
    loading.value = false
    const last = messages.value[messages.value.length - 1]
    if (last?.streaming) {
      last.streaming = false
    }
  }

  async function send(params) {
    if (!params.prompt.trim() || loading.value) return

    const userMsg = {
      id: genId(),
      role: 'user',
      content: params.prompt.trim(),
    }
    const history = messages.value.map(m => ({
      role: m.role,
      content: m.content,
    }))

    messages.value.push(userMsg)

    const assistantMsg = {
      id: genId(),
      role: 'assistant',
      content: '',
      streaming: true,
    }
    messages.value.push(assistantMsg)

    const body = {
      scene: params.scene || 'CHAT',
      prompt: params.prompt.trim(),
      includeContext: params.includeContext,
      articleTitle: params.articleTitle,
      articleContent: params.articleContent,
      history,
    }

    loading.value = true
    abortController.value = new AbortController()

    try {
      await streamAiChat({
        body,
        signal: abortController.value.signal,
        onChunk: text => {
          assistantMsg.content += text
        },
        onError: msg => {
          assistantMsg.content = assistantMsg.content || `⚠️ ${msg}`
          assistantMsg.streaming = false
        },
        onDone: () => {
          assistantMsg.streaming = false
        },
      })
    } catch (e) {
      if (e instanceof Error && e.name === 'AbortError') {
        assistantMsg.content += '\n\n_(已停止生成)_'
      } else {
        assistantMsg.content = `⚠️ ${e instanceof Error ? e.message : '请求失败'}`
      }
      assistantMsg.streaming = false
    } finally {
      loading.value = false
      abortController.value = null
      assistantMsg.streaming = false
    }
  }

  function clear() {
    stop()
    messages.value = []
  }

  return {
    messages,
    loading,
    send,
    stop,
    clear,
  }
}
