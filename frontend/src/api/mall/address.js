import request from '@/utils/request'

const baseUrl = '/mall/address'

export function listMallAddress(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

export function getMallAddress(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallAddress(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallAddress(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function removeMallAddress(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

export function setDefaultMallAddress(id) {
  return request({
    url: `${baseUrl}/${id}/default`,
    method: 'put'
  })
}
