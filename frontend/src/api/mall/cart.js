import request from '@/utils/request'

const baseUrl = '/mall/cart'

export function listMallCart() {
  return request({
    url: baseUrl,
    method: 'get'
  })
}

export function addMallCart(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallCartItem(id, data) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'put',
    data
  })
}

export function removeMallCartItem(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

export function clearMallCart() {
  return request({
    url: baseUrl,
    method: 'delete'
  })
}
