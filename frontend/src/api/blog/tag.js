import request from '@/utils/request'

export function fetchTags() {
  return request({
    url: '/blog/tag',
    method: 'get',
  })
}
