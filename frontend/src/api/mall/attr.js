import request from '@/utils/request'

const baseUrl = '/mall/attr'

export function listMallAttr(params) {
  return request({
    url: baseUrl,
    method: 'get',
    params
  })
}

export function getMallAttr(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addMallAttr(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateMallAttr(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delMallAttr(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

export function listMallAttrOptions(id) {
  return request({
    url: `${baseUrl}/${id}/options`,
    method: 'get'
  })
}

/** body 必须为 { options: [...] }，不可传裸数组 */
export function replaceMallAttrOptions(id, options) {
  return request({
    url: `${baseUrl}/${id}/options`,
    method: 'put',
    data: { options }
  })
}
