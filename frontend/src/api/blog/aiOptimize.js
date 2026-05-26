import request from '@/utils/request'

export function optimizeArticle(data) {
  return request({
    url: '/blog/ai/optimize',
    method: 'post',
    data,
  })
}

export function fetchPromptTemplate(scene) {
  return request({
    url: `/blog/ai/templates/${scene}`,
    method: 'get',
  })
}
