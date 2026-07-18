import request from '@/utils/request'

const noToken = { isToken: false }

export function listPublicMallSpu(params) {
  return request({
    url: '/public/mall/spus',
    method: 'get',
    params,
    headers: noToken
  })
}

export function getPublicMallSpu(id) {
  return request({
    url: `/public/mall/spus/${id}`,
    method: 'get',
    headers: noToken
  })
}

export function listPublicMallCategory(params) {
  return request({
    url: '/public/mall/categories',
    method: 'get',
    params,
    headers: noToken
  })
}
