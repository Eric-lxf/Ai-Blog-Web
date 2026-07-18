import request from '@/utils/request'

const baseUrl = '/mall/brand'

export function listMallBrand(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

export function getMallBrand(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallBrand(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallBrand(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delMallBrand(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}
