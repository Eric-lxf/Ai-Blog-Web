import request from '@/utils/request'

const adminBaseUrl = '/mall/admin/payments'
const userBaseUrl = '/mall/payments'

export function listMallAdminPayment(params) {
  return request({
    url: adminBaseUrl,
    method: 'get',
    params
  })
}

export function getMallAdminPayment(id) {
  return request({
    url: `${adminBaseUrl}/${id}`,
    method: 'get'
  })
}

export function createMallPayment(data) {
  return request({
    url: userBaseUrl,
    method: 'post',
    data
  })
}

export function getMallPayment(id) {
  return request({
    url: `${userBaseUrl}/${id}`,
    method: 'get'
  })
}

export function confirmMockPayment(data) {
  return request({
    url: `${userBaseUrl}/mock/confirm`,
    method: 'post',
    data
  })
}
