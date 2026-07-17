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
    url: `${baseUrl}/${data.id}`,
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

export function setDefaultMallAddress(row) {
  return updateMallAddress({
    id: row.id,
    receiver: row.receiver,
    mobile: row.mobile,
    province: row.province,
    city: row.city,
    district: row.district,
    detail: row.detail,
    isDefault: '1'
  })
}
