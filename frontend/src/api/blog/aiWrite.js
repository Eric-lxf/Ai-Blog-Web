import request from '@/utils/request'

export function generateTitles(data) {
  return request({
    url: '/blog/ai/write/titles',
    method: 'post',
    data,
  })
}

export function generateSummary(data) {
  return request({
    url: '/blog/ai/write/summary',
    method: 'post',
    data,
  })
}

export function generateOutline(data) {
  return request({
    url: '/blog/ai/write/outline',
    method: 'post',
    data,
  })
}

export function submitGenerateArticle(data) {
  return request({
    url: '/blog/ai/write/generate',
    method: 'post',
    data,
  })
}

export function fetchAiTask(id) {
  return request({
    url: `/blog/ai/tasks/${id}`,
    method: 'get',
  })
}
