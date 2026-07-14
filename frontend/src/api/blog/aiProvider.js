import request from '@/utils/request'

export function listAiProvider(query) {
  return request({
    url: '/blog/ai/provider',
    method: 'get',
    params: query
  })
}

export function listAiProviderOptions() {
  return request({
    url: '/blog/ai/provider/options',
    method: 'get'
  })
}

export function getAiProvider(id) {
  return request({
    url: `/blog/ai/provider/${id}`,
    method: 'get'
  })
}

export function saveAiProvider(data) {
  return request({
    url: '/blog/ai/provider',
    method: 'post',
    data
  })
}

export function deleteAiProvider(id) {
  return request({
    url: `/blog/ai/provider/${id}`,
    method: 'delete'
  })
}

export function testAiProvider(id) {
  return request({
    url: `/blog/ai/provider/${id}/test`,
    method: 'post'
  })
}

export function getAiModuleConfig() {
  return request({
    url: '/blog/ai/provider/config',
    method: 'get'
  })
}

export function saveAiModuleConfig(data) {
  return request({
    url: '/blog/ai/provider/config',
    method: 'post',
    data
  })
}
