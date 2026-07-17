import request from '@/utils/request'

const baseUrl = '/mall/category'

export function listMallCategory(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

export function getMallCategory(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallCategory(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallCategory(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delMallCategory(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}
