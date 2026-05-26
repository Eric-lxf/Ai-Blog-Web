import { getToken } from '@/utils/auth'
import request from '@/utils/request'

function streamChatUrl() {
  const base = (import.meta.env.VITE_APP_BASE_API || '').replace(/\/$/, '')
  return `${base}/blog/ai/stream/chat`
}

export function fetchAiTemplates() {
  return request({
    url: '/blog/ai/templates',
    method: 'get',
  })
}

export function fetchAiStatus() {
  return request({
    url: '/blog/ai/status',
    method: 'get',
  })
}

/**
 * SSE 流式对话
 * @param {{ body: object, signal?: AbortSignal, onChunk: (t: string) => void, onError: (m: string) => void, onDone: () => void }} options
 */
export async function streamAiChat(options) {
  const { body, signal, onChunk, onError, onDone } = options

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(streamChatUrl(), {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
    signal,
  })

  if (!response.ok) {
    const text = await response.text()
    onError(text || `请求失败 (${response.status})`)
    return
  }

  const reader = response.body?.getReader()
  if (!reader) {
    onError('浏览器不支持流式响应')
    return
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = 'message'

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n')
    buffer = parts.pop() ?? ''

    for (const line of parts) {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
        continue
      }
      if (!line.startsWith('data:')) continue
      const data = line.slice(5).trim()
      if (data === '[DONE]') {
        onDone()
        return
      }
      if (currentEvent === 'error') {
        onError(data)
        return
      }
      if (data) {
        onChunk(data)
      }
      currentEvent = 'message'
    }
  }
  onDone()
}
