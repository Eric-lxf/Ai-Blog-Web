import request from '@/utils/request'

export function fetchSensitiveWords(params) {
  return request({
    url: '/blog/sensitive-word',
    method: 'get',
    params,
  })
}

export function addSensitiveWord(data) {
  return request({
    url: '/blog/sensitive-word',
    method: 'post',
    data,
  })
}

export function updateSensitiveWord(data) {
  return request({
    url: '/blog/sensitive-word',
    method: 'put',
    data,
  })
}

export function deleteSensitiveWords(ids) {
  return request({
    url: `/blog/sensitive-word/${ids}`,
    method: 'delete',
  })
}
