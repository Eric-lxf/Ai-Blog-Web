import request from '@/utils/request'

const adminBaseUrl = '/mall/admin/orders'
const userBaseUrl = '/mall/orders'

export function listMallAdminOrder(params) {
  return request({
    url: adminBaseUrl,
    method: 'get',
    params
  })
}

export function getMallAdminOrder(id) {
  return request({
    url: `${adminBaseUrl}/${id}`,
    method: 'get'
  })
}

export function shipMallOrder(id, data) {
  return request({
    url: `${adminBaseUrl}/${id}/ship`,
    method: 'post',
    data
  })
}

export function completeMallOrder(id) {
  return request({
    url: `${adminBaseUrl}/${id}/complete`,
    method: 'post'
  })
}

export function listMyMallOrder(params) {
  return request({
    url: userBaseUrl,
    method: 'get',
    params
  })
}

export function getMyMallOrder(id) {
  return request({
    url: `${userBaseUrl}/${id}`,
    method: 'get'
  })
}

export function createMallOrder(data) {
  return request({
    url: userBaseUrl,
    method: 'post',
    data
  })
}

export function cancelMyMallOrder(id, data) {
  return request({
    url: `${userBaseUrl}/${id}/cancel`,
    method: 'post',
    data
  })
}
