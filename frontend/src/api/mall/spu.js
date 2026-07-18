import request from '@/utils/request'

const baseUrl = '/mall/spu'

export function listMallSpu(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

export function getMallSpu(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallSpu(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallSpu(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function delMallSpu(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

export function updateMallSpuStatus(id, status) {
  return request({
    url: `${baseUrl}/${id}/publish`,
    method: 'put',
    data: { status }
  })
}
