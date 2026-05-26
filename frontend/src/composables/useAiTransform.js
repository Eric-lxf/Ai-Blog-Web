import { ref } from 'vue'
import { streamAiChat } from '@/api/blog/ai'

export function useAiTransform() {
  const loading = ref(false)
  const abortController = ref(null)

  function stop() {
    abortController.value?.abort()
    abortController.value = null
    loading.value = false
  }

  async function run(params, onChunk) {
    stop()
    loading.value = true
    abortController.value = new AbortController()
    let result = ''

    try {
      await streamAiChat({
        body: {
          scene: params.scene,
          prompt: params.prompt,
          includeContext: params.includeContext ?? false,
          articleTitle: params.articleTitle,
          articleContent: params.articleContent,
        },
        signal: abortController.value.signal,
        onChunk: text => {
          result += text
          onChunk?.(result)
        },
        onError: msg => {
          throw new Error(msg)
        },
        onDone: () => {},
      })
      return result.trim()
    } finally {
      loading.value = false
      abortController.value = null
    }
  }

  return { loading, run, stop }
}
